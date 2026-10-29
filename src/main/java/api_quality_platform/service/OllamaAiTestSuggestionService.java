package api_quality_platform.service;

import api_quality_platform.dto.AiSuggestionRequest;
import api_quality_platform.dto.ApiTestSuggestionResponse;
import api_quality_platform.entity.Api;
import api_quality_platform.exception.AiConfigurationException;
import api_quality_platform.exception.AiProviderException;
import api_quality_platform.exception.NotFoundException;
import api_quality_platform.repository.ApiRepository;
import com.fasterxml.jackson.core.type.TypeReference;
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
public class OllamaAiTestSuggestionService {

    private static final Logger log = LoggerFactory.getLogger(OllamaAiTestSuggestionService.class);

    private final ApiRepository apiRepository;
    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper objectMapper;
    private final ChangelogService changelogService;
    private final String apiKey;
    private final String model;

    private static final String OLLAMA_BASE_URL = "https://ollama.com";

    public OllamaAiTestSuggestionService(
            ApiRepository apiRepository,
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            ChangelogService changelogService,
            @Value("${OLLAMA_API_KEY:}") String apiKey,
            @Value("${OLLAMA_MODEL:gemma4:31b}") String model) {

        this.apiRepository = apiRepository;
        this.restClientBuilder = restClientBuilder;
        this.objectMapper = objectMapper;
        this.changelogService = changelogService;
        this.apiKey = apiKey;
        this.model = model;
    }

    public ApiTestSuggestionResponse generateAiSuggestions(Long apiId, AiSuggestionRequest request) {
        Api api = apiRepository.findById(apiId)
                .orElseThrow(() -> new NotFoundException("Api not found with id: " + apiId));

        if (apiKey == null || apiKey.isBlank()) {
            throw new AiConfigurationException("OLLAMA_API_KEY is missing.");
        }

        String prompt = buildPrompt(api, request);

        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "user", "content", prompt)
                ),
                "stream", false
        );

        long startTime = System.currentTimeMillis();
        log.info("Starting Ollama AI test suggestion generation for apiId: {} using model: {}", apiId, model);
        changelogService.log("OLLAMA_SUGGESTION_START", "apiId=" + apiId + " model=" + model);

        try {
            RestClient client = restClientBuilder
                    .baseUrl(OLLAMA_BASE_URL)
                    .build();

            String jsonResponse = client.post()
                    .uri("/api/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + apiKey)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("Successfully completed Ollama AI test suggestion in {}ms for apiId: {}", elapsed, apiId);
            changelogService.log("OLLAMA_SUGGESTION_SUCCESS", "apiId=" + apiId + " elapsed=" + elapsed + "ms");

            return parseOllamaResponse(jsonResponse, api);
        } catch (NotFoundException | AiConfigurationException | AiProviderException e) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("Ollama suggestion failed after {}ms: {}", elapsed, e.getMessage());
            changelogService.log("OLLAMA_SUGGESTION_FAILED", "apiId=" + apiId + " error=" + e.getMessage());
            throw e;
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("Ollama provider error after {}ms: {}", elapsed, e.getMessage());
            changelogService.log("OLLAMA_SUGGESTION_ERROR", "apiId=" + apiId + " error=" + e.getMessage());
            throw new AiProviderException("Ollama provider request failed after " + elapsed + "ms", e);
        }
    }

    private String buildPrompt(Api api, AiSuggestionRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("Generate comprehensive API test suggestions for the following registered API across multiple test scenarios (e.g. successful response, invalid/missing input, authentication failure when applicable, missing resource, and response-field/data-type checks):\n");
        sb.append("- Name: ").append(api.getName()).append("\n");
        sb.append("- URL: ").append(api.getUrl()).append("\n");
        sb.append("- Method: ").append(api.getMethod()).append("\n");
        sb.append("- Auth Type: ").append(api.getAuthType()).append("\n");

        if (request != null) {
            if (request.getSampleResponse() != null && !request.getSampleResponse().isBlank()) {
                sb.append("\nSample Response:\n").append(request.getSampleResponse()).append("\n");
            }
            if (request.getOpenApiSchema() != null && !request.getOpenApiSchema().isBlank()) {
                sb.append("\nOpenAPI Schema:\n").append(request.getOpenApiSchema()).append("\n");
            }
        }

        sb.append("\nProvide a JSON object with this exact structure:\n");
        sb.append("{\n");
        sb.append("  \"apiId\": ").append(api.getId()).append(",\n");
        sb.append("  \"apiName\": \"").append(api.getName()).append("\",\n");
        sb.append("  \"suggestions\": [\n");
        sb.append("    {\n");
        sb.append("      \"testName\": \"String\",\n");
        sb.append("      \"description\": \"String\",\n");
        sb.append("      \"testRequest\": {\n");
        sb.append("        \"testName\": \"String\",\n");
        sb.append("        \"expectedStatus\": 200,\n");
        sb.append("        \"requiredFields\": \"comma-separated-fields\",\n");
        sb.append("        \"expectedFieldTypes\": \"json-map-string\",\n");
        sb.append("        \"requestBody\": \"optional-json-body\",\n");
        sb.append("        \"maxResponseTimeMs\": 2000,\n");
        sb.append("        \"timeoutMs\": 5000\n");
        sb.append("      }\n");
        sb.append("    }\n");
        sb.append("  ]\n");
        sb.append("}");
        return sb.toString();
    }

    private ApiTestSuggestionResponse parseOllamaResponse(String jsonResponse, Api api) throws Exception {
        JsonNode root = objectMapper.readTree(jsonResponse);
        JsonNode contentNode = root.path("message").path("content");
        String content = contentNode.isMissingNode() ? jsonResponse : contentNode.asText();
        ApiTestSuggestionResponse response = objectMapper.readValue(content, ApiTestSuggestionResponse.class);
        validateSuggestionResponse(response, api);
        return response;
    }

    private void validateSuggestionResponse(ApiTestSuggestionResponse response, Api api) {
        if (response == null) {
            throw new AiProviderException("AI suggestion response is null", new IllegalArgumentException());
        }
        if (response.getApiId() == null) {
            throw new AiProviderException("AI suggestion response missing apiId", new IllegalArgumentException());
        }
        if (response.getApiName() == null || response.getApiName().isBlank()) {
            throw new AiProviderException("AI suggestion response missing apiName", new IllegalArgumentException());
        }
        if (response.getSuggestions() == null || response.getSuggestions().isEmpty()) {
            throw new AiProviderException("AI suggestion response contains no suggestions", new IllegalArgumentException());
        }

        boolean isShopApi = api.getName() != null && api.getName().toLowerCase().contains("shopapi");
        List<String> confirmedShopApiFields = List.of("id", "name", "price", "category", "stockQuantity");

        for (ApiTestSuggestionResponse.SuggestionItem item : response.getSuggestions()) {
            if (item == null) {
                throw new AiProviderException("AI suggestion item is null", new IllegalArgumentException());
            }
            if (item.getTestName() == null || item.getTestName().isBlank()) {
                throw new AiProviderException("AI suggestion item missing testName", new IllegalArgumentException());
            }
            api_quality_platform.dto.ApiTestCreateRequest req = item.getTestRequest();
            if (req == null) {
                throw new AiProviderException("AI suggestion item missing testRequest", new IllegalArgumentException());
            }
            Integer status = req.getExpectedStatus();
            if (status == null || status < 100 || status > 599) {
                throw new AiProviderException("AI suggestion item has invalid expectedStatus: " + status, new IllegalArgumentException());
            }

            if (isShopApi && (status == 400 || status == 404 || status == 500)) {
                throw new AiProviderException("Unverified expected status code " + status + " for ShopAPI V1 without contract evidence", new IllegalArgumentException());
            }

            String fieldTypes = req.getExpectedFieldTypes();
            if (fieldTypes != null && !fieldTypes.isBlank()) {
                String trimmedTypes = fieldTypes.trim();
                if (!trimmedTypes.startsWith("{") || !trimmedTypes.endsWith("}") || trimmedTypes.contains(":int") || (trimmedTypes.contains(",") && !trimmedTypes.contains("\""))) {
                    throw new AiProviderException("expectedFieldTypes must be a valid JSON map, not CSV", new IllegalArgumentException());
                }
                try {
                    Map<String, String> typeMap = objectMapper.readValue(
                            fieldTypes,
                            new TypeReference<Map<String, String>>() {}
                    );
                    if (isShopApi) {
                        for (String key : typeMap.keySet()) {
                            if (!confirmedShopApiFields.contains(key)) {
                                throw new AiProviderException("Unsupported field in expectedFieldTypes for ShopAPI V1: " + key, new IllegalArgumentException());
                            }
                        }
                    }
                } catch (AiProviderException e) {
                    throw e;
                } catch (Exception e) {
                    throw new AiProviderException("expectedFieldTypes is not a valid JSON map: " + e.getMessage(), new IllegalArgumentException());
                }
            }

            String requiredFields = req.getRequiredFields();
            if (requiredFields != null && !requiredFields.isBlank()) {
                String[] fields = requiredFields.split(",");
                for (String field : fields) {
                    String f = field.trim();
                    if (f.isEmpty()) continue;
                    if (isShopApi && !confirmedShopApiFields.contains(f)) {
                        throw new AiProviderException("Unsupported field in requiredFields for ShopAPI V1: " + f, new IllegalArgumentException());
                    }
                }
            }

            String method = api.getMethod() != null ? api.getMethod().toUpperCase() : "GET";
            String body = req.getRequestBody();
            if (body != null) {
                String trimmedBody = body.trim();
                if ("GET".equals(method) && ("null".equalsIgnoreCase(trimmedBody) || "\"null\"".equalsIgnoreCase(trimmedBody))) {
                    throw new AiProviderException("GET suggestions cannot have a requestBody of literal null", new IllegalArgumentException());
                }
                if (trimmedBody.startsWith("?") || (trimmedBody.contains("=") && trimmedBody.contains("&") && !trimmedBody.startsWith("{"))) {
                    throw new AiProviderException("Query parameters must use queryParams, not requestBody", new IllegalArgumentException());
                }
            }

            String testNameLower = (item.getTestName() != null ? item.getTestName() : "").toLowerCase();
            String descLower = (item.getDescription() != null ? item.getDescription() : "").toLowerCase();
            boolean claimsAuth = testNameLower.contains("credential") || testNameLower.contains("auth") || testNameLower.contains("unauthorized") ||
                    descLower.contains("credential") || descLower.contains("auth") || descLower.contains("unauthorized") ||
                    req.getAuthType() != null;

            if (claimsAuth) {
                if (req.getAuthType() == null || req.getAuthType().isBlank()) {
                    throw new AiProviderException("Authentication configuration cannot be null or blank for test claiming credentials or auth", new IllegalArgumentException());
                }
                if (!"NONE".equalsIgnoreCase(req.getAuthType()) && (req.getUsername() == null || req.getUsername().isBlank())) {
                    throw new AiProviderException("Authentication configuration missing credentials for auth test", new IllegalArgumentException());
                }
                if ("NONE".equalsIgnoreCase(req.getAuthType()) && ("BASIC".equalsIgnoreCase(api.getAuthType()) || api.getUsername() != null)) {
                    throw new AiProviderException("Authentication mismatch: test uses NONE auth but API requires authentication", new IllegalArgumentException());
                }
            }
        }
    }
}
