package api_quality_platform.controller;

import api_quality_platform.dto.ApiTestCreateRequest;
import api_quality_platform.dto.ApiTestResponse;
import api_quality_platform.service.ApiExecutionResult;
import api_quality_platform.service.ApiExecutionService;
import api_quality_platform.service.ApiTestResult;
import api_quality_platform.service.ApiTestService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/apis")
public class ApiTestController {

    private final ApiTestService apiTestService;
    private final ApiExecutionService apiExecutionService;
    private final String executionUsername;
    private final String executionPassword;

    public ApiTestController(
            ApiTestService apiTestService,
            ApiExecutionService apiExecutionService,
            @Value("${api.execution.username:}") String executionUsername,
            @Value("${api.execution.password:}") String executionPassword) {

        this.apiTestService = apiTestService;
        this.apiExecutionService = apiExecutionService;
        this.executionUsername = executionUsername;
        this.executionPassword = executionPassword;
    }

    @PostMapping("/{apiId}/tests")
    public ApiTestResponse createTest(
            @PathVariable Long apiId,
            @RequestBody ApiTestCreateRequest request) {

        return apiTestService.createTest(apiId, request);
    }

    @GetMapping("/{apiId}/tests")
    public List<ApiTestResponse> getTestsByApiId(
            @PathVariable Long apiId) {
        return apiTestService.getTestsByApiId(apiId);
    }

    @GetMapping("/execute")
    public ApiExecutionResult executeGet(@RequestParam String url) {
        // Credentials come from configuration (environment variables /
        // api.execution.username / api.execution.password). When unset, blank
        // values are passed and ApiExecutionService sends no Authorization
        // header. No credential is hardcoded or returned to the caller.
        return apiExecutionService.executeGet(
                url,
                executionUsername,
                executionPassword
        );
    }

    @PostMapping("/tests/{testId}/execute")
    public ApiTestResult executeTest(@PathVariable Long testId) {
        return apiTestService.runTestById(testId);
    }
}
