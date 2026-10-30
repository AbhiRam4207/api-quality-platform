package api_quality_platform.service;

import api_quality_platform.dto.AiSuggestionRequest;
import api_quality_platform.dto.ApiTestSuggestionResponse;
import api_quality_platform.entity.Api;
import api_quality_platform.exception.AiConfigurationException;
import api_quality_platform.exception.AiProviderException;
import api_quality_platform.exception.NotFoundException;
import api_quality_platform.repository.ApiRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class AiTestSuggestionServiceTest {

    private static final String NVIDIA_URL = "https://integrate.api.nvidia.com/v1/chat/completions";
    private static final String TEST_KEY = "test-nvidia-key";
    private static final String MODEL = "deepseek-ai/deepseek-v4.1-flash";

    private ApiRepository apiRepository;
    private ObjectMapper objectMapper;
    private ChangelogService changelogService;

    @BeforeEach
    void setUp() {
        apiRepository = Mockito.mock(ApiRepository.class);
        objectMapper = new ObjectMapper();
        changelogService = Mockito.mock(ChangelogService.class);
    }

    private AiTestSuggestionService service(RestClient.Builder builder, String key) {
        return new AiTestSuggestionService(apiRepository, builder, objectMapper, changelogService, "https://integrate.api.nvidia.com/v1", key, MODEL);
    }

    @Test
    void shouldThrowNotFoundWhenApiDoesNotExist() {
        when(apiRepository.findById(999L)).thenReturn(Optional.empty());

        AiTestSuggestionService svc = service(RestClient.builder(), TEST_KEY);

        assertThrows(NotFoundException.class, () -> svc.generateAiSuggestions(999L, new AiSuggestionRequest()));
    }

    @Test
    void shouldThrowAiConfigurationExceptionWhenKeyMissing() {
        Api api = new Api();
        api.setId(1L);
        api.setName("Test API");
        when(apiRepository.findById(1L)).thenReturn(Optional.of(api));

        AiTestSuggestionService svc = service(RestClient.builder(), "");

        assertThrows(AiConfigurationException.class, () -> svc.generateAiSuggestions(1L, new AiSuggestionRequest()));
    }

    @Test
    void shouldSendCorrectModelAndBearerHeader() throws Exception {
        Api api = buildApi();
        when(apiRepository.findById(1L)).thenReturn(Optional.of(api));

        ApiTestSuggestionResponse expected = buildExpectedSuggestionResponse();
        String responseJson = buildNvidiaResponse(objectMapper.writeValueAsString(expected));

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(NVIDIA_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer " + TEST_KEY))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        AiTestSuggestionService svc = service(builder, TEST_KEY);
        AiSuggestionRequest req = new AiSuggestionRequest();
        req.setSampleResponse("[{\"id\":1}]");

        ApiTestSuggestionResponse result = svc.generateAiSuggestions(1L, req);

        assertNotNull(result);
        assertEquals(1L, result.getApiId());
        assertEquals("ShopAPI Products", result.getApiName());
        assertEquals(1, result.getSuggestions().size());
        assertEquals("Success Test", result.getSuggestions().get(0).getTestName());
        server.verify();
    }

    @Test
    void shouldParseNvidiaChoicesStructure() throws Exception {
        Api api = buildApi();
        when(apiRepository.findById(1L)).thenReturn(Optional.of(api));

        ApiTestSuggestionResponse expected = buildExpectedSuggestionResponse();
        String responseBody = buildNvidiaResponse(objectMapper.writeValueAsString(expected));

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(NVIDIA_URL))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        ApiTestSuggestionResponse result = service(builder, TEST_KEY).generateAiSuggestions(1L, null);

        assertEquals("ShopAPI Products", result.getApiName());
        server.verify();
    }

    @Test
    void shouldHandleMalformedResponseContent() throws Exception {
        Api api = buildApi();
        when(apiRepository.findById(1L)).thenReturn(Optional.of(api));

        String malformed = buildNvidiaResponse("not-valid-json");

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(NVIDIA_URL))
                .andRespond(withSuccess(malformed, MediaType.APPLICATION_JSON));

        assertThrows(AiProviderException.class, () -> service(builder, TEST_KEY).generateAiSuggestions(1L, null));
        server.verify();
    }

    @Test
    void shouldHandleEmptyChoicesContent() throws Exception {
        Api api = buildApi();
        when(apiRepository.findById(1L)).thenReturn(Optional.of(api));

        String emptyChoices = "{\"choices\":[{\"message\":{\"content\":\"\"}}]}";

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(NVIDIA_URL))
                .andRespond(withSuccess(emptyChoices, MediaType.APPLICATION_JSON));

        assertThrows(AiProviderException.class, () -> service(builder, TEST_KEY).generateAiSuggestions(1L, null));
        server.verify();
    }

    @Test
    void shouldHandle401Unauthorized() throws Exception {
        Api api = buildApi();
        when(apiRepository.findById(1L)).thenReturn(Optional.of(api));

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(NVIDIA_URL))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThrows(AiConfigurationException.class, () -> service(builder, TEST_KEY).generateAiSuggestions(1L, null));
        server.verify();
    }

    @Test
    void shouldHandle403Forbidden() throws Exception {
        Api api = buildApi();
        when(apiRepository.findById(1L)).thenReturn(Optional.of(api));

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(NVIDIA_URL))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));

        assertThrows(AiConfigurationException.class, () -> service(builder, TEST_KEY).generateAiSuggestions(1L, null));
        server.verify();
    }

    @Test
    void shouldHandle429RateLimit() throws Exception {
        Api api = buildApi();
        when(apiRepository.findById(1L)).thenReturn(Optional.of(api));

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(NVIDIA_URL))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThrows(AiProviderException.class, () -> service(builder, TEST_KEY).generateAiSuggestions(1L, null));
        server.verify();
    }

    @Test
    void shouldHandle5xxServerError() throws Exception {
        Api api = buildApi();
        when(apiRepository.findById(1L)).thenReturn(Optional.of(api));

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(NVIDIA_URL))
                .andRespond(withServerError());

        assertThrows(AiProviderException.class, () -> service(builder, TEST_KEY).generateAiSuggestions(1L, null));
        server.verify();
    }

    @Test
    void shouldStripMarkdownFencesFromContent() throws Exception {
        Api api = buildApi();
        when(apiRepository.findById(1L)).thenReturn(Optional.of(api));

        ApiTestSuggestionResponse expected = buildExpectedSuggestionResponse();
        String innerJson = objectMapper.writeValueAsString(expected);
        String fencedContent = "```json\n" + innerJson + "\n```";
        String responseBody = buildNvidiaResponse(fencedContent);

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(NVIDIA_URL))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        ApiTestSuggestionResponse result = service(builder, TEST_KEY).generateAiSuggestions(1L, null);
        assertEquals("ShopAPI Products", result.getApiName());
        server.verify();
    }

    @Test
    void shouldRejectCsvExpectedFieldTypes() throws Exception {
        Api api = buildApi();
        when(apiRepository.findById(1L)).thenReturn(Optional.of(api));

        api_quality_platform.dto.ApiTestCreateRequest req = new api_quality_platform.dto.ApiTestCreateRequest();
        req.setExpectedStatus(200);
        req.setExpectedFieldTypes("id:int,name:STRING");
        ApiTestSuggestionResponse resp = new ApiTestSuggestionResponse(1L, "ShopAPI Products", List.of(
                new ApiTestSuggestionResponse.SuggestionItem("CSV Test", "desc", req)
        ));
        String responseBody = buildNvidiaResponse(objectMapper.writeValueAsString(resp));

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(NVIDIA_URL))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        AiTestSuggestionService svc = service(builder, TEST_KEY);
        assertThrows(AiProviderException.class, () -> svc.generateAiSuggestions(1L, null));
        server.verify();
    }

    @Test
    void shouldRejectUnsupportedShopApiField() throws Exception {
        Api api = buildApi();
        when(apiRepository.findById(1L)).thenReturn(Optional.of(api));

        api_quality_platform.dto.ApiTestCreateRequest req = new api_quality_platform.dto.ApiTestCreateRequest();
        req.setExpectedStatus(200);
        req.setRequiredFields("id,description");
        ApiTestSuggestionResponse resp = new ApiTestSuggestionResponse(1L, "ShopAPI Products", List.of(
                new ApiTestSuggestionResponse.SuggestionItem("Field Test", "desc", req)
        ));
        String responseBody = buildNvidiaResponse(objectMapper.writeValueAsString(resp));

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(NVIDIA_URL))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        AiTestSuggestionService svc = service(builder, TEST_KEY);
        assertThrows(AiProviderException.class, () -> svc.generateAiSuggestions(1L, null));
        server.verify();
    }

    @Test
    void shouldRejectNullAuthConfiguration() throws Exception {
        Api api = buildApi();
        when(apiRepository.findById(1L)).thenReturn(Optional.of(api));

        api_quality_platform.dto.ApiTestCreateRequest req = new api_quality_platform.dto.ApiTestCreateRequest();
        req.setExpectedStatus(200);
        req.setAuthType(null);
        ApiTestSuggestionResponse resp = new ApiTestSuggestionResponse(1L, "ShopAPI Products", List.of(
                new ApiTestSuggestionResponse.SuggestionItem("Invalid credentials test", "tests credentials with null authType", req)
        ));
        String responseBody = buildNvidiaResponse(objectMapper.writeValueAsString(resp));

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(NVIDIA_URL))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        AiTestSuggestionService svc = service(builder, TEST_KEY);
        assertThrows(AiProviderException.class, () -> svc.generateAiSuggestions(1L, null));
        server.verify();
    }

    @Test
    void shouldRejectQueryStringInRequestBody() throws Exception {
        Api api = buildApi();
        when(apiRepository.findById(1L)).thenReturn(Optional.of(api));

        api_quality_platform.dto.ApiTestCreateRequest req = new api_quality_platform.dto.ApiTestCreateRequest();
        req.setExpectedStatus(200);
        req.setRequestBody("search=query&filter=active");
        ApiTestSuggestionResponse resp = new ApiTestSuggestionResponse(1L, "ShopAPI Products", List.of(
                new ApiTestSuggestionResponse.SuggestionItem("Query Test", "desc", req)
        ));
        String responseBody = buildNvidiaResponse(objectMapper.writeValueAsString(resp));

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(NVIDIA_URL))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        AiTestSuggestionService svc = service(builder, TEST_KEY);
        assertThrows(AiProviderException.class, () -> svc.generateAiSuggestions(1L, null));
        server.verify();
    }

    @Test
    void shouldRejectUnverifiedStatusForShopApi() throws Exception {
        Api api = buildApi();
        when(apiRepository.findById(1L)).thenReturn(Optional.of(api));

        api_quality_platform.dto.ApiTestCreateRequest req = new api_quality_platform.dto.ApiTestCreateRequest();
        req.setExpectedStatus(404);
        ApiTestSuggestionResponse resp = new ApiTestSuggestionResponse(1L, "ShopAPI Products", List.of(
                new ApiTestSuggestionResponse.SuggestionItem("NotFound Test", "desc", req)
        ));
        String responseBody = buildNvidiaResponse(objectMapper.writeValueAsString(resp));

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(NVIDIA_URL))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        AiTestSuggestionService svc = service(builder, TEST_KEY);
        assertThrows(AiProviderException.class, () -> svc.generateAiSuggestions(1L, null));
        server.verify();
    }

    @Test
    void shouldRejectLiteralNullRequestBodyForGet() throws Exception {
        Api api = buildApi();
        when(apiRepository.findById(1L)).thenReturn(Optional.of(api));

        api_quality_platform.dto.ApiTestCreateRequest req = new api_quality_platform.dto.ApiTestCreateRequest();
        req.setExpectedStatus(200);
        req.setRequestBody("null");
        ApiTestSuggestionResponse resp = new ApiTestSuggestionResponse(1L, "ShopAPI Products", List.of(
                new ApiTestSuggestionResponse.SuggestionItem("Null Body Test", "desc", req)
        ));
        String responseBody = buildNvidiaResponse(objectMapper.writeValueAsString(resp));

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(NVIDIA_URL))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        AiTestSuggestionService svc = service(builder, TEST_KEY);
        assertThrows(AiProviderException.class, () -> svc.generateAiSuggestions(1L, null));
        server.verify();
    }

    @Test
    void shouldRejectWhitespaceLiteralNullRequestBodyForGet() throws Exception {
        Api api = buildApi();
        when(apiRepository.findById(1L)).thenReturn(Optional.of(api));

        api_quality_platform.dto.ApiTestCreateRequest req = new api_quality_platform.dto.ApiTestCreateRequest();
        req.setExpectedStatus(200);
        req.setRequestBody("  null  ");
        ApiTestSuggestionResponse resp = new ApiTestSuggestionResponse(1L, "ShopAPI Products", List.of(
                new ApiTestSuggestionResponse.SuggestionItem("Whitespace Null Body Test", "desc", req)
        ));
        String responseBody = buildNvidiaResponse(objectMapper.writeValueAsString(resp));

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(NVIDIA_URL))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        AiTestSuggestionService svc = service(builder, TEST_KEY);
        assertThrows(AiProviderException.class, () -> svc.generateAiSuggestions(1L, null));
        server.verify();
    }

    @Test
    void shouldAcceptEmptyRequestBodyForGet() throws Exception {
        Api api = buildApi();
        when(apiRepository.findById(1L)).thenReturn(Optional.of(api));

        api_quality_platform.dto.ApiTestCreateRequest req = new api_quality_platform.dto.ApiTestCreateRequest();
        req.setExpectedStatus(200);
        req.setRequestBody(null);
        ApiTestSuggestionResponse resp = new ApiTestSuggestionResponse(1L, "ShopAPI Products", List.of(
                new ApiTestSuggestionResponse.SuggestionItem("Empty Body Test", "desc", req)
        ));
        String responseBody = buildNvidiaResponse(objectMapper.writeValueAsString(resp));

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(NVIDIA_URL))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        AiTestSuggestionService svc = service(builder, TEST_KEY);
        ApiTestSuggestionResponse result = svc.generateAiSuggestions(1L, null);
        assertNotNull(result);
        server.verify();
    }

    @Test
    void shouldAcceptNonGetRequestBody() throws Exception {
        Api api = buildApi();
        api.setMethod("POST");
        when(apiRepository.findById(1L)).thenReturn(Optional.of(api));

        api_quality_platform.dto.ApiTestCreateRequest req = new api_quality_platform.dto.ApiTestCreateRequest();
        req.setExpectedStatus(201);
        req.setRequestBody("{\"name\":\"test\"}");
        ApiTestSuggestionResponse resp = new ApiTestSuggestionResponse(1L, "ShopAPI Products", List.of(
                new ApiTestSuggestionResponse.SuggestionItem("POST Body Test", "desc", req)
        ));
        String responseBody = buildNvidiaResponse(objectMapper.writeValueAsString(resp));

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(NVIDIA_URL))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        AiTestSuggestionService svc = service(builder, TEST_KEY);
        ApiTestSuggestionResponse result = svc.generateAiSuggestions(1L, null);
        assertNotNull(result);
        server.verify();
    }

    @Test
    void shouldRejectInvalidSuggestionResponse() throws Exception {
        Api api = buildApi();
        when(apiRepository.findById(1L)).thenReturn(Optional.of(api));

        ApiTestSuggestionResponse invalidResp = new ApiTestSuggestionResponse(null, "Test", List.of());
        String responseBody = buildNvidiaResponse(objectMapper.writeValueAsString(invalidResp));

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(NVIDIA_URL))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        AiTestSuggestionService svc = service(builder, TEST_KEY);
        assertThrows(api_quality_platform.exception.AiProviderException.class, () -> svc.generateAiSuggestions(1L, null));
        server.verify();
    }

    @Test
    void shouldUseConfiguredBaseUrlAndModel() throws Exception {
        Api api = buildApi();
        when(apiRepository.findById(1L)).thenReturn(Optional.of(api));

        ApiTestSuggestionResponse expected = buildExpectedSuggestionResponse();
        String responseJson = buildNvidiaResponse(objectMapper.writeValueAsString(expected));

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(NVIDIA_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer " + TEST_KEY))
                .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

        AiTestSuggestionService svc = new AiTestSuggestionService(
                apiRepository, builder, objectMapper, changelogService,
                "https://integrate.api.nvidia.com/v1", TEST_KEY, MODEL
        );

        ApiTestSuggestionResponse result = svc.generateAiSuggestions(1L, new AiSuggestionRequest());

        assertNotNull(result);
        assertEquals(1L, result.getApiId());
        assertEquals("https://integrate.api.nvidia.com/v1", svc.getBaseUrl());
        assertEquals(MODEL, svc.getModel());
        server.verify();
    }

    @Test
    void shouldThrowAiConfigurationExceptionWhenNvidiaKeyMissing() {
        Api api = buildApi();
        when(apiRepository.findById(1L)).thenReturn(Optional.of(api));

        AiTestSuggestionService svc = new AiTestSuggestionService(
                apiRepository, RestClient.builder(), objectMapper, changelogService,
                "https://integrate.api.nvidia.com/v1", "", MODEL
        );

        assertThrows(AiConfigurationException.class, () -> svc.generateAiSuggestions(1L, null));
    }

    private Api buildApi() {
        Api api = new Api();
        api.setId(1L);
        api.setName("ShopAPI Products");
        api.setUrl("http://localhost:8081/api/products");
        api.setMethod("GET");
        api.setAuthType("NONE");
        return api;
    }

    private ApiTestSuggestionResponse buildExpectedSuggestionResponse() {
        api_quality_platform.dto.ApiTestCreateRequest req = new api_quality_platform.dto.ApiTestCreateRequest();
        req.setExpectedStatus(200);
        return new ApiTestSuggestionResponse(
                1L,
                "ShopAPI Products",
                List.of(new ApiTestSuggestionResponse.SuggestionItem("Success Test", "Tests 200 response", req))
        );
    }

    private String buildNvidiaResponse(String content) throws Exception {
        Map<String, Object> responseObj = Map.of(
                "choices", List.of(
                        Map.of("message", Map.of("role", "assistant", "content", content))
                )
        );
        return new ObjectMapper().writeValueAsString(responseObj);
    }

    // ===== Authentication Handling Tests =====

    @Test
    void shouldInheritApiAuthForPositiveTestOnAuthenticatedApi() throws Exception {
        Api api = new Api();
        api.setId(1L);
        api.setName("Secured API");
        api.setUrl("http://localhost:8081/api/secured");
        api.setMethod("GET");
        api.setAuthType("BASIC");
        api.setUsername("admin");
        api.setPassword("secret123");
        when(apiRepository.findById(1L)).thenReturn(Optional.of(api));

        api_quality_platform.dto.ApiTestCreateRequest req = new api_quality_platform.dto.ApiTestCreateRequest();
        req.setExpectedStatus(200);
        req.setAuthType("NONE"); // AI returns NONE because it doesn't know credentials

        ApiTestSuggestionResponse resp = new ApiTestSuggestionResponse(1L, "Secured API", List.of(
                new ApiTestSuggestionResponse.SuggestionItem("Standard Success Test", "Tests 200 response", req)
        ));
        String responseBody = buildNvidiaResponse(objectMapper.writeValueAsString(resp));

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(NVIDIA_URL))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        AiTestSuggestionService svc = service(builder, TEST_KEY);
        ApiTestSuggestionResponse result = svc.generateAiSuggestions(1L, null);

        assertNotNull(result);
        assertEquals(1, result.getSuggestions().size());
        api_quality_platform.dto.ApiTestCreateRequest resultReq = result.getSuggestions().get(0).getTestRequest();
        assertEquals("BASIC", resultReq.getAuthType());
        assertEquals("admin", resultReq.getUsername());
        assertEquals("secret123", resultReq.getPassword());
        server.verify();
    }

    @Test
    void shouldPreserveMissingAuthNegativeTest() throws Exception {
        Api api = new Api();
        api.setId(1L);
        api.setName("Secured API");
        api.setUrl("http://localhost:8081/api/secured");
        api.setMethod("GET");
        api.setAuthType("BASIC");
        api.setUsername("admin");
        api.setPassword("secret123");
        when(apiRepository.findById(1L)).thenReturn(Optional.of(api));

        api_quality_platform.dto.ApiTestCreateRequest req = new api_quality_platform.dto.ApiTestCreateRequest();
        req.setExpectedStatus(401);
        req.setAuthType("NONE"); // Intentionally testing missing auth

        ApiTestSuggestionResponse resp = new ApiTestSuggestionResponse(1L, "Secured API", List.of(
                new ApiTestSuggestionResponse.SuggestionItem("Unauthorized Missing Auth Test", "Tests 401 without credentials", req)
        ));
        String responseBody = buildNvidiaResponse(objectMapper.writeValueAsString(resp));

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(NVIDIA_URL))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        AiTestSuggestionService svc = service(builder, TEST_KEY);
        ApiTestSuggestionResponse result = svc.generateAiSuggestions(1L, null);

        assertNotNull(result);
        api_quality_platform.dto.ApiTestCreateRequest resultReq = result.getSuggestions().get(0).getTestRequest();
        assertEquals("NONE", resultReq.getAuthType()); // Preserved as NONE
        assertNull(resultReq.getUsername());
        assertNull(resultReq.getPassword());
        server.verify();
    }

    @Test
    void shouldPreserveInvalidCredentialsNegativeTest() throws Exception {
        Api api = new Api();
        api.setId(1L);
        api.setName("Secured API");
        api.setUrl("http://localhost:8081/api/secured");
        api.setMethod("GET");
        api.setAuthType("BASIC");
        api.setUsername("admin");
        api.setPassword("secret123");
        when(apiRepository.findById(1L)).thenReturn(Optional.of(api));

        api_quality_platform.dto.ApiTestCreateRequest req = new api_quality_platform.dto.ApiTestCreateRequest();
        req.setExpectedStatus(401);
        req.setAuthType("BASIC");
        req.setUsername("wrong_user");
        req.setPassword("wrong_pass");

        ApiTestSuggestionResponse resp = new ApiTestSuggestionResponse(1L, "Secured API", List.of(
                new ApiTestSuggestionResponse.SuggestionItem("Invalid Credentials Test", "Tests 401 with bad credentials", req)
        ));
        String responseBody = buildNvidiaResponse(objectMapper.writeValueAsString(resp));

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(NVIDIA_URL))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        AiTestSuggestionService svc = service(builder, TEST_KEY);
        ApiTestSuggestionResponse result = svc.generateAiSuggestions(1L, null);

        assertNotNull(result);
        api_quality_platform.dto.ApiTestCreateRequest resultReq = result.getSuggestions().get(0).getTestRequest();
        assertEquals("BASIC", resultReq.getAuthType());
        assertEquals("wrong_user", resultReq.getUsername()); // Preserved invalid credentials
        assertEquals("wrong_pass", resultReq.getPassword()); // Not overwritten with real credentials
        server.verify();
    }

    @Test
    void shouldHandlePublicApiWithNoneAuth() throws Exception {
        Api api = buildApi(); // authType = "NONE"
        when(apiRepository.findById(1L)).thenReturn(Optional.of(api));

        api_quality_platform.dto.ApiTestCreateRequest req = new api_quality_platform.dto.ApiTestCreateRequest();
        req.setExpectedStatus(200);
        // authType is null - should default to NONE

        ApiTestSuggestionResponse resp = new ApiTestSuggestionResponse(1L, "ShopAPI Products", List.of(
                new ApiTestSuggestionResponse.SuggestionItem("Public Endpoint Test", "Tests public API", req)
        ));
        String responseBody = buildNvidiaResponse(objectMapper.writeValueAsString(resp));

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(NVIDIA_URL))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        AiTestSuggestionService svc = service(builder, TEST_KEY);
        ApiTestSuggestionResponse result = svc.generateAiSuggestions(1L, null);

        assertNotNull(result);
        api_quality_platform.dto.ApiTestCreateRequest resultReq = result.getSuggestions().get(0).getTestRequest();
        assertEquals("NONE", resultReq.getAuthType());
        server.verify();
    }

    @Test
    void shouldProvideInvalidCredentialsWhenMissing() throws Exception {
        Api api = new Api();
        api.setId(1L);
        api.setName("Secured API");
        api.setUrl("http://localhost:8081/api/secured");
        api.setMethod("GET");
        api.setAuthType("BASIC");
        api.setUsername("admin");
        api.setPassword("secret123");
        when(apiRepository.findById(1L)).thenReturn(Optional.of(api));

        api_quality_platform.dto.ApiTestCreateRequest req = new api_quality_platform.dto.ApiTestCreateRequest();
        req.setExpectedStatus(401);
        req.setAuthType("BASIC");
        // username and password are null - should get placeholder invalid values

        ApiTestSuggestionResponse resp = new ApiTestSuggestionResponse(1L, "Secured API", List.of(
                new ApiTestSuggestionResponse.SuggestionItem("Invalid Password Test", "Tests invalid credentials", req)
        ));
        String responseBody = buildNvidiaResponse(objectMapper.writeValueAsString(resp));

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(NVIDIA_URL))
                .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        AiTestSuggestionService svc = service(builder, TEST_KEY);
        ApiTestSuggestionResponse result = svc.generateAiSuggestions(1L, null);

        assertNotNull(result);
        api_quality_platform.dto.ApiTestCreateRequest resultReq = result.getSuggestions().get(0).getTestRequest();
        assertEquals("BASIC", resultReq.getAuthType());
        assertNotNull(resultReq.getUsername());
        assertNotNull(resultReq.getPassword());
        assertNotEquals("admin", resultReq.getUsername()); // Must be different from real credentials
        server.verify();
    }
}
