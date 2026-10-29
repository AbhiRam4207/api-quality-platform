package api_quality_platform.service;

import api_quality_platform.dto.AiSuggestionRequest;
import api_quality_platform.dto.ApiTestCreateRequest;
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
public class AiTestSuggestionService {

    private static final Logger log = LoggerFactory.getLogger(AiTestSuggestionService.class);

    private static final String DEFAULT_GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models";
    private static final String DEFAULT_OMNIROUTE_BASE_URL = "http://localhost:20128/v1";
    private static final String DEFAULT_OPENROUTER_BASE_URL = "https://openrouter.ai/api/v1";
    private static final String CHAT_COMPLETIONS_PATH = "/chat/completions";

    private final ApiRepository apiRepository;
    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper objectMapper;
    private final ChangelogService changelogService;
    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final String providerName;

    @Autowired
    public AiTestSuggestionService(
            ApiRepository apiRepository,
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

        this.apiRepository = apiRepository;
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

    public AiTestSuggestionService(
            ApiRepository apiRepository,
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            ChangelogService changelogService,
            String baseUrl,
            String apiKey,
            String model) {
        this(apiRepository, restClientBuilder, objectMapper, changelogService, baseUrl, apiKey, model, "omniroute");
    }

    public AiTestSuggestionService(
            ApiRepository apiRepository,
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            ChangelogService changelogService,
            String baseUrl,
            String apiKey,
            String model,
            String providerName) {
        this.apiRepository = apiRepository;
        this.restClientBuilder = restClientBuilder;
        this.objectMapper = objectMapper;
        this.changelogService = changelogService;
        this.baseUrl = normalizeBaseUrl(baseUrl, DEFAULT_OMNIROUTE_BASE_URL);
        this.apiKey = apiKey;
        this.model = model;
        this.providerName = providerName != null ? providerName : "omniroute";
    }

    public AiTestSuggestionService(
            ApiRepository apiRepository,
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            ChangelogService changelogService,
            String apiKey,
            String model) {
        this(apiRepository, restClientBuilder, objectMapper, changelogService, DEFAULT_OPENROUTER_BASE_URL, apiKey, model, "openrouter");
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

    public ApiTestSuggestionResponse generateAiSuggestions(Long apiId, AiSuggestionRequest request) {
        Api api = apiRepository.findById(apiId)
                .orElseThrow(() -> new NotFoundException("Api not found with id: " + apiId));

        if (apiKey == null || apiKey.isBlank()) {
            String keyName = "gemini".equalsIgnoreCase(providerName) ? "GEMINI_API_KEY" : "OMNIROUTE_API_KEY";
            throw new AiConfigurationException(keyName + " is not configured.");
        }

        String prompt = buildPrompt(api, request);

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
        log.info("Starting {} AI test suggestion for apiId: {} using model: {}", providerLabel, apiId, model);
        changelogService.log(logPrefix + "_SUGGESTION_START", "apiId=" + apiId + " model=" + model);

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
            log.info("{} AI test suggestion completed in {}ms for apiId: {}", providerLabel, elapsed, apiId);
            changelogService.log(logPrefix + "_SUGGESTION_SUCCESS", "apiId=" + apiId + " elapsed=" + elapsed + "ms");

            return parseResponse(jsonResponse, api);
        } catch (HttpClientErrorException e) {
            long elapsed = System.currentTimeMillis() - startTime;
            HttpStatus status = (HttpStatus) e.getStatusCode();
            String responseBody = e.getResponseBodyAsString();
            String keyName = "gemini".equalsIgnoreCase(providerName) ? "GEMINI_API_KEY" : "OMNIROUTE_API_KEY";
            if (status == HttpStatus.UNAUTHORIZED || status == HttpStatus.FORBIDDEN) {
                log.error("{} authentication error after {}ms: status={}, response={}", providerLabel, elapsed, status.value(), responseBody);
                changelogService.log(logPrefix + "_SUGGESTION_AUTH_ERROR", "apiId=" + apiId + " status=" + status.value());
                throw new AiConfigurationException(providerLabel + " authentication failed. Check " + keyName + ".");
            }
            if (status == HttpStatus.TOO_MANY_REQUESTS) {
                log.error("{} rate limit exceeded after {}ms: response={}", providerLabel, elapsed, responseBody);
                changelogService.log(logPrefix + "_SUGGESTION_RATE_LIMIT", "apiId=" + apiId);
                throw new AiProviderException(providerLabel + " rate limit exceeded. Retry later.", e);
            }
            log.error("{} client error after {}ms: status={}, response={}", providerLabel, elapsed, status.value(), responseBody);
            changelogService.log(logPrefix + "_SUGGESTION_CLIENT_ERROR", "apiId=" + apiId + " status=" + status.value());
            throw new AiProviderException(providerLabel + " returned client error " + status.value() + ": " + responseBody, e);
        } catch (HttpServerErrorException e) {
            long elapsed = System.currentTimeMillis() - startTime;
            String responseBody = e.getResponseBodyAsString();
            log.error("{} server error after {}ms: status={}, response={}", providerLabel, elapsed, e.getStatusCode().value(), responseBody);
            changelogService.log(logPrefix + "_SUGGESTION_SERVER_ERROR", "apiId=" + apiId + " status=" + e.getStatusCode().value());
            throw new AiProviderException(providerLabel + " server error " + e.getStatusCode().value() + ": " + responseBody, e);
        } catch (ResourceAccessException e) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("{} network or timeout error after {}ms: {}", providerLabel, elapsed, e.getMessage());
            changelogService.log(logPrefix + "_SUGGESTION_NETWORK_ERROR", "apiId=" + apiId);
            throw new AiProviderException(providerLabel + " request failed due to network error or timeout after " + elapsed + "ms", e);
        } catch (NotFoundException | AiConfigurationException | AiProviderException e) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("AI suggestion failed after {}ms: {}", elapsed, e.getMessage());
            changelogService.log(logPrefix + "_SUGGESTION_FAILED", "apiId=" + apiId);
            throw e;
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("Unexpected error during AI suggestion after {}ms: class={}, message={}", elapsed, e.getClass().getName(), e.getMessage(), e);
            changelogService.log(logPrefix + "_SUGGESTION_ERROR", "apiId=" + apiId);
            throw new AiProviderException("Unexpected error from " + providerLabel + " after " + elapsed + "ms: " + e.getMessage(), e);
        }
    }

    private String buildPrompt(Api api, AiSuggestionRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("Generate comprehensive, contract-aware API test suggestions for the following registered API across multiple test scenarios (e.g. successful response with confirmed fields, required field checks, data type validations, and authentication tests when applicable):\n");
        sb.append("- Name: ").append(api.getName()).append("\n");
        sb.append("- URL: ").append(api.getUrl()).append("\n");
        sb.append("- Method: ").append(api.getMethod()).append("\n");
        sb.append("- Auth Type: ").append(api.getAuthType()).append("\n");

        if (api.getName() != null && api.getName().toLowerCase().contains("shopapi")) {
            sb.append("\nConfirmed ShopAPI V1 Contract Fields & Types (Use JSON Map format for expectedFieldTypes):\n");
            sb.append("- id: NUMBER\n");
            sb.append("- name: STRING\n");
            sb.append("- price: NUMBER\n");
            sb.append("- category: STRING\n");
            sb.append("- stockQuantity: NUMBER\n");
            sb.append("Example expectedFieldTypes JSON: {\"id\": \"NUMBER\", \"name\": \"STRING\", \"price\": \"NUMBER\"}. Do not use CSV format (e.g. id:int).\n");
        }

        if (request != null) {
            if (request.getSampleResponse() != null && !request.getSampleResponse().isBlank()) {
                sb.append("\nSample Response:\n").append(request.getSampleResponse()).append("\n");
            }
            if (request.getOpenApiSchema() != null && !request.getOpenApiSchema().isBlank()) {
                sb.append("\nOpenAPI Schema:\n").append(request.getOpenApiSchema()).append("\n");
            }
        }

        sb.append("\nInstructions:\n");
        sb.append("1. Distinguish confirmed contract behavior from assumptions. Do not invent query parameters, response fields, or status codes without evidence.\n");
        sb.append("2. When suggesting authentication tests, populate authType, username, and password in testRequest appropriately when testing auth failure or success.\n");

        sb.append("\nRespond with ONLY a valid JSON object matching this exact structure, no markdown fences:\n");
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
        sb.append("        \"authType\": \"NONE/BASIC\",\n");
        sb.append("        \"username\": \"string\",\n");
        sb.append("        \"password\": \"string\",\n");
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

    private ApiTestSuggestionResponse parseResponse(String jsonResponse, Api api) {
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
                log.error("{} response missing content node for apiId: {}", providerLabel, api.getId());
                throw new AiProviderException(providerLabel + " returned an empty or malformed response", new IllegalStateException("Missing content node"));
            }
            String content = contentNode.asText().trim();
            if (content.startsWith("```")) {
                content = content.replaceAll("^```[a-z]*\\r?\\n?", "").replaceAll("\\r?\\n?```$", "").trim();
            }
            ApiTestSuggestionResponse response = objectMapper.readValue(content, ApiTestSuggestionResponse.class);
            validateSuggestionResponse(response, api);
            return response;
        } catch (AiProviderException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to parse {} response for apiId {}: {}", providerLabel, api.getId(), e.getMessage());
            throw new AiProviderException("Failed to parse " + providerLabel + " response", e);
        }
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
            ApiTestCreateRequest req = item.getTestRequest();
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
