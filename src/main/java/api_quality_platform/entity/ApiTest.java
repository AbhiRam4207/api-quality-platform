package api_quality_platform.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "api_tests")
public class ApiTest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String testName;

    private Integer expectedStatus;

    @Column(name = "required_fields", columnDefinition = "TEXT")
    private String requiredFields;

    @Column(name = "expected_field_types", columnDefinition = "TEXT")
    private String expectedFieldTypes;

    @Column(name = "request_body", columnDefinition = "TEXT")
    private String requestBody;

    @Column(name = "headers", columnDefinition = "TEXT")
    private String headers;

    @Column(name = "query_params", columnDefinition = "TEXT")
    private String queryParams;

    @Column(name = "path_params", columnDefinition = "TEXT")
    private String pathParams;

    private Long maxResponseTimeMs;

    private Long timeoutMs;

    private Integer regressionResponseTimeThresholdPercent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "api_id")
    private Api api;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public Api getApi() {
        return api;
    }

    public void setApi(Api api) {
        this.api = api;
    }
}
