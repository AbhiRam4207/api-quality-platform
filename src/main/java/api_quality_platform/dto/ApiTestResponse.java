package api_quality_platform.dto;

public class ApiTestResponse {

    private final Long id;
    private final String testName;
    private final Integer expectedStatus;
    private final Long maxResponseTimeMs;
    private final Long timeoutMs;
    private final Integer regressionResponseTimeThresholdPercent;

    public ApiTestResponse(Long id, String testName, Integer expectedStatus) {
        this(id, testName, expectedStatus, null, null, null);
    }

    public ApiTestResponse(Long id, String testName, Integer expectedStatus, Long maxResponseTimeMs) {
        this(id, testName, expectedStatus, maxResponseTimeMs, null, null);
    }

    public ApiTestResponse(Long id, String testName, Integer expectedStatus, Long maxResponseTimeMs, Long timeoutMs) {
        this(id, testName, expectedStatus, maxResponseTimeMs, timeoutMs, null);
    }

    public ApiTestResponse(Long id, String testName, Integer expectedStatus, Long maxResponseTimeMs, Long timeoutMs, Integer regressionResponseTimeThresholdPercent) {
        this.id = id;
        this.testName = testName;
        this.expectedStatus = expectedStatus;
        this.maxResponseTimeMs = maxResponseTimeMs;
        this.timeoutMs = timeoutMs;
        this.regressionResponseTimeThresholdPercent = regressionResponseTimeThresholdPercent;
    }

    public Long getId() {
        return id;
    }

    public String getTestName() {
        return testName;
    }

    public Integer getExpectedStatus() {
        return expectedStatus;
    }

    public Long getMaxResponseTimeMs() {
        return maxResponseTimeMs;
    }

    public Long getTimeoutMs() {
        return timeoutMs;
    }

    public Integer getRegressionResponseTimeThresholdPercent() {
        return regressionResponseTimeThresholdPercent;
    }
}
