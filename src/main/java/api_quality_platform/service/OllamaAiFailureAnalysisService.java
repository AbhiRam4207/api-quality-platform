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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class OllamaAiFailureAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(OllamaAiFailureAnalysisService.class);

    private final TestExecutionService testExecutionService;
    private final ApiTestRepository apiTestRepository;
    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper objectMapper;
    private final ChangelogService changelogService;
    private final String apiKey;
    private final String model;

    private static final String OLLAMA_BASE_URL = "https://ollama.com";

    public OllamaAiFailureAnalysisService(
            TestExecutionService testExecutionService,
            ApiTestRepository apiTestRepository,
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            ChangelogService changelogService,
            @Value("${OLLAMA_API_KEY:}") String apiKey,
            @Value("${OLLAMA_MODEL:gemma4:31b}") String model) {

        this.testExecutionService = testExecutionService;
        this.apiTestRepository = apiTestRepository;
        this.restClientBuilder = restClientBuilder;
        this.objectMapper = objectMapper;
        this.changelogService = changelogService;
        this.apiKey = apiKey;
        this.model = model;
    }

    public AiFailureAnalysisResponse analyzeFailure(Long executionId) {
        TestExecution execution = testExecutionService.getExecutionById(executionId);

        ApiTest apiTest = apiTestRepository.findById(execution.getTestId()).orElse(null);
        Api api = (apiTest != null) ? apiTest.getApi() : null;
        TestExecution previousExecution = testExecutionService.getPreviousExecution(execution.getTestId(), executionId);

        if (apiKey == null || apiKey.isBlank()) {
            throw new AiConfigurationException("OLLAMA_API_KEY is missing.");
        }

        String prompt = buildPrompt(execution, apiTest, api, previousExecution);

        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "user", "content", prompt)
                ),
                "stream", false
        );

        long startTime = System.currentTimeMillis();
        log.info("Starting Ollama AI failure analysis for executionId: {} using model: {}", executionId, model);
        changelogService.log("OLLAMA_ANALYSIS_START", "executionId=" + executionId + " model=" + model);

        try {
            RestClient client = restClientBuilder.baseUrl(OLLAMA_BASE_URL).build();
            String jsonResponse = client.post()
                    .uri("/api/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + apiKey)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("Successfully completed Ollama AI failure analysis in {}ms for executionId: {}", elapsed, executionId);
            changelogService.log("OLLAMA_ANALYSIS_SUCCESS", "executionId=" + executionId + " elapsed=" + elapsed + "ms");

            return parseOllamaResponse(jsonResponse);
        } catch (NotFoundException | AiConfigurationException | AiProviderException e) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("Ollama failure analysis failed after {}ms: {}", elapsed, e.getMessage());
            changelogService.log("OLLAMA_ANALYSIS_FAILED", "executionId=" + executionId + " error=" + e.getMessage());
            throw e;
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("Ollama failure analysis provider error after {}ms: {}", elapsed, e.getMessage());
            changelogService.log("OLLAMA_ANALYSIS_ERROR", "executionId=" + executionId + " error=" + e.getMessage());
            throw new AiProviderException("Ollama provider request failed after " + elapsed + "ms", e);
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

        sb.append("\nProvide a JSON object with this exact structure:\n");
        sb.append("{\n");
        sb.append("  \"executionId\": ").append(execution.getId()).append(",\n");
        sb.append("  \"testId\": ").append(execution.getTestId()).append(",\n");
        sb.append("  \"failureDetected\": ").append(!execution.isPassed()).append(",\n");
        sb.append("  \"summary\": \"Concise summary of what happened\",\n");
        sb.append("  \"likelyRootCause\": \"Best explanation for observed failure or success\",\n");
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

    private AiFailureAnalysisResponse parseOllamaResponse(String jsonResponse) throws Exception {
        JsonNode root = objectMapper.readTree(jsonResponse);
        JsonNode contentNode = root.path("message").path("content");
        String content = contentNode.isMissingNode() ? jsonResponse : contentNode.asText();

        return objectMapper.readValue(content, AiFailureAnalysisResponse.class);
    }
}
