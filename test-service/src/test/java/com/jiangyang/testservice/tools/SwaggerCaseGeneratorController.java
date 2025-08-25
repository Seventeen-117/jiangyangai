package com.jiangyang.testservice.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.qameta.allure.Step;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;

@RestController
@RequestMapping("/tools/swagger")
public class SwaggerCaseGeneratorController {

    private static final Logger log = LoggerFactory.getLogger(SwaggerCaseGeneratorController.class);

    @Autowired
    private SwaggerTargetsProperties props;

    private final ObjectMapper mapper = new ObjectMapper();

    private final WebClient webClient = WebClient.builder().build();

    @GetMapping(value = "/generate", produces = MediaType.TEXT_PLAIN_VALUE)
    @Step("Generate YAML case templates from Swagger of all configured services")
    public String generate() throws Exception {
        StringBuilder sb = new StringBuilder();
        for (SwaggerTargetsProperties.Target t : props.getTargets()) {
            String api = t.getUrl();
            if (!StringUtils.hasText(api)) continue;
            if (!api.endsWith(props.getApiDocsPath())) {
                if (api.endsWith("/")) api = api.substring(0, api.length() - 1);
                api = api + props.getApiDocsPath();
            }
            sb.append("Processing ").append(t.getName()).append(" -> ").append(api).append('\n');
            String json = webClient.get().uri(api).accept(MediaType.APPLICATION_JSON).retrieve().bodyToMono(String.class).block();
            JsonNode root = mapper.readTree(json);
            JsonNode paths = root.path("paths");
            File outDir = new File("test-service/src/test/resources/test-data/" + t.getName());
            if (!outDir.exists()) outDir.mkdirs();
            int count = 0;
            for (Iterator<Map.Entry<String, JsonNode>> it = paths.fields(); it.hasNext();) {
                Map.Entry<String, JsonNode> e = it.next();
                String path = e.getKey();
                JsonNode methods = e.getValue();
                for (Iterator<Map.Entry<String, JsonNode>> mi = methods.fields(); mi.hasNext();) {
                    Map.Entry<String, JsonNode> me = mi.next();
                    String method = me.getKey().toUpperCase();
                    JsonNode op = me.getValue();
                    String summary = op.path("summary").asText("");
                    String fileSafe = path.replaceAll("[^a-zA-Z0-9-_]", "_") + "__" + method.toLowerCase() + ".yml";
                    String yaml = buildTemplateYaml(t.getName(), method, path, summary);
                    try (FileOutputStream fos = new FileOutputStream(new File(outDir, fileSafe))) {
                        fos.write(yaml.getBytes(StandardCharsets.UTF_8));
                    }
                    count++;
                }
            }
            sb.append("Generated ").append(count).append(" templates for ").append(t.getName()).append('\n');
        }
        return sb.toString();
    }

    private String buildTemplateYaml(String service, String method, String path, String summary) {
        return "service: " + service + '\n' +
                "name: \"" + (summary.isEmpty() ? path : summary) + "\"\n" +
                "request:\n" +
                "  method: " + method + '\n' +
                "  url: ${TARGET_BASE_URL:http://localhost:8080}" + path + "\n" +
                "  headers:\n" +
                "    Content-Type: application/json\n" +
                "    X-Gateway-Source: test-service\n" +
                "  body: {}\n" +
                "expect:\n" +
                "  status: 200\n";
    }
}


