package api_quality_platform.service;

import api_quality_platform.dto.ApiTestCreateRequest;
import api_quality_platform.dto.ApiTestSuggestionResponse;
import api_quality_platform.entity.Api;
import api_quality_platform.exception.NotFoundException;
import api_quality_platform.repository.ApiRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TestSuggestionService {

    private final ApiRepository apiRepository;

    public TestSuggestionService(ApiRepository apiRepository) {
        this.apiRepository = apiRepository;
    }

    public ApiTestSuggestionResponse generateSuggestions(Long apiId) {
        Api api = apiRepository.findById(apiId)
                .orElseThrow(() -> new NotFoundException("Api not found with id: " + apiId));

        String method = api.getMethod() != null ? api.getMethod().toUpperCase() : "GET";
        int expectedStatus = "POST".equals(method) ? 201 : 200;

        List<ApiTestSuggestionResponse.SuggestionItem> suggestions = new ArrayList<>();

        // Suggestion 1: Standard Success Test
        ApiTestCreateRequest req1 = new ApiTestCreateRequest();
        req1.setTestName("Standard Success Test (" + method + ")");
        req1.setExpectedStatus(expectedStatus);
        req1.setMaxResponseTimeMs(2000L);
        req1.setTimeoutMs(5000L);
        suggestions.add(new ApiTestSuggestionResponse.SuggestionItem(
                "Standard Success Test",
                "Validates that the endpoint returns expected success status and responds within 2000ms SLA.",
                req1
        ));

        // Suggestion 2: Performance SLA Test
        ApiTestCreateRequest req2 = new ApiTestCreateRequest();
        req2.setTestName("Performance SLA Test");
        req2.setExpectedStatus(expectedStatus);
        req2.setMaxResponseTimeMs(500L);
        req2.setTimeoutMs(3000L);
        suggestions.add(new ApiTestSuggestionResponse.SuggestionItem(
                "Performance SLA Test",
                "Verifies strict performance SLA compliance under 500ms.",
                req2
        ));

        // Suggestion 3: Strict Status Code Validation
        ApiTestCreateRequest req3 = new ApiTestCreateRequest();
        req3.setTestName("Strict Status Code Validation");
        req3.setExpectedStatus(expectedStatus);
        suggestions.add(new ApiTestSuggestionResponse.SuggestionItem(
                "Strict Status Code Validation",
                "Confirms exact HTTP status code match without response body constraints.",
                req3
        ));

        return new ApiTestSuggestionResponse(api.getId(), api.getName(), suggestions);
    }
}
