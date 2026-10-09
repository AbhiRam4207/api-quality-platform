package api_quality_platform.controller;

import api_quality_platform.dto.ApiCreateRequest;
import api_quality_platform.dto.ApiResponse;
import api_quality_platform.entity.Api;
import api_quality_platform.service.ApiService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/apis")
public class ApiController {

    private final ApiService apiService;

    public ApiController(ApiService apiService) {
        this.apiService = apiService;
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
