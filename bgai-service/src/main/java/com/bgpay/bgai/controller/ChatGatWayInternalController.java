package com.bgpay.bgai.controller;

import com.bgpay.bgai.entity.ApiConfig;
import com.bgpay.bgai.entity.UsageInfo;
import com.bgpay.bgai.response.ChatResponse;
import com.bgpay.bgai.service.ApiConfigService;
import com.bgpay.bgai.service.deepseek.DeepSeekService;
import com.bgpay.bgai.service.deepseek.ReactiveFileProcessor;
import com.bgpay.bgai.service.impl.FallbackService;
import com.bgpay.bgai.transaction.TransactionCoordinator;
import com.bgpay.bgai.web.RequestAttributesProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreakerFactory;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeoutException;
import java.util.Map;

/**
 * ChatGatWay内部接口控制器
 * 提供内部服务调用的聊天接口，专注于AI处理逻辑
 * 验证逻辑已由signature-service处理
 */
@Tag(name = "ChatGatWay内部接口", description = "内部服务调用的聊天接口，专注于AI处理")
@RestController
@RequestMapping("/api")
@Slf4j
public class ChatGatWayInternalController {

    private final ReactiveFileProcessor fileProcessor;
    private final ApiConfigService apiConfigService;
    private final DeepSeekService deepSeekService;
    private final ReactiveCircuitBreakerFactory circuitBreakerFactory;
    private final FallbackService fallbackService;
    private final TransactionCoordinator transactionCoordinator;
    private final RequestAttributesProvider attributesProvider;

    @Autowired
    public ChatGatWayInternalController(ReactiveFileProcessor fileProcessor,
                                        ApiConfigService apiConfigService,
                                        DeepSeekService deepSeekService,
                                        ReactiveCircuitBreakerFactory circuitBreakerFactory,
                                        FallbackService fallbackService,
                                        TransactionCoordinator transactionCoordinator,
                                        RequestAttributesProvider attributesProvider) {
        this.fileProcessor = fileProcessor;
        this.apiConfigService = apiConfigService;
        this.deepSeekService = deepSeekService;
        this.circuitBreakerFactory = circuitBreakerFactory;
        this.fallbackService = fallbackService;
        this.transactionCoordinator = transactionCoordinator;
        this.attributesProvider = attributesProvider;
    }

    /**
     * 处理聊天请求 - 支持多种格式
     * 验证逻辑已由signature-service处理，此处专注于AI处理
     *
     * @param file 上传的文件（可选）
     * @param question 问题内容
     * @param apiUrl API URL（可选）
     * @param apiKey API密钥（可选）
     * @param modelName 模型名称（可选）
     * @param multiTurn 是否多轮对话
     * @param exchange 请求上下文
     * @return 聊天响应
     */
    @Operation(
        summary = "处理聊天请求", 
        description = "处理内部服务的聊天请求，支持文件上传和文本处理，验证已由signature-service完成"
    )
    @PostMapping(
            value = "/chatGatWay-internal",
            consumes = {MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_FORM_URLENCODED_VALUE, 
                       MediaType.APPLICATION_JSON_VALUE, MediaType.ALL_VALUE},
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Mono<ResponseEntity<ChatResponse>> handleChatRequest(
            @RequestPart(value = "file", required = false) FilePart file,
            @RequestPart(value = "question", required = false) String questionParam,
            @RequestPart(value = "apiUrl", required = false) String apiUrlParam,
            @RequestPart(value = "apiKey", required = false) String apiKeyParam,
            @RequestPart(value = "modelName", required = false) String modelNameParam,
            @RequestPart(value = "multiTurn", required = false) String multiTurnStr,
            ServerWebExchange exchange) {

        log.info("接收到chatGatWay-internal请求 - 文件={}, 问题参数={}, 模型参数={}", 
            file != null ? file.filename() : "无文件", 
            questionParam, modelNameParam);

        // 从请求头中获取用户信息（由signature-service验证后传递）
        String userId = getUserIdFromHeaders(exchange);
        log.info("从请求头获取用户ID: {}", userId);

        // 从请求体中尝试获取表单数据
        return exchange.getFormData()
            .doOnError(e -> log.warn("获取表单数据失败: {}", e.getMessage()))
            .onErrorResume(e -> {
                log.warn("将使用URL参数代替表单数据");
                return Mono.just(new LinkedMultiValueMap<>());
            })
            .defaultIfEmpty(new LinkedMultiValueMap<>())
            .flatMap(formData -> {
                // 从表单数据中提取，如果参数为空
                String question = StringUtils.hasText(questionParam) ? questionParam : 
                        formData.getFirst("question");
                
                String apiUrl = StringUtils.hasText(apiUrlParam) ? apiUrlParam : 
                        formData.getFirst("apiUrl");
                
                String apiKey = StringUtils.hasText(apiKeyParam) ? apiKeyParam : 
                        formData.getFirst("apiKey");
                
                String modelName = StringUtils.hasText(modelNameParam) ? modelNameParam : 
                        formData.getFirst("modelName");
                
                // 处理multiTurn参数，支持字符串和布尔值
                boolean multiTurn = false;
                if (StringUtils.hasText(multiTurnStr)) {
                    multiTurn = "true".equalsIgnoreCase(multiTurnStr);
                } else if (formData.containsKey("multiTurn")) {
                    multiTurn = "true".equalsIgnoreCase(formData.getFirst("multiTurn"));
                }

                // 记录所有收集到的参数
                log.info("收集到的完整参数: question=\"{}\", apiUrl=\"{}\", modelName=\"{}\", multiTurn={}", 
                    question, apiUrl, modelName, multiTurn);

                return processRequestWithUserId(userId, question, apiUrl, apiKey, 
                                                modelName, multiTurn, file, exchange);
            });
    }

    /**
     * 处理JSON格式的聊天请求
     */
    @PostMapping(
        value = "/chatGatWay-internal",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public Mono<ResponseEntity<ChatResponse>> handleChatRequestJson(
            @RequestBody(required = false) Map<String, Object> body,
            ServerWebExchange exchange) {
        
        if (body == null) {
            return Mono.just(errorResponse(400, "请求体不能为空"));
        }
        
        String question = body != null ? (String) body.get("question") : null;
        if (question == null || question.trim().isEmpty()) {
            return Mono.just(errorResponse(400, "question参数不能为空"));
        }
        
        String apiKey = body != null && body.get("apiKey") != null ? (String) body.get("apiKey") : null;
        String apiUrl = body != null && body.get("apiUrl") != null ? (String) body.get("apiUrl") : null;
        String modelName = body != null ? (String) body.get("modelName") : null;
        Object multiTurnObj = body != null ? body.get("multiTurn") : null;
        String multiTurnStr = multiTurnObj != null ? String.valueOf(multiTurnObj) : null;
        
        // 从请求头中获取用户信息
        String userId = getUserIdFromHeaders(exchange);
        log.info("JSON请求 - 用户ID: {}, 问题: {}", userId, question);
        
        // 复用原有逻辑
        return handleChatRequest(null, question, apiUrl, apiKey, modelName, multiTurnStr, exchange);
    }

    /**
     * 从请求头获取用户ID
     * 验证已由signature-service完成，此处只需提取用户信息
     */
    private String getUserIdFromHeaders(ServerWebExchange exchange) {
        // 优先从X-User-Id头部获取
        String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
        if (StringUtils.hasText(userId)) {
            return userId;
        }
        
        // 其次从X-Gateway-Source头部判断是否来自网关
        String gatewaySource = exchange.getRequest().getHeaders().getFirst("X-Gateway-Source");
        if (StringUtils.hasText(gatewaySource)) {
            return "gateway-user";
        }
        
        // 最后使用默认值
        return "anonymous";
    }

    /**
     * 使用确定的用户ID处理请求
     */
    private Mono<ResponseEntity<ChatResponse>> processRequestWithUserId(String userId, String question, String apiUrl, 
                                                        String apiKey, String modelName, boolean multiTurn, 
                                                        FilePart file, ServerWebExchange exchange) {
        if ((file == null || (file.filename() != null && file.filename().isEmpty())) && 
            (question == null || question.trim().isEmpty())) {
            log.error("Both file and question are empty");
            return Mono.just(errorResponse(400, "必须提供问题或文件"));
        }

        // 处理文件 - 使用熔断器
        Mono<String> contentMono = file == null ?
            Mono.just(buildTextContent(question != null ? question : "")) :
            processFileWithCircuitBreaker(file, question != null ? question : "", multiTurn);

        return resolveApiConfigReactive(apiUrl, apiKey, modelName, userId)
                .flatMap(apiConfig -> {
                    log.info("API config resolved: url={}, model={}",
                            apiConfig.getApiUrl(), apiConfig.getModelName());

                    return contentMono.flatMap(content -> {
                        log.info("Content processed, length: {}", content.length());

                        // API调用 - 添加超时熔断器
                        return callDeepSeekApiWithCircuitBreaker(
                                content,
                                apiConfig,
                                userId,
                                multiTurn
                        );
                    });
                })
                .onErrorResume(e -> {
                    if (e instanceof IllegalArgumentException) {
                        log.error("API configuration error: {}", e.getMessage());
                        return Mono.just(errorResponse(400, e.getMessage()));
                    }
                    if (e instanceof RedisSystemException) {
                        log.error("Redis error: {}", e.getMessage());
                        return Mono.just(errorResponse(503, "系统暂时不可用，请稍后重试"));
                    }
                    log.error("Request processing failed: {}", e.getMessage(), e);

                    // 创建通用错误熔断器
                    ReactiveCircuitBreaker generalErrorBreaker = createCircuitBreaker("generalErrorBreaker");

                    return generalErrorBreaker.run(
                        fallbackService.handleGeneralFallback(e),
                        throwable -> fallbackService.handleGeneralFallback(e)
                    );
                });
    }

    /**
     * 创建指定名称的熔断器
     */
    private ReactiveCircuitBreaker createCircuitBreaker(String name) {
        return circuitBreakerFactory.create(name);
    }

    /**
     * 使用熔断器处理文件内容，包含备选处理机制
     */
    private Mono<String> processFileWithCircuitBreaker(FilePart file, String question, boolean multiTurn) {
        log.info("Processing file with circuit breaker: fileName={}, question={}", file.filename(), question);

        // 创建文件处理熔断器
        ReactiveCircuitBreaker fileProcessingBreaker = createCircuitBreaker("fileProcessingBreaker");

        return fileProcessingBreaker.run(
            fileProcessor.processReactiveFile(file)
                .onErrorResume(e -> {
                    log.error("Standard file processing failed, trying alternative method: {}", e.getMessage());
                    return tryAlternativeFileProcessing(file);
                }),
            throwable -> {
                log.warn("Circuit breaker triggered for file processing, using direct FileProcessor: {}", throwable.getMessage());
                return tryAlternativeFileProcessing(file);
            }
        ).map(fileContent -> buildFileContent(fileContent, question));
    }
    
    /**
     * 使用FileProcessor进行替代性文件处理
     */
    private Mono<String> tryAlternativeFileProcessing(FilePart file) {
        try {
            // 创建临时文件用于处理
            Path tempFile = Files.createTempFile("alt_process_", "_" + sanitizeFilename(file.filename()));
            
            return file.transferTo(tempFile)
                .then(Mono.fromCallable(() -> {
                    try {
                        log.info("Attempting alternative file processing with direct FileProcessor for: {}", file.filename());
                        // 使用FileProcessor直接处理文件
                        return fileProcessor.getFileProcessor().processFile(tempFile.toFile());
                    } catch (Exception ex) {
                        log.error("Alternative file processing failed: {}", ex.getMessage(), ex);
                        throw new RuntimeException("所有文件处理方法均失败: " + ex.getMessage());
                    } finally {
                        // 清理临时文件
                        try {
                            Files.deleteIfExists(tempFile);
                        } catch (Exception ignored) {
                            log.warn("Failed to delete temp file: {}", tempFile);
                        }
                    }
                }))
                .onErrorResume(e -> {
                    log.error("All file processing methods failed: {}", e.getMessage());
                    return Mono.just("无法解析文件内容，请提供不同格式的文件或直接提问。错误: " + e.getMessage());
                });
        } catch (IOException e) {
            log.error("Failed to create temporary file for alternative processing: {}", e.getMessage());
            return Mono.just("文件处理失败: 无法创建临时文件");
        }
    }
    
    /**
     * 清理文件名，防止路径遍历攻击
     */
    private String sanitizeFilename(String filename) {
        return filename != null ? filename.replaceAll("[\\\\/:*?\"<>|]", "_") : "unknown";
    }

    /**
     * 使用熔断器调用DeepSeek API
     */
    private Mono<ResponseEntity<ChatResponse>> callDeepSeekApiWithCircuitBreaker(
            String content, ApiConfig apiConfig, String userId, boolean multiTurn) {

        // 创建API调用熔断器
        ReactiveCircuitBreaker deepseekApiBreaker = createCircuitBreaker("deepseekApiBreaker");
        
        return deepseekApiBreaker.run(
            deepSeekService.processRequestReactive(
                    content,
                    apiConfig.getApiUrl(),
                    apiConfig.getApiKey(),
                    apiConfig.getModelName(),
                    userId,
                    multiTurn
            )
            .doOnNext(resp -> {
                log.info("Processed response for user {}, content length: {}", 
                        userId, resp.getContent().length());
                
                // 提取实际返回的chatCompletionId，与缓存比较确认一致性
                if (resp.getUsage() != null && resp.getUsage().getChatCompletionId() != null) {
                    String responseId = resp.getUsage().getChatCompletionId();
                    String cachedId = transactionCoordinator.getCurrentCompletionId(userId);
                    
                    if (cachedId != null && !cachedId.equals(responseId)) {
                        log.warn("ChatCompletionId inconsistency detected, using transaction coordinator ID");
                        // 优先使用事务协调器中的ID
                        resp.getUsage().setChatCompletionId(cachedId);
                    }
                }
            })
            .map(ResponseEntity::ok)
            .timeout(Duration.ofSeconds(60))  // 添加超时
            .retryWhen(Retry.backoff(1, Duration.ofSeconds(2))
                .filter(throwable -> throwable instanceof TimeoutException)
                .doBeforeRetry(signal -> 
                    log.warn("Retrying DeepSeek API call after timeout: {}", signal.failure().getMessage()))
            ),
            throwable -> {
                log.error("Circuit broken for API call: {}", throwable.getMessage());
                
                // 使用事务协调器的回滚机制获取一致的ID
                String rollbackId = transactionCoordinator.rollback(userId);
                log.info("Using rolled back chatCompletionId for circuit breaker fallback: {}", rollbackId);
                
                // 构建包含一致ID的回退响应
                return fallbackService.handleCircuitBreakerFallback(userId, throwable, rollbackId);
            }
        );
    }

    /**
     * 解析API配置
     */
    private Mono<ApiConfig> resolveApiConfigReactive(String apiUrl, String apiKey, String modelName, String userId) {
        // 如果提供了完整API参数，直接使用这些参数创建配置
        if (StringUtils.hasText(apiUrl) && StringUtils.hasText(apiKey)) {
            // 如果modelName为空，使用默认值
            String finalModelName = StringUtils.hasText(modelName) ? modelName : "deepseek-chat";
            log.info("PARAM TRACE [{}]: 直接使用提供的参数 - apiUrl={}, modelName={} (原始: {})", 
                    userId, apiUrl, finalModelName, modelName);
            
            return Mono.just(new ApiConfig()
                    .setApiUrl(apiUrl)
                    .setApiKey(apiKey)
                    .setModelName(finalModelName));
        }
        
        // 尝试根据提供的任意参数和userId查询匹配的配置
        return Mono.fromCallable(() -> {
            // 从数据库中查找匹配的配置
            log.info("PARAM TRACE [{}]: 尝试查找匹配配置 - 参数: apiUrl={}, modelName={}", 
                    userId, apiUrl, modelName);
            ApiConfig config = apiConfigService.findMatchingConfig(userId, apiUrl, apiKey, modelName);
            
            // 如果找到了配置
            if (config != null) {
                log.info("PARAM TRACE [{}]: 找到匹配配置 - apiUrl={}, modelName={} (原始请求: {})", 
                        userId, config.getApiUrl(), config.getModelName(), modelName);
                
                // 如果传入了modelName，使用传入的modelName
                if (StringUtils.hasText(modelName)) {
                    config.setModelName(modelName);
                    log.info("PARAM TRACE [{}]: 使用传入的modelName: {}", userId, modelName);
                } else if (!StringUtils.hasText(config.getModelName())) {
                    // 如果配置中的modelName为空，使用默认值
                    config.setModelName("deepseek-chat");
                    log.info("PARAM TRACE [{}]: 配置中模型名称为空，使用默认值: {}", userId, config.getModelName());
                }

                return config;
            }
            
            // 如果没有任何参数传入，尝试获取最新的配置
            if (!StringUtils.hasText(apiUrl) && !StringUtils.hasText(apiKey) && !StringUtils.hasText(modelName)) {
                log.info("PARAM TRACE [{}]: 未提供任何参数，尝试获取最新配置", userId);
                ApiConfig latestConfig = apiConfigService.getLatestConfig(userId);
                if (latestConfig != null) {
                    log.info("PARAM TRACE [{}]: 使用最新配置 - apiUrl={}, modelName={}", 
                            userId, latestConfig.getApiUrl(), latestConfig.getModelName());
                    
                    // 如果传入了modelName，使用传入的modelName
                    if (StringUtils.hasText(modelName)) {
                        latestConfig.setModelName(modelName);
                        log.info("PARAM TRACE [{}]: 使用传入的modelName: {}", userId, modelName);
                    } else if (!StringUtils.hasText(latestConfig.getModelName())) {
                        // 如果配置中的modelName为空，使用默认值
                        latestConfig.setModelName("deepseek-chat");
                        log.info("PARAM TRACE [{}]: 最新配置中模型名称为空，使用默认值: {}", userId, latestConfig.getModelName());
                    }
                    
                    return latestConfig;
                }
            }
            
            // 如果无法找到配置，抛出异常
            log.warn("PARAM TRACE [{}]: 未找到匹配的API配置 - 原始请求参数: apiUrl={}, modelName={}", 
                    userId, apiUrl, modelName);
            throw new IllegalArgumentException("未找到匹配的API配置");
        })
        .switchIfEmpty(Mono.error(new IllegalArgumentException("未找到用户API配置且未提供完整参数")))
        .doOnError(e -> log.error("PARAM TRACE [{}]: 配置解析失败 - {}", userId, e.getMessage(), e));
    }

    private String buildFileContent(String fileContent, String question) {
        return "【File Content】\n" + fileContent + "\n\n【User Question】" + question;
    }

    private String buildTextContent(String question) {
        return "【User Question】" + question;
    }

    private ResponseEntity<ChatResponse> errorResponse(int code, String message) {
        ChatResponse response = new ChatResponse();
        response.setContent(String.format("{\"error\":{\"code\":%d,\"message\":\"%s\"}}", code, message));
        response.setUsage(new UsageInfo());
        return ResponseEntity.status(code).body(response);
    }
} 