package api_quality_platform.dto;

import java.util.ArrayList;
import java.util.List;

public class RegressionResponse {

    private final Long testId;
    private final Long currentExecutionId;
    private final Long previousExecutionId;
    private final Integer previousStatus;
    private final Integer currentStatus;
    private final boolean regression;
    private final List<String> regressionReasons;

    public RegressionResponse(
            Long testId,
            Long currentExecutionId,
            Long previousExecutionId,
            Integer previousStatus,
            Integer currentStatus,
            boolean regression) {
        this(testId, currentExecutionId, previousExecutionId, previousStatus, currentStatus, regression, new ArrayList<>());
    }

    public RegressionResponse(
            Long testId,
            Long currentExecutionId,
            Long previousExecutionId,
            Integer previousStatus,
            Integer currentStatus,
            boolean regression,
            List<String> regressionReasons) {

        this.testId = testId;
        this.currentExecutionId = currentExecutionId;
        this.previousExecutionId = previousExecutionId;
        this.previousStatus = previousStatus;
        this.currentStatus = currentStatus;
        this.regression = regression;
        this.regressionReasons = regressionReasons;
    }

    public Long getTestId() {
        return testId;
    }

    public Long getCurrentExecutionId() {
        return currentExecutionId;
    }

    public Long getPreviousExecutionId() {
        return previousExecutionId;
    }

    public Integer getPreviousStatus() {
        return previousStatus;
    }

    public Integer getCurrentStatus() {
        return currentStatus;
    }

    public boolean isRegression() {
        return regression;
    }

    public List<String> getRegressionReasons() {
        return regressionReasons;
    }
}