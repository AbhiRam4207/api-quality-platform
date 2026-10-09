package api_quality_platform.service;

import java.util.List;

public class ApiTestResult {

    private final String testName;
    private final int expectedStatus;
    private final int actualStatus;
    private final boolean passed;
    private final boolean valid;
    private final List<String> validationErrors;
    private final Long actualResponseTimeMs;
    private final boolean executionError;
    private final String executionErrorMessage;
    private final String executionErrorType;

    public ApiTestResult(String testName, int expectedStatus, int actualStatus, boolean passed, boolean valid, List<String> validationErrors) {
        this(testName, expectedStatus, actualStatus, passed, valid, validationErrors, null);
    }

    public ApiTestResult(String testName, int expectedStatus, int actualStatus, boolean passed, boolean valid, List<String> validationErrors, Long actualResponseTimeMs) {
        this(testName, expectedStatus, actualStatus, passed, valid, validationErrors, actualResponseTimeMs, false, null, null);
    }

    public ApiTestResult(String testName, int expectedStatus, int actualStatus, boolean passed, boolean valid, List<String> validationErrors, Long actualResponseTimeMs, boolean executionError, String executionErrorMessage, String executionErrorType) {
        this.testName = testName;
        this.expectedStatus = expectedStatus;
        this.actualStatus = actualStatus;
        this.passed = passed;
        this.valid = valid;
        this.validationErrors = validationErrors;
        this.actualResponseTimeMs = actualResponseTimeMs;
        this.executionError = executionError;
        this.executionErrorMessage = executionErrorMessage;
        this.executionErrorType = executionErrorType;
    }

    public String getTestName() {
        return testName;
    }

    public int getExpectedStatus() {
        return expectedStatus;
    }

    public int getActualStatus() {
        return actualStatus;
    }

    public boolean isPassed() {
        return passed;
    }

    public boolean isValid() {
        return valid;
    }

    public List<String> getValidationErrors() {
        return validationErrors;
    }

    public Long getActualResponseTimeMs() {
        return actualResponseTimeMs;
    }

    public boolean isExecutionError() {
        return executionError;
    }

    public String getExecutionErrorMessage() {
        return executionErrorMessage;
    }

    public String getExecutionErrorType() {
        return executionErrorType;
    }
}
