package api_quality_platform.dto;

import java.util.List;

public class ApiTestSuggestionResponse {

    private Long apiId;
    private String apiName;
    private List<SuggestionItem> suggestions;

    public ApiTestSuggestionResponse() {
    }

    public ApiTestSuggestionResponse(Long apiId, String apiName, List<SuggestionItem> suggestions) {
        this.apiId = apiId;
        this.apiName = apiName;
        this.suggestions = suggestions;
    }

    public Long getApiId() {
        return apiId;
    }

    public void setApiId(Long apiId) {
        this.apiId = apiId;
    }

    public String getApiName() {
        return apiName;
    }

    public void setApiName(String apiName) {
        this.apiName = apiName;
    }

    public List<SuggestionItem> getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(List<SuggestionItem> suggestions) {
        this.suggestions = suggestions;
    }

    public static class SuggestionItem {
        private String testName;
        private String description;
        private ApiTestCreateRequest testRequest;

        public SuggestionItem() {
        }

        public SuggestionItem(String testName, String description, ApiTestCreateRequest testRequest) {
            this.testName = testName;
            this.description = description;
            this.testRequest = testRequest;
        }

        public String getTestName() {
            return testName;
        }

        public void setTestName(String testName) {
            this.testName = testName;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public ApiTestCreateRequest getTestRequest() {
            return testRequest;
        }

        public void setTestRequest(ApiTestCreateRequest testRequest) {
            this.testRequest = testRequest;
        }
    }
}
