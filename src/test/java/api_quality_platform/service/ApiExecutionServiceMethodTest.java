package api_quality_platform.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.junit.jupiter.api.Assertions.*;

class ApiExecutionServiceMethodTest {

    private RestClient createMockedRestClient(String expectedUrl, HttpMethod expectedMethod) throws Exception {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(expectedUrl))
              .andExpect(method(expectedMethod))
              .andRespond(withSuccess("{\"id\":1}", MediaType.APPLICATION_JSON));
        return builder.build();
    }

    @Test
    void shouldUseGetMethod() throws Exception {
        RestClient restClient = createMockedRestClient("http://example.com", HttpMethod.GET);
        ApiExecutionService service = new ApiExecutionService(restClient, new ObjectMapper());
        ApiExecutionResult result = service.execute("http://example.com", "GET", null, null);
        assertEquals(200, result.getStatusCode());
    }

    @Test
    void shouldUsePostMethod() throws Exception {
        RestClient restClient = createMockedRestClient("http://example.com", HttpMethod.POST);
        ApiExecutionService service = new ApiExecutionService(restClient, new ObjectMapper());
        ApiExecutionResult result = service.execute("http://example.com", "POST", null, null);
        assertEquals(200, result.getStatusCode());
    }

    @Test
    void shouldUsePutMethod() throws Exception {
        RestClient restClient = createMockedRestClient("http://example.com", HttpMethod.PUT);
        ApiExecutionService service = new ApiExecutionService(restClient, new ObjectMapper());
        ApiExecutionResult result = service.execute("http://example.com", "PUT", null, null);
        assertEquals(200, result.getStatusCode());
    }

    @Test
    void shouldUsePatchMethod() throws Exception {
        RestClient restClient = createMockedRestClient("http://example.com", HttpMethod.PATCH);
        ApiExecutionService service = new ApiExecutionService(restClient, new ObjectMapper());
        ApiExecutionResult result = service.execute("http://example.com", "PATCH", null, null);
        assertEquals(200, result.getStatusCode());
    }

    @Test
    void shouldUseDeleteMethod() throws Exception {
        RestClient restClient = createMockedRestClient("http://example.com", HttpMethod.DELETE);
        ApiExecutionService service = new ApiExecutionService(restClient, new ObjectMapper());
        ApiExecutionResult result = service.execute("http://example.com", "DELETE", null, null);
        assertEquals(200, result.getStatusCode());
    }

    @Test
    void shouldFailForUnsupportedMethod() {
        RestClient.Builder builder = RestClient.builder();
        ApiExecutionService service = new ApiExecutionService(builder.build(), new ObjectMapper());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.execute("http://example.com", "OPTIONS", null, null)
        );

        assertTrue(exception.getMessage().contains("Unsupported HTTP method"));
    }

    @Test
    void shouldDefaultToGetWhenMethodIsNull() throws Exception {
        RestClient restClient = createMockedRestClient("http://example.com", HttpMethod.GET);
        ApiExecutionService service = new ApiExecutionService(restClient, new ObjectMapper());
        ApiExecutionResult result = service.execute("http://example.com", null, null, null);
        assertEquals(200, result.getStatusCode());
    }

    @Test
    void shouldDefaultToGetWhenMethodIsEmpty() throws Exception {
        RestClient restClient = createMockedRestClient("http://example.com", HttpMethod.GET);
        ApiExecutionService service = new ApiExecutionService(restClient, new ObjectMapper());
        ApiExecutionResult result = service.execute("http://example.com", "", null, null);
        assertEquals(200, result.getStatusCode());
    }
}
