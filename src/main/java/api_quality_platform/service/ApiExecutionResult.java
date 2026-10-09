package api_quality_platform.service;

public class ApiExecutionResult {

    private final int statusCode;
    private final String responseBody;
    private final long actualResponseTimeMs;
    private final boolean error;
    private final String errorMessage;
    private final String errorType;

    public ApiExecutionResult(int statusCode, String responseBody) {
        this(statusCode, responseBody, -1);
    }

    public ApiExecutionResult(int statusCode, String responseBody, long actualResponseTimeMs) {
        this(statusCode, responseBody, actualResponseTimeMs, false, null, null);
    }

    public ApiExecutionResult(String errorMessage, String errorType, long actualResponseTimeMs) {
        this(-1, null, actualResponseTimeMs, true, errorMessage, errorType);
    }

    public ApiExecutionResult(int statusCode, String responseBody, long actualResponseTimeMs, boolean error, String errorMessage, String errorType) {
        this.statusCode = statusCode;
        this.responseBody = responseBody;
        this.actualResponseTimeMs = actualResponseTimeMs;
        this.error = error;
        this.errorMessage = errorMessage;
        this.errorType = errorType;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public long getActualResponseTimeMs() {
        return actualResponseTimeMs;
    }

    public boolean isError() {
        return error;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getErrorType() {
        return errorType;
    }
}
