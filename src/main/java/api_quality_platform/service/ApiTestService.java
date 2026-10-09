package api_quality_platform.service;

import api_quality_platform.dto.ApiTestCreateRequest;
import api_quality_platform.dto.ApiTestResponse;
import api_quality_platform.entity.ApiTest;
import api_quality_platform.entity.TestExecution;
import api_quality_platform.exception.NotFoundException;
import api_quality_platform.repository.ApiRepository;
import api_quality_platform.repository.ApiTestRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ApiTestService {

    private final ApiTestRepository apiTestRepository;
    private final ApiRepository apiRepository;
    private final ApiExecutionService apiExecutionService;
    private final TestExecutionService testExecutionService;
    private final ApiResponseValidationService validationService;

    public ApiTestService(
            ApiTestRepository apiTestRepository,
            ApiRepository apiRepository,
            ApiExecutionService apiExecutionService,
            TestExecutionService testExecutionService,
            ApiResponseValidationService validationService) {

        this.apiTestRepository = apiTestRepository;
        this.apiRepository = apiRepository;
        this.apiExecutionService = apiExecutionService;
        this.testExecutionService = testExecutionService;
        this.validationService = validationService;
    }

    public ApiTestResponse createTest(Long apiId, ApiTestCreateRequest request) {
        apiRepository.findById(apiId)
                .orElseThrow(() -> new NotFoundException("Api not found with id: " + apiId));

        ApiTest apiTest = new ApiTest();
        apiTest.setTestName(request.getTestName());
        apiTest.setExpectedStatus(request.getExpectedStatus());
        apiTest.setRequiredFields(request.getRequiredFields());
        apiTest.setExpectedFieldTypes(request.getExpectedFieldTypes());
        apiTest.setRequestBody(request.getRequestBody());
        apiTest.setHeaders(request.getHeaders());
        apiTest.setQueryParams(request.getQueryParams());
        apiTest.setPathParams(request.getPathParams());
        apiTest.setMaxResponseTimeMs(request.getMaxResponseTimeMs());
        apiTest.setTimeoutMs(request.getTimeoutMs());
        apiTest.setRegressionResponseTimeThresholdPercent(request.getRegressionResponseTimeThresholdPercent());
        apiTest.setApi(apiRepository.getReferenceById(apiId));

        ApiTest saved = apiTestRepository.save(apiTest);

        return new ApiTestResponse(
                saved.getId(),
                saved.getTestName(),
                saved.getExpectedStatus(),
                saved.getMaxResponseTimeMs(),
                saved.getTimeoutMs(),
                saved.getRegressionResponseTimeThresholdPercent()
        );
    }

    public List<ApiTestResponse> getTestsByApiId(Long apiId) {
        return apiTestRepository.findByApiId(apiId).stream()
                .map(apiTest -> new ApiTestResponse(
                        apiTest.getId(),
                        apiTest.getTestName(),
                        apiTest.getExpectedStatus(),
                        apiTest.getMaxResponseTimeMs(),
                        apiTest.getTimeoutMs(),
                        apiTest.getRegressionResponseTimeThresholdPercent()
                ))
                .toList();
    }

    public ApiTestResult runTest(ApiTest apiTest) {
        ApiExecutionResult executionResult = apiExecutionService.execute(
                apiTest.getApi().getUrl(),
                apiTest.getApi().getMethod(),
                apiTest.getApi().getAuthType(),
                apiTest.getApi().getUsername(),
                apiTest.getApi().getPassword(),
                apiTest.getApi().getToken(),
                apiTest.getApi().getApiKey(),
                apiTest.getApi().getApiKeyHeader(),
                apiTest.getRequestBody(),
                apiTest.getHeaders(),
                apiTest.getQueryParams(),
                apiTest.getPathParams(),
                apiTest.getTimeoutMs()
        );

        boolean passed;
        int actualStatus;
        String responseBody;
        Long actualResponseTimeMs;
        boolean executionError = executionResult.isError();
        String executionErrorMessage = executionResult.getErrorMessage();
        String executionErrorType = executionResult.getErrorType();

        if (executionError) {
            passed = false;
            actualStatus = -1;
            responseBody = null;
            actualResponseTimeMs = executionResult.getActualResponseTimeMs() >= 0 ? executionResult.getActualResponseTimeMs() : null;
        } else {
            passed = apiTest.getExpectedStatus().equals(executionResult.getStatusCode());
            actualStatus = executionResult.getStatusCode();
            responseBody = executionResult.getResponseBody();
            actualResponseTimeMs = executionResult.getActualResponseTimeMs() >= 0 ? executionResult.getActualResponseTimeMs() : null;
        }

        TestExecution execution = new TestExecution(
                apiTest.getId(),
                apiTest.getExpectedStatus(),
                executionError ? null : actualStatus,
                passed,
                responseBody,
                LocalDateTime.now(),
                actualResponseTimeMs
        );

        testExecutionService.save(execution);

        ValidationResult validationResult = validationService.validate(
                apiTest.getExpectedStatus(),
                actualStatus,
                responseBody != null ? responseBody : "",
                apiTest.getRequiredFields(),
                apiTest.getExpectedFieldTypes()
        );

        ValidationResult responseTimeResult = validationService.validateResponseTime(
                apiTest.getMaxResponseTimeMs(),
                actualResponseTimeMs != null ? actualResponseTimeMs : -1
        );

        boolean overallValid;
        List<String> allErrors;

        if (executionError) {
            overallValid = false;
            allErrors = new ArrayList<>();
            allErrors.add(executionErrorMessage != null ? executionErrorMessage : "Execution failed");
        } else {
            overallValid = validationResult.isValid() && responseTimeResult.isValid();
            allErrors = new ArrayList<>(validationResult.getErrors());
            allErrors.addAll(responseTimeResult.getErrors());
        }

        return new ApiTestResult(
                apiTest.getTestName(),
                apiTest.getExpectedStatus(),
                actualStatus,
                passed,
                overallValid,
                allErrors,
                actualResponseTimeMs,
                executionError,
                executionErrorMessage,
                executionErrorType
        );
    }

    public ApiTestResult runTestById(Long testId) {
        ApiTest apiTest = apiTestRepository.findById(testId)
                .orElseThrow(() -> new NotFoundException("Test not found with id: " + testId));
        return runTest(apiTest);
    }
}
