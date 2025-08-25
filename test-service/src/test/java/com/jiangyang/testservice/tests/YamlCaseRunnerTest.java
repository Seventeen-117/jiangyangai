package com.jiangyang.testservice.tests;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Generic YAML-driven runner.
 * Loads all YAML files under test-data/messages-service and executes.
 * Supports two formats:
 * 1) Single-case template (from generator):
 *    service, name, request{method,url,headers,body}, expect{status}
 * 2) Multi-case file (like messages-saga-send.yml with root baseUrl/endpoint/headers + cases[])
 */
@TestPropertySource(properties = {
        "yaml.case.dir=test-data"
})
public class YamlCaseRunnerTest extends BaseIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(YamlCaseRunnerTest.class);

    @Value("${yaml.case.dir:test-data}")
    private String yamlDir;

    @Value("${target.gateway.base-url:http://localhost:8080}")
    private String gatewayBaseUrl;

    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    private final ObjectMapper jsonMapper = new ObjectMapper();
    private WebClient webClient;

    record Single(String fileName, String name, String method, String url, Map<String,String> headers, JsonNode body, int expectStatus) {}

    @BeforeClass
    public void setup() {
        webClient = WebClient.builder().build();
    }

    @DataProvider(name = "yamlFiles")
    public Object[][] yamlFiles() throws Exception {
        List<Object[]> list = new ArrayList<>();
        String dirSetting = (yamlDir != null && !yamlDir.isBlank()) ? yamlDir : "test-data";
        Path dirPath = null;
        // Prefer filesystem under main resources (generator output)
        Path fsMain = Paths.get("test-service", "src", "main", "resources", dirSetting);
        if (Files.exists(fsMain)) {
            dirPath = fsMain;
        } else {
            // Try classpath
            URL url = getClass().getClassLoader().getResource(dirSetting);
            if (url != null && "file".equals(url.getProtocol())) {
                dirPath = Paths.get(url.toURI());
            }
        }
        if (dirPath == null) {
            throw new IllegalStateException("YAML directory not found: " + dirSetting);
        }
        Files.walk(dirPath)
                .filter(p -> p.toString().endsWith(".yml") || p.toString().endsWith(".yaml"))
                .forEach(p -> list.add(new Object[]{p.toFile()}));
        return list.toArray(new Object[0][0]);
    }

    @Test(dataProvider = "yamlFiles")
    public void runYaml(File file) throws Exception {
        log.info("Execute YAML file: {}", file.getAbsolutePath());
        try (InputStream is = Files.newInputStream(file.toPath())) {
            JsonNode root = yamlMapper.readTree(is);
            List<Single> singles = expandCases(file.getName(), root);
            for (Single s : singles) {
                executeSingle(s);
            }
        }
    }

    private List<Single> expandCases(String fileName, JsonNode root) {
        List<Single> out = new ArrayList<>();
        if (root.has("request") && root.has("expect")) {
            // generator single template
            String method = root.path("request").path("method").asText("GET");
            String url = substituteGateway(root.path("request").path("url").asText());
            Map<String,String> headers = jsonMapper.convertValue(root.path("request").path("headers"), Map.class);
            JsonNode body = root.path("request").path("body");
            int expect = root.path("expect").path("status").asInt(200);
            String name = root.path("name").asText(fileName);
            out.add(new Single(fileName, name, method, url, headers, body, expect));
            return out;
        }
        // multi-case format
        String endpoint = root.path("endpoint").asText("");
        String baseUrl = ((gatewayBaseUrl != null && !gatewayBaseUrl.isBlank()) ? gatewayBaseUrl : "http://localhost:8080")
                + (endpoint.startsWith("/") ? endpoint : "/" + endpoint);
        Map<String,String> commonHeaders = jsonMapper.convertValue(root.path("headers"), Map.class);
        for (Iterator<JsonNode> it = root.path("cases").elements(); it.hasNext();) {
            JsonNode c = it.next();
            String name = c.path("name").asText(fileName);
            JsonNode body = c.path("request").path("body");
            String url = fillPathVariables(baseUrl, c.path("request").path("pathParams"));
            int expect = c.path("expect").path("status").asInt(200);
            out.add(new Single(fileName, name, "POST", url, commonHeaders, body, expect));
        }
        return out;
    }

    private String substituteGateway(String raw) {
        if (raw == null || raw.isBlank()) return raw;
        String base = (gatewayBaseUrl != null && !gatewayBaseUrl.isBlank()) ? gatewayBaseUrl : "http://localhost:8080";
        if (raw.startsWith("${TARGET_BASE_URL")) {
            int endBrace = raw.indexOf('}');
            if (endBrace == -1) {
                return raw;
            }
            String suffix = raw.substring(endBrace + 1);
            return base + suffix;
        }
        if (raw.startsWith("/")) {
            return base + raw;
        }
        return raw;
    }

    private String fillPathVariables(String url, JsonNode pathParamsNode) {
        if (url == null) return null;
        String result = url;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\{([^/}]+)\\}").matcher(url);
        while (m.find()) {
            String var = m.group(1);
            String replacement = "1";
            if (pathParamsNode != null && pathParamsNode.has(var) && !pathParamsNode.get(var).isNull()) {
                replacement = pathParamsNode.get(var).asText();
            }
            result = result.replace("{" + var + "}", replacement);
        }
        return result;
    }

    private void executeSingle(Single c) throws IOException {
        log.info("Case [{}] {} {}", c.name(), c.method(), c.url());
        boolean hasBody = c.body()!=null && !c.body().isNull() && (c.method().equalsIgnoreCase("POST") || c.method().equalsIgnoreCase("PUT") || c.method().equalsIgnoreCase("PATCH"));
        Integer status;
        if (hasBody) {
            WebClient.RequestBodyUriSpec s = webClient.method(org.springframework.http.HttpMethod.valueOf(c.method()));
            status = s.uri(c.url())
                    .accept(MediaType.APPLICATION_JSON)
                    .headers(h -> { if (c.headers()!=null) c.headers().forEach(h::add); })
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(jsonMapper.writeValueAsString(c.body())))
                    .exchangeToMono(resp -> reactor.core.publisher.Mono.just(resp.statusCode().value()))
                    .onErrorResume(ex -> reactor.core.publisher.Mono.just(-1))
                    .block();
        } else {
            WebClient.RequestHeadersSpec<?> s = webClient.method(org.springframework.http.HttpMethod.valueOf(c.method()))
                    .uri(c.url())
                    .accept(MediaType.APPLICATION_JSON)
                    .headers(h -> { if (c.headers()!=null) c.headers().forEach(h::add); });
            status = s.exchangeToMono(resp -> reactor.core.publisher.Mono.just(resp.statusCode().value()))
                    .onErrorResume(ex -> reactor.core.publisher.Mono.just(-1))
                    .block();
        }
        Assert.assertNotNull(status);
        Assert.assertEquals(status.intValue(), c.expectStatus(), "Unexpected HTTP status for case: " + c.name());
    }
}


