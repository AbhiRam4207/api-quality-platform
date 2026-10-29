package api_quality_platform.service;

import api_quality_platform.dto.AiFailureAnalysisResponse;
import api_quality_platform.entity.Api;
import api_quality_platform.entity.ApiTest;
import api_quality_platform.entity.TestExecution;
import api_quality_platform.exception.AiConfigurationException;
import api_quality_platform.exception.AiProviderException;
import api_quality_platform.exception.NotFoundException;
import api_quality_platform.repository.ApiTestRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class AiFailureAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AiFailureAnalysisService.class);

    private static final String DEFAULT_GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models";
    private static final String DEFAULT_OMNIROUTE_BASE_URL = "http://localhost:20128/v1";
    private static final String DEFAULT_OPENROUTER_BASE_URL = "https://openrouter.ai/api/v1";
    private static final String CHAT_COMPLETIONS_PATH = "/chat/completions";

    private final TestExecutionService testExecutionService;
    private final ApiTestRepository apiTestRepository;
    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper objectMapper;
    private final ChangelogService changelogService;
    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final String providerName;

    @Autowired
    public AiFailureAnalysisService(
            TestExecutionService testExecutionService,
            ApiTestRepository apiTestRepository,
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            ChangelogService changelogService,
            @Value("${ai.provider:gemini}") String provider,
            @Value("${gemini.base-url:https://generativelanguage.googleapis.com/v1beta/models}") String geminiBaseUrl,
            @Value("${GEMINI_API_KEY:${gemini.api-key:}}") String geminiApiKey,
            @Value("${gemini.model:gemini-3.5-flash-lite}") String geminiModel,
            @Value("${omniroute.base-url:http://localhost:20128/v1}") String omnirouteBaseUrl,
            @Value("${OMNIROUTE_API_KEY:${omniroute.api-key:}}") String omnirouteApiKey,
            @Value("${omniroute.model:auto/best-coding}") String omnirouteModel) {

        this.testExecutionService = testExecutionService;
        this.apiTestRepository = apiTestRepository;
        this.restClientBuilder = restClientBuilder;
        this.objectMapper = objectMapper;
        this.changelogService = changelogService;

        if ("gemini".equalsIgnoreCase(provider)) {
            this.providerName = "gemini";
            this.baseUrl = normalizeBaseUrl(geminiBaseUrl, DEFAULT_GEMINI_BASE_URL);
            this.apiKey = geminiApiKey;
            this.model = (geminiModel != null && !geminiModel.isBlank()) ? geminiModel : "gemini-3.5-flash-lite";
        } else if ("omniroute".equalsIgnoreCase(provider)) {
            this.providerName = "omniroute";
            this.baseUrl = normalizeBaseUrl(omnirouteBaseUrl, DEFAULT_OMNIROUTE_BASE_URL);
            this.apiKey = omnirouteApiKey;
            this.model = (omnirouteModel != null && !omnirouteModel.isBlank()) ? omnirouteModel : "auto/best-coding";
        } else {
            if (geminiApiKey != null && !geminiApiKey.isBlank()) {
                this.providerName = "gemini";
                this.baseUrl = normalizeBaseUrl(geminiBaseUrl, DEFAULT_GEMINI_BASE_URL);
                this.apiKey = geminiApiKey;
                this.model = (geminiModel != null && !geminiModel.isBlank()) ? geminiModel : "gemini-3.5-flash-lite";
            } else {
                this.providerName = "omniroute";
                this.baseUrl = normalizeBaseUrl(omnirouteBaseUrl, DEFAULT_OMNIROUTE_BASE_URL);
                this.apiKey = omnirouteApiKey;
                this.model = (omnirouteModel != null && !omnirouteModel.isBlank()) ? omnirouteModel : "auto/best-coding";
            }
        }
    }

    public AiFailureAnalysisService(
            TestExecutionService testExecutionService,
            ApiTestRepository apiTestRepository,
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            ChangelogService changelogService,
            String baseUrl,
            String apiKey,
            String model) {
        this(testExecutionService, apiTestRepository, restClientBuilder, objectMapper, changelogService, baseUrl, apiKey, model, "omniroute");
    }

    public AiFailureAnalysisService(
            TestExecutionService testExecutionService,
            ApiTestRepository apiTestRepository,
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            ChangelogService changelogService,
            String baseUrl,
            String apiKey,
            String model,
            String providerName) {
        this.testExecutionService = testExecutionService;
        this.apiTestRepository = apiTestRepository;
        this.restClientBuilder = restClientBuilder;
        this.objectMapper = objectMapper;
        this.changelogService = changelogService;
        this.baseUrl = normalizeBaseUrl(baseUrl, DEFAULT_OMNIROUTE_BASE_URL);
        this.apiKey = apiKey;
        this.model = model;
        this.providerName = providerName != null ? providerName : "omniroute";
    }

    public AiFailureAnalysisService(
            TestExecutionService testExecutionService,
            ApiTestRepository apiTestRepository,
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            ChangelogService changelogService,
            String apiKey,
            String model) {
        this(testExecutionService, apiTestRepository, restClientBuilder, objectMapper, changelogService, DEFAULT_OPENROUTER_BASE_URL, apiKey, model, "openrouter");
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getModel() {
        return model;
    }

    public String getProviderName() {
        return providerName;
    }

    private static String normalizeBaseUrl(String url, String defaultUrl) {
        if (url == null || url.isBlank()) {
            return defaultUrl;
        }
        String trimmed = url.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    public AiFailureAnalysisResponse analyzeFailure(Long executionId) {
        TestExecution execution = testExecutionService.getExecutionById(executionId);

        ApiTest apiTest = apiTestRepository.findById(execution.getTestId()).orElse(null);
        Api api = (apiTest != null) ? apiTest.getApi() : null;
        TestExecution previousExecution = testExecutionService.getPreviousExecution(execution.getTestId(), executionId);

        if (apiKey == null || apiKey.isBlank()) {
            String keyName = "gemini".equalsIgnoreCase(providerName) ? "GEMINI_API_KEY" : "OMNIROUTE_API_KEY";
            throw new AiConfigurationException(keyName + " is not configured.");
        }

        String prompt = buildPrompt(execution, apiTest, api, previousExecution);

        Map<String, Object> body;
        String endpointPath;
        if ("gemini".equalsIgnoreCase(providerName)) {
            body = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(
                                    Map.of("text", prompt)
                            ))
                    )
            );
            endpointPath = "/" + model + ":generateContent";
        } else {
            body = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "user", "content", prompt)
                    )
            );
            endpointPath = CHAT_COMPLETIONS_PATH;
        }

        String providerLabel = "gemini".equalsIgnoreCase(providerName) ? "Gemini" : "OmniRoute";
        String logPrefix = providerLabel.toUpperCase();
        long startTime = System.currentTimeMillis();
        log.info("Starting {} AI failure analysis for executionId: {} using model: {}", providerLabel, executionId, model);
        changelogService.log(logPrefix + "_ANALYSIS_START", "executionId=" + executionId + " model=" + model);

        try {
            RestClient client = restClientBuilder.baseUrl(baseUrl).build();

            var requestSpec = client.post()
                    .uri(endpointPath)
                    .contentType(MediaType.APPLICATION_JSON);

            if ("gemini".equalsIgnoreCase(providerName)) {
                requestSpec.header("x-goog-api-key", apiKey);
            } else {
                requestSpec.header("Authorization", "Bearer " + apiKey);
            }

            String jsonResponse = requestSpec
                    .body(body)
                    .retrieve()
                    .body(String.class);

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("{} AI failure analysis completed in {}ms for executionId: {}", providerLabel, elapsed, executionId);
            changelogService.log(logPrefix + "_ANALYSIS_SUCCESS", "executionId=" + executionId + " elapsed=" + elapsed + "ms");

            return parseResponse(jsonResponse, executionId);
        } catch (HttpClientErrorException e) {
            long elapsed = System.currentTimeMillis() - startTime;
            HttpStatus status = (HttpStatus) e.getStatusCode();
            String responseBody = e.getResponseBodyAsString();
            String keyName = "gemini".equalsIgnoreCase(providerName) ? "GEMINI_API_KEY" : "OMNIROUTE_API_KEY";
            if (status == HttpStatus.UNAUTHORIZED || status == HttpStatus.FORBIDDEN) {
                log.error("{} authentication error after {}ms: status={}, response={}", providerLabel, elapsed, status.value(), responseBody);
                changelogService.log(logPrefix + "_ANALYSIS_AUTH_ERROR", "executionId=" + executionId + " status=" + status.value());
                throw new AiConfigurationException(providerLabel + " authentication failed. Check " + keyName + ".");
            }
            if (status == HttpStatus.TOO_MANY_REQUESTS) {
                log.error("{} rate limit exceeded after {}ms: response={}", providerLabel, elapsed, responseBody);
                changelogService.log(logPrefix + "_ANALYSIS_RATE_LIMIT", "executionId=" + executionId);
                throw new AiProviderException(providerLabel + " rate limit exceeded. Retry later.", e);
            }
            log.error("{} client error after {}ms: status={}, response={}", providerLabel, elapsed, status.value(), responseBody);
            changelogService.log(logPrefix + "_ANALYSIS_CLIENT_ERROR", "executionId=" + executionId + " status=" + status.value());
            throw new AiProviderException(providerLabel + " returned client error " + status.value() + ": " + responseBody, e);
        } catch (HttpServerErrorException e) {
            long elapsed = System.currentTimeMillis() - startTime;
            String responseBody = e.getResponseBodyAsString();
            log.error("{} server error after {}ms: status={}, response={}", providerLabel, elapsed, e.getStatusCode().value(), responseBody);
            changelogService.log(logPrefix + "_ANALYSIS_SERVER_ERROR", "executionId=" + executionId + " status=" + e.getStatusCode().value());
            throw new AiProviderException(providerLabel + " server error " + e.getStatusCode().value() + ": " + responseBody, e);
        } catch (ResourceAccessException e) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("{} network or timeout error after {}ms: {}", providerLabel, elapsed, e.getMessage());
            changelogService.log(logPrefix + "_ANALYSIS_NETWORK_ERROR", "executionId=" + executionId);
            throw new AiProviderException(providerLabel + " request failed due to network error or timeout after " + elapsed + "ms", e);
        } catch (NotFoundException | AiConfigurationException | AiProviderException e) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("AI failure analysis failed after {}ms: {}", elapsed, e.getMessage());
            changelogService.log(logPrefix + "_ANALYSIS_FAILED", "executionId=" + executionId);
            throw e;
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("Unexpected error during AI failure analysis after {}ms: {}", elapsed, e.getMessage());
            changelogService.log(logPrefix + "_ANALYSIS_ERROR", "executionId=" + executionId);
            throw new AiProviderException("Unexpected error from " + providerLabel + " after " + elapsed + "ms", e);
        }
    }

    private String buildPrompt(TestExecution execution, ApiTest apiTest, Api api, TestExecution previousExecution) {
        StringBuilder sb = new StringBuilder();
        sb.append("Analyze the following API test execution results and provide a structured root-cause analysis.\n\n");

        sb.append("Execution ID: ").append(execution.getId()).append("\n");
        sb.append("Passed: ").append(execution.isPassed()).append("\n");
        sb.append("Expected Status: ").append(execution.getExpectedStatus()).append("\n");
        sb.append("Actual Status: ").append(execution.getActualStatus() != null ? execution.getActualStatus() : "Null (Connection Failure / Timeout)").append("\n");
        sb.append("Actual Response Time (ms): ").append(execution.getActualResponseTimeMs() != null ? execution.getActualResponseTimeMs() : "N/A").append("\n");

        if (apiTest != null) {
            sb.append("Test Name: ").append(apiTest.getTestName()).append("\n");
            sb.append("Required Fields: ").append(apiTest.getRequiredFields() != null ? apiTest.getRequiredFields() : "None").append("\n");
            sb.append("Max Allowed Response Time (ms): ").append(apiTest.getMaxResponseTimeMs() != null ? apiTest.getMaxResponseTimeMs() : "None").append("\n");
        }

        if (api != null) {
            sb.append("API Name: ").append(api.getName()).append("\n");
            sb.append("API URL: ").append(api.getUrl()).append("\n");
            sb.append("HTTP Method: ").append(api.getMethod()).append("\n");
        }

        if (previousExecution != null) {
            sb.append("Previous Execution Status: ").append(previousExecution.getActualStatus()).append(" (Passed: ").append(previousExecution.isPassed()).append(")\n");
        }

        if (execution.getResponseBody() != null && !execution.getResponseBody().isBlank()) {
            String sanitizedBody = sanitizeResponseBody(execution.getResponseBody());
            sb.append("Response Body (Truncated/Sanitized):\n").append(sanitizedBody).append("\n");
        }

        sb.append("\nRespond with ONLY a valid JSON object matching this exact structure, no markdown fences:\n");
        sb.append("{\n");
        sb.append("  \"executionId\": ").append(execution.getId()).append(",\n");
        sb.append("  \"testId\": ").append(execution.getTestId()).append(",\n");
        sb.append("  \"failureDetected\": ").append(!execution.isPassed()).append(",\n");
        sb.append("  \"summary\": \"Concise summary of what happened\",\n");
        sb.append("  \"likelyRootCause\": \"[HYPOTHESIS] Best explanation for observed failure or success\",\n");
        sb.append("  \"evidence\": \"Observed facts supporting the analysis (status codes, timings, errors)\",\n");
        sb.append("  \"recommendedActions\": \"What the developer should investigate or change\",\n");
        sb.append("  \"nextVerificationSteps\": \"How to confirm the suspected cause\"\n");
        sb.append("}");

        return sb.toString();
    }

    private String sanitizeResponseBody(String body) {
        if (body.length() > 1000) {
            body = body.substring(0, 1000) + "... [truncated]";
        }
        body = body.replaceAll("(?i)(password|secret|token|api_key|key)\\s*[:=]\\s*[\"'][^\"']+[\"']", "$1: [REDACTED]");
        return body;
    }

    private AiFailureAnalysisResponse parseResponse(String jsonResponse, Long executionId) {
        String providerLabel = "gemini".equalsIgnoreCase(providerName) ? "Gemini" : ("openrouter".equalsIgnoreCase(providerName) ? "OpenRouter" : "OmniRoute");
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode contentNode;
            if ("gemini".equalsIgnoreCase(providerName)) {
                contentNode = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");
            } else {
                contentNode = root.path("choices").path(0).path("message").path("content");
            }
            if (contentNode.isMissingNode() || contentNode.asText().isBlank()) {
                log.error("{} response missing content node for executionId: {}", providerLabel, executionId);
                throw new AiProviderException(providerLabel + " returned an empty or malformed response", new IllegalStateException("Missing content node"));
            }
            String content = contentNode.asText().trim();
            if (content.startsWith("```")) {
                content = content.replaceAll("^```[a-z]*\\r?\\n?", "").replaceAll("\\r?\\n?```$", "").trim();
            }
            AiFailureAnalysisResponse response = objectMapper.readValue(content, AiFailureAnalysisResponse.class);
            validateAnalysisResponse(response);
            return response;
        } catch (AiProviderException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to parse {} response for executionId {}: {}", providerLabel, executionId, e.getMessage());
            throw new AiProviderException("Failed to parse " + providerLabel + " response", e);
        }
    }

    private void validateAnalysisResponse(AiFailureAnalysisResponse response) {
        if (response == null) {
            throw new AiProviderException("AI failure analysis response is null", new IllegalArgumentException());
        }
        if (response.getExecutionId() == null) {
            throw new AiProviderException("AI failure analysis response missing executionId", new IllegalArgumentException());
        }
        if (response.getTestId() == null) {
            throw new AiProviderException("AI failure analysis response missing testId", new IllegalArgumentException());
        }
        if (response.getSummary() == null || response.getSummary().isBlank()) {
            throw new AiProviderException("AI failure analysis response missing summary", new IllegalArgumentException());
        }
        if (response.getLikelyRootCause() == null || response.getLikelyRootCause().isBlank()) {
            throw new AiProviderException("AI failure analysis response missing likelyRootCause", new IllegalArgumentException());
        }
        if (response.getEvidence() == null || response.getEvidence().isBlank()) {
            throw new AiProviderException("AI failure analysis response missing evidence", new IllegalArgumentException());
        }
    }
}
