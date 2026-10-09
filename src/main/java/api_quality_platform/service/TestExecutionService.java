package api_quality_platform.service;

import api_quality_platform.entity.ApiTest;
import api_quality_platform.entity.TestExecution;
import api_quality_platform.repository.TestExecutionRepository;
import api_quality_platform.exception.NotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TestExecutionService {

    private final TestExecutionRepository testExecutionRepository;

    public TestExecutionService(TestExecutionRepository testExecutionRepository) {
        this.testExecutionRepository = testExecutionRepository;
    }

    public TestExecution save(TestExecution execution) {
        return testExecutionRepository.save(execution);
    }

    public List<TestExecution> getExecutionsByTestId(Long testId) {
        return testExecutionRepository
                .findByTestIdOrderByExecutedAtDesc(testId);
    }

    public TestExecution getPreviousExecution(Long testId, Long executionId) {
        return testExecutionRepository
                .findFirstByTestIdAndIdLessThanOrderByIdDesc(
                        testId,
                        executionId
                );
    }
    public TestExecution getExecutionById(Long executionId) {
        return testExecutionRepository.findById(executionId)
                .orElseThrow(() ->
                        new NotFoundException("Execution not found with id: " + executionId));
    }
}
