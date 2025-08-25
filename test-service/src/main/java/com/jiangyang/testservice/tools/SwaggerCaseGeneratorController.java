package com.jiangyang.testservice.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ExceptionHandler;
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

    @GetMapping(value = "/config", produces = MediaType.APPLICATION_JSON_VALUE)
    public java.util.Map<String, Object> config() {
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        map.put("apiDocsPath", props.getApiDocsPath());
        java.util.List<java.util.Map<String,String>> targets = new java.util.ArrayList<>();
        for (SwaggerTargetsProperties.Target t : props.getTargets()) {
            java.util.Map<String,String> one = new java.util.HashMap<>();
            one.put("name", t.getName());
            one.put("url", t.getUrl());
            targets.add(one);
        }
        map.put("targets", targets);
        return map;
    }

    @GetMapping(value = "/preview", produces = MediaType.TEXT_PLAIN_VALUE)
    public String preview() {
        StringBuilder sb = new StringBuilder();
        for (SwaggerTargetsProperties.Target t : props.getTargets()) {
            try {
                String api = t.getUrl();
                if (!StringUtils.hasText(api)) {
                    sb.append(t.getName()).append(" -> SKIP (empty url)\n");
                    continue;
                }
                if (!api.endsWith(props.getApiDocsPath())) {
                    if (api.endsWith("/")) api = api.substring(0, api.length() - 1);
                    api = api + props.getApiDocsPath();
                }
                String json = webClient.get().uri(api).accept(MediaType.APPLICATION_JSON).retrieve().bodyToMono(String.class).block();
                if (json == null || json.isBlank()) {
                    sb.append(t.getName()).append(" -> EMPTY RESPONSE ").append(api).append('\n');
                    continue;
                }
                JsonNode root = mapper.readTree(json);
                int count = root.path("paths").size();
                sb.append(t.getName()).append(" -> OK paths=").append(count).append(" @ ").append(api).append('\n');
            } catch (Exception e) {
                sb.append(t.getName()).append(" -> ERROR ").append(e.getMessage()).append('\n');
            }
        }
        return sb.toString();
    }

    @GetMapping(value = "/generate", produces = MediaType.TEXT_PLAIN_VALUE)
    public String generate() throws Exception {
        StringBuilder sb = new StringBuilder();
        for (SwaggerTargetsProperties.Target t : props.getTargets()) {
            try {
                String api = t.getUrl();
                if (!StringUtils.hasText(api)) {
                    sb.append("Skip ").append(t.getName()).append(": empty url\n");
                    continue;
                }
                if (!api.endsWith(props.getApiDocsPath())) {
                    if (api.endsWith("/")) api = api.substring(0, api.length() - 1);
                    api = api + props.getApiDocsPath();
                }
                sb.append("Processing ").append(t.getName()).append(" -> ").append(api).append('\n');
                String json = webClient.get().uri(api).accept(MediaType.APPLICATION_JSON).retrieve().bodyToMono(String.class).block();
                if (json == null || json.isBlank()) {
                    sb.append("  Failed: empty response\n");
                    continue;
                }
                JsonNode root = mapper.readTree(json);
                JsonNode paths = root.path("paths");
                File outDir = resolveOutDir(t.getName());
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
                sb.append("  Generated ").append(count).append(" files\n");
            } catch (Exception ex) {
                sb.append("  Error: ").append(ex.getMessage()).append('\n');
                log.warn("Generate for {} failed", t.getName(), ex);
            }
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

    private File resolveOutDir(String service) {
        // Prefer test resources: ./src/test/resources/test-data/{service}
        File testLocal = new File("src/test/resources/test-data/" + service);
        if (testLocal.exists() || (testLocal.getParentFile() != null && testLocal.getParentFile().exists())) return testLocal;
        // Fallback when running from repo root
        File testRepoPath = new File("test-service/src/test/resources/test-data/" + service);
        if (testRepoPath.exists() || (testRepoPath.getParentFile() != null && testRepoPath.getParentFile().exists())) return testRepoPath;
        // Final fallback to main resources to avoid failure
        File mainRepoPath = new File("test-service/src/main/resources/test-data/" + service);
        return mainRepoPath;
    }

    @ExceptionHandler(Exception.class)
    public String onError(Exception e) {
        log.error("Generate failed", e);
        return "ERROR: " + e.getMessage();
    }
}


