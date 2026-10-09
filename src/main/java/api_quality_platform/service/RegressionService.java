package api_quality_platform.service;

import api_quality_platform.dto.RegressionResponse;
import api_quality_platform.entity.ApiTest;
import api_quality_platform.entity.TestExecution;
import api_quality_platform.exception.NotFoundException;
import api_quality_platform.repository.ApiTestRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class RegressionService {

    private final TestExecutionService testExecutionService;
    private final ApiTestRepository apiTestRepository;

    public RegressionService(TestExecutionService testExecutionService, ApiTestRepository apiTestRepository) {
        this.testExecutionService = testExecutionService;
        this.apiTestRepository = apiTestRepository;
    }

    public RegressionResponse checkRegression(
            Long testId,
            Long currentExecutionId) {

        TestExecution currentExecution =
                testExecutionService
                        .getExecutionsByTestId(testId)
                        .stream()
                        .filter(execution ->
                                execution.getId().equals(currentExecutionId))
                        .findFirst()
                        .orElseThrow(() ->
                                new NotFoundException("Execution not found with id: " + currentExecutionId));

        TestExecution previousExecution =
                testExecutionService.getPreviousExecution(
                        testId,
                        currentExecutionId
                );

        if (previousExecution == null) {
            return new RegressionResponse(
                    testId,
                    currentExecutionId,
                    null,
                    null,
                    currentExecution.getActualStatus(),
                    false,
                    new ArrayList<>()
            );
        }

        List<String> reasons = new ArrayList<>();

        if (!Objects.equals(previousExecution.getActualStatus(), currentExecution.getActualStatus())) {
            reasons.add("Status regression: previous was " + previousExecution.getActualStatus() + ", current is " + currentExecution.getActualStatus());
        }

        if (currentExecution.getActualStatus() == null && previousExecution.getActualStatus() != null) {
            reasons.add("Execution error regression: current execution failed but previous succeeded");
        }

        ApiTest apiTest = apiTestRepository.findById(testId)
                .orElseThrow(() -> new NotFoundException("Test not found with id: " + testId));

        Integer thresholdPercent = apiTest.getRegressionResponseTimeThresholdPercent();
        if (thresholdPercent == null) {
            thresholdPercent = 50;
        }

        Long previousResponseTime = previousExecution.getActualResponseTimeMs();
        Long currentResponseTime = currentExecution.getActualResponseTimeMs();

        if (previousResponseTime != null && currentResponseTime != null && previousResponseTime > 0) {
            double maxAllowed = previousResponseTime * (1 + thresholdPercent / 100.0);
            if (currentResponseTime > maxAllowed) {
                reasons.add("Response time regression: " + currentResponseTime + "ms exceeds " + thresholdPercent + "% threshold from previous " + previousResponseTime + "ms");
            }
        }

        boolean regression = !reasons.isEmpty();

        return new RegressionResponse(
                testId,
                currentExecutionId,
                previousExecution.getId(),
                previousExecution.getActualStatus(),
                currentExecution.getActualStatus(),
                regression,
                reasons
        );
    }
}