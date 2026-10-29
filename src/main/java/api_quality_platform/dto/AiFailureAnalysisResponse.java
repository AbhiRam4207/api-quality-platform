package api_quality_platform.dto;

public class AiFailureAnalysisResponse {

    private Long executionId;
    private Long testId;
    private boolean failureDetected;
    private String summary;
    private String likelyRootCause;
    private String evidence;
    private String recommendedActions;
    private String nextVerificationSteps;

    public AiFailureAnalysisResponse() {
    }

    public AiFailureAnalysisResponse(
            Long executionId,
            Long testId,
            boolean failureDetected,
            String summary,
            String likelyRootCause,
            String evidence,
            String recommendedActions,
            String nextVerificationSteps) {
        this.executionId = executionId;
        this.testId = testId;
        this.failureDetected = failureDetected;
        this.summary = summary;
        this.likelyRootCause = likelyRootCause;
        this.evidence = evidence;
        this.recommendedActions = recommendedActions;
        this.nextVerificationSteps = nextVerificationSteps;
    }

    public Long getExecutionId() {
        return executionId;
    }

    public void setExecutionId(Long executionId) {
        this.executionId = executionId;
    }

    public Long getTestId() {
        return testId;
    }

    public void setTestId(Long testId) {
        this.testId = testId;
    }

    public boolean isFailureDetected() {
        return failureDetected;
    }

    public void setFailureDetected(boolean failureDetected) {
        this.failureDetected = failureDetected;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getLikelyRootCause() {
        return likelyRootCause;
    }

    public void setLikelyRootCause(String likelyRootCause) {
        this.likelyRootCause = likelyRootCause;
    }

    public String getEvidence() {
        return evidence;
    }

    public void setEvidence(String evidence) {
        this.evidence = evidence;
    }

    public String getRecommendedActions() {
        return recommendedActions;
    }

    public void setRecommendedActions(String recommendedActions) {
        this.recommendedActions = recommendedActions;
    }

    public String getNextVerificationSteps() {
        return nextVerificationSteps;
    }

    public void setNextVerificationSteps(String nextVerificationSteps) {
        this.nextVerificationSteps = nextVerificationSteps;
    }
}
