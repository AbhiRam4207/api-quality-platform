package api_quality_platform.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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

    @ParameterizedTest(name = "should use {0} method")
    @ValueSource(strings = {"GET", "POST", "PUT", "PATCH", "DELETE"})
    void shouldUseSupportedHttpMethods(String httpMethod) throws Exception {
        HttpMethod method = HttpMethod.valueOf(httpMethod);
        RestClient restClient = createMockedRestClient("http://example.com", method);
        ApiExecutionService service = new ApiExecutionService(restClient, new ObjectMapper());
        ApiExecutionResult result = service.execute("http://example.com", httpMethod, null, null);
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
