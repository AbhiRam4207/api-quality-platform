package api_quality_platform.controller;

import api_quality_platform.dto.AiFailureAnalysisResponse;
import api_quality_platform.entity.TestExecution;
import api_quality_platform.service.AiFailureAnalysisService;
import api_quality_platform.service.TestExecutionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/test-executions")
public class TestExecutionController {

    private final TestExecutionService testExecutionService;
    private final AiFailureAnalysisService aiFailureAnalysisService;

    public TestExecutionController(
            TestExecutionService testExecutionService,
            AiFailureAnalysisService aiFailureAnalysisService) {
        this.testExecutionService = testExecutionService;
        this.aiFailureAnalysisService = aiFailureAnalysisService;
    }

    @GetMapping("/test/{testId}")
    public List<TestExecution> getExecutionsByTestId(@PathVariable Long testId) {
        return testExecutionService.getExecutionsByTestId(testId);
    }

    @PostMapping("/{executionId}/ai-analysis")
    public AiFailureAnalysisResponse analyzeExecutionFailure(@PathVariable Long executionId) {
        return aiFailureAnalysisService.analyzeFailure(executionId);
    }
}
