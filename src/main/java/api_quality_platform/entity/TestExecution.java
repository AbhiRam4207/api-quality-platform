package api_quality_platform.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "test_executions")
public class TestExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long testId;

    private int expectedStatus;

    private Integer actualStatus;

    private boolean passed;

    private LocalDateTime executedAt;

    @Column(columnDefinition = "TEXT")
    private String responseBody;

    private Long actualResponseTimeMs;

    public TestExecution() {
    }

    public TestExecution(
            Long testId,
            int expectedStatus,
            Integer actualStatus,
            boolean passed,
            String responseBody,
            LocalDateTime executedAt,
            Long actualResponseTimeMs) {

        this.testId = testId;
        this.expectedStatus = expectedStatus;
        this.actualStatus = actualStatus;
        this.passed = passed;
        this.responseBody = responseBody;
        this.executedAt = executedAt;
        this.actualResponseTimeMs = actualResponseTimeMs;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTestId() {
        return testId;
    }

    public void setTestId(Long testId) {
        this.testId = testId;
    }

    public int getExpectedStatus() {
        return expectedStatus;
    }

    public void setExpectedStatus(int expectedStatus) {
        this.expectedStatus = expectedStatus;
    }

    public Integer getActualStatus() {
        return actualStatus;
    }

    public void setActualStatus(Integer actualStatus) {
        this.actualStatus = actualStatus;
    }

    public boolean isPassed() {
        return passed;
    }

    public void setPassed(boolean passed) {
        this.passed = passed;
    }

    public LocalDateTime getExecutedAt() {
        return executedAt;
    }

    public void setExecutedAt(LocalDateTime executedAt) {
        this.executedAt = executedAt;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }

    public Long getActualResponseTimeMs() {
        return actualResponseTimeMs;
    }

    public void setActualResponseTimeMs(Long actualResponseTimeMs) {
        this.actualResponseTimeMs = actualResponseTimeMs;
    }
}