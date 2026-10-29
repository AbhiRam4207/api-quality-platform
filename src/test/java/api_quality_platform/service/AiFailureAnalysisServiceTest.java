package api_quality_platform.service;

import api_quality_platform.dto.AiFailureAnalysisResponse;
import api_quality_platform.entity.Api;
import api_quality_platform.entity.ApiTest;
import api_quality_platform.entity.TestExecution;
import api_quality_platform.exception.AiConfigurationException;
import api_quality_platform.exception.AiProviderException;
import api_quality_platform.exception.NotFoundException;
import api_quality_platform.repository.ApiTestRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class AiFailureAnalysisServiceTest {

    private static final String OPENROUTER_URL = "https://openrouter.ai/api/v1/chat/completions";
    private static final String TEST_KEY = "test-openrouter-key";
    private static final String MODEL = "cohere/north-mini-code:free";

    private TestExecutionService testExecutionService;
    private ApiTestRepository apiTestRepository;
    private ObjectMapper objectMapper;
    private ChangelogService changelogService;

    @BeforeEach
    void setUp() {
        testExecutionService = Mockito.mock(TestExecutionService.class);
        apiTestRepository = Mockito.mock(ApiTestRepository.class);
        objectMapper = new ObjectMapper();
        changelogService = Mockito.mock(ChangelogService.class);
    }

    private AiFailureAnalysisService service(RestClient.Builder builder, String key) {
        return new AiFailureAnalysisService(testExecutionService, apiTestRepository, builder, objectMapper, changelogService, key, MODEL);
    }

    @Test
    void shouldThrowNotFoundWhenExecutionDoesNotExist() {
        when(testExecutionService.getExecutionById(999L))
                .thenThrow(new NotFoundException("Execution not found with id: 999"));

        AiFailureAnalysisService svc = service(RestClient.builder(), TEST_KEY);

        assertThrows(NotFoundException.class, () -> svc.analyzeFailure(999L));
    }

    @Test
    void shouldThrowAiConfigurationExceptionWhenKeyMissing() {
        TestExecution execution = new TestExecution(10L, 200, 500, false, "Error", LocalDateTime.now(), 1500L);
        execution.setId(1L);

        when(testExecutionService.getExecutionById(1L)).thenReturn(execution);

        AiFailureAnalysisService svc = service(RestClient.builder(), "");

        assertThrows(AiConfigurationException.class, () -> svc.analyzeFailure(1L));
    }

    @Test
    void shouldSuccessfullyAnalyzeFailure() throws Exception {
        TestExecution execution = new TestExecution(10L, 200, 500, false, "{\"error\": \"Internal Server Error\"}", LocalDateTime.now(), 1500L);
        execution.setId(1L);

        ApiTest apiTest = new ApiTest();
        apiTest.setId(10L);
        apiTest.setTestName("Product Get Test");

        Api api = new Api();
        api.setName("ShopAPI");
        api.setUrl("http://localhost:8081/api/products");
        api.setMethod("GET");
        api.setAuthType("NONE");
        apiTest.setApi(api);

        when(testExecutionService.getExecutionById(1L)).thenReturn(execution);
        when(apiTestRepository.findById(10L)).thenReturn(Optional.of(apiTest));
        when(testExecutionService.getPreviousExecution(10L, 1L)).thenReturn(null);

        AiFailureAnalysisResponse expectedAnalysis = new AiFailureAnalysisResponse(
                1L, 10L, true,
                "HTTP 500 received instead of 200",
                "[HYPOTHESIS] Internal Server Error on backend service",
                "Actual status 500 vs expected 200",
                "Check backend server logs",
                "Restart service and re-run test"
        );

        String innerJson = objectMapper.writeValueAsString(expectedAnalysis);
        String responseBody = buildOpenRouterResponse(innerJson);

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(OPENROUTER_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer " + TEST_KEY))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        AiFailureAnalysisService svc = service(builder, TEST_KEY);
        AiFailureAnalysisResponse result = svc.analyzeFailure(1L);

        assertNotNull(result);
        assertEquals(1L, result.getExecutionId());
        assertTrue(result.isFailureDetected());
        assertEquals("HTTP 500 received instead of 200", result.getSummary());
        server.verify();
    }

    @Test
    void shouldHandle401Unauthorized() {
        TestExecution execution = new TestExecution(10L, 200, 500, false, "Error", LocalDateTime.now(), 1500L);
        execution.setId(1L);

        when(testExecutionService.getExecutionById(1L)).thenReturn(execution);
        when(apiTestRepository.findById(10L)).thenReturn(Optional.empty());

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(OPENROUTER_URL))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThrows(AiConfigurationException.class, () -> service(builder, TEST_KEY).analyzeFailure(1L));
        server.verify();
    }

    @Test
    void shouldHandleServerError() {
        TestExecution execution = new TestExecution(10L, 200, 500, false, "Error", LocalDateTime.now(), 1500L);
        execution.setId(1L);

        when(testExecutionService.getExecutionById(1L)).thenReturn(execution);
        when(apiTestRepository.findById(10L)).thenReturn(Optional.empty());

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(OPENROUTER_URL))
                .andRespond(withServerError());

        assertThrows(AiProviderException.class, () -> service(builder, TEST_KEY).analyzeFailure(1L));
        server.verify();
    }

    private String buildOpenRouterResponse(String content) throws Exception {
        Map<String, Object> responseObj = Map.of(
                "choices", List.of(
                        Map.of("message", Map.of("role", "assistant", "content", content))
                )
        );
        return new ObjectMapper().writeValueAsString(responseObj);
    }
}
