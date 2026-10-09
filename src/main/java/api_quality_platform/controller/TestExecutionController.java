package api_quality_platform.controller;

import api_quality_platform.entity.TestExecution;
import api_quality_platform.service.TestExecutionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/test-executions")
public class TestExecutionController {

    private final TestExecutionService testExecutionService;

    public TestExecutionController(TestExecutionService testExecutionService) {
        this.testExecutionService = testExecutionService;
    }

    @GetMapping("/test/{testId}")
    public List<TestExecution> getExecutionsByTestId(@PathVariable Long testId) {
        return testExecutionService.getExecutionsByTestId(testId);
    }
}
