package api_quality_platform.dto;

public class ApiTestCreateRequest {

    private String testName;

    private Integer expectedStatus;

    private String authType;

    private String username;

    private String password;

    private String requiredFields;

    private String expectedFieldTypes;

    private String requestBody;

    private String headers;

    private String queryParams;

    private String pathParams;

    private Long maxResponseTimeMs;

    private Long timeoutMs;

    private Integer regressionResponseTimeThresholdPercent;

    public String getTestName() {
        return testName;
    }

    public void setTestName(String testName) {
        this.testName = testName;
    }

    public Integer getExpectedStatus() {
        return expectedStatus;
    }

    public void setExpectedStatus(Integer expectedStatus) {
        this.expectedStatus = expectedStatus;
    }

    public String getAuthType() {
        return authType;
    }

    public void setAuthType(String authType) {
        this.authType = authType;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRequiredFields() {
        return requiredFields;
    }

    public void setRequiredFields(String requiredFields) {
        this.requiredFields = requiredFields;
    }

    public String getExpectedFieldTypes() {
        return expectedFieldTypes;
    }

    public void setExpectedFieldTypes(String expectedFieldTypes) {
        this.expectedFieldTypes = expectedFieldTypes;
    }

    public String getRequestBody() {
        return requestBody;
    }

    public void setRequestBody(String requestBody) {
        this.requestBody = requestBody;
    }

    public String getHeaders() {
        return headers;
    }

    public void setHeaders(String headers) {
        this.headers = headers;
    }

    public String getQueryParams() {
        return queryParams;
    }

    public void setQueryParams(String queryParams) {
        this.queryParams = queryParams;
    }

    public String getPathParams() {
        return pathParams;
    }

    public void setPathParams(String pathParams) {
        this.pathParams = pathParams;
    }

    public Long getMaxResponseTimeMs() {
        return maxResponseTimeMs;
    }

    public void setMaxResponseTimeMs(Long maxResponseTimeMs) {
        this.maxResponseTimeMs = maxResponseTimeMs;
    }

    public Long getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(Long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public Integer getRegressionResponseTimeThresholdPercent() {
        return regressionResponseTimeThresholdPercent;
    }

    public void setRegressionResponseTimeThresholdPercent(Integer regressionResponseTimeThresholdPercent) {
        this.regressionResponseTimeThresholdPercent = regressionResponseTimeThresholdPercent;
    }
}
