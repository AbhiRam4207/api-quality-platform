package api_quality_platform.controller;

import api_quality_platform.dto.ApiCreateRequest;
import api_quality_platform.dto.ApiResponse;
import api_quality_platform.dto.ApiTestSuggestionResponse;
import api_quality_platform.entity.Api;
import api_quality_platform.service.ApiService;
import api_quality_platform.service.TestSuggestionService;
import api_quality_platform.dto.AiSuggestionRequest;
import api_quality_platform.service.AiTestSuggestionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/apis")
public class ApiController {

    private final ApiService apiService;
    private final TestSuggestionService testSuggestionService;
    private final AiTestSuggestionService aiTestSuggestionService;

    public ApiController(
            ApiService apiService,
            TestSuggestionService testSuggestionService,
            AiTestSuggestionService aiTestSuggestionService) {
        this.apiService = apiService;
        this.testSuggestionService = testSuggestionService;
        this.aiTestSuggestionService = aiTestSuggestionService;
    }

    @PostMapping
    public ApiResponse createApi(@Valid @RequestBody ApiCreateRequest request) {
        Api api = new Api();
        api.setName(request.getName());
        api.setUrl(request.getUrl());
        api.setMethod(request.getMethod());
        api.setAuthType(request.getAuthType());
        api.setUsername(request.getUsername());
        api.setPassword(request.getPassword());
        api.setToken(request.getToken());
        api.setApiKey(request.getApiKey());
        api.setApiKeyHeader(request.getApiKeyHeader());

        Api saved = apiService.createApi(api);
        return toResponse(saved);
    }

    @GetMapping
    public List<ApiResponse> getAllApis() {
        return apiService.getAllApis().stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/{apiId}/suggestions")
    public ApiTestSuggestionResponse getTestSuggestions(@PathVariable Long apiId) {
        return testSuggestionService.generateSuggestions(apiId);
    }

    @PostMapping("/{apiId}/ai-suggestions")
    public ApiTestSuggestionResponse getAiTestSuggestions(
            @PathVariable Long apiId,
            @RequestBody(required = false) AiSuggestionRequest request) {
        return aiTestSuggestionService.generateAiSuggestions(apiId, request);
    }

    private ApiResponse toResponse(Api api) {
        return new ApiResponse(
                api.getId(),
                api.getName(),
                api.getUrl(),
                api.getMethod(),
                api.getAuthType()
        );
    }
}
