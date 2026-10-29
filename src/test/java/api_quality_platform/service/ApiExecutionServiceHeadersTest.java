package api_quality_platform.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.junit.jupiter.api.Assertions.*;

class ApiExecutionServiceHeadersTest {

    @ParameterizedTest(name = "should apply custom headers for {0}")
    @ValueSource(strings = {"GET", "POST", "PUT", "PATCH", "DELETE"})
    void shouldApplyCustomHeadersForAllMethods(String httpMethod) {
        String headersJson = "{\"X-Custom-Header\":\"custom-value\"}";
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer.bindTo(builder).build()
                .expect(requestTo("http://example.com"))
                .andExpect(method(HttpMethod.valueOf(httpMethod)))
                .andExpect(header("X-Custom-Header", "custom-value"))
                .andRespond(withSuccess("{\"id\":1}", MediaType.APPLICATION_JSON));
        ApiExecutionService service = new ApiExecutionService(builder.build(), new com.fasterxml.jackson.databind.ObjectMapper());
        ApiExecutionResult result = service.execute("http://example.com", httpMethod, null, null, null, headersJson);
        assertEquals(200, result.getStatusCode());
    }

    @Test
    void shouldWorkWithoutHeadersForPost() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer.bindTo(builder).build()
                .expect(requestTo("http://example.com"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"id\":1}", MediaType.APPLICATION_JSON));
        ApiExecutionService service = new ApiExecutionService(builder.build(), new com.fasterxml.jackson.databind.ObjectMapper());
        ApiExecutionResult result = service.execute("http://example.com", "POST", null, null, null, null);
        assertEquals(200, result.getStatusCode());
    }

    @Test
    void shouldWorkWithoutHeadersForGet() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer.bindTo(builder).build()
                .expect(requestTo("http://example.com"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"id\":1}", MediaType.APPLICATION_JSON));
        ApiExecutionService service = new ApiExecutionService(builder.build(), new com.fasterxml.jackson.databind.ObjectMapper());
        ApiExecutionResult result = service.execute("http://example.com", "GET", null, null, null, null);
        assertEquals(200, result.getStatusCode());
    }

    @Test
    void shouldNotOverwriteAuthorizationWhenBasicAuthConfigured() {
        String customAuth = "CustomToken";
        String headersJson = "{\"Authorization\":\"" + customAuth + "\"}";
        String username = "user";
        String password = "pass";
        String expectedBasicAuth = "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://example.com"))
              .andExpect(method(HttpMethod.POST))
              .andExpect(header("Authorization", expectedBasicAuth))
              .andRespond(withSuccess("{\"id\":1}", MediaType.APPLICATION_JSON));

        ApiExecutionService service = new ApiExecutionService(builder.build(), new com.fasterxml.jackson.databind.ObjectMapper());
        ApiExecutionResult result = service.execute("http://example.com", "POST", username, password, null, headersJson);
        assertEquals(200, result.getStatusCode());
    }

    @Test
    void shouldPreserveCustomAuthorizationWithoutBasicAuth() {
        String customAuth = "CustomToken";
        String headersJson = "{\"Authorization\":\"" + customAuth + "\"}";

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://example.com"))
              .andExpect(method(HttpMethod.POST))
              .andExpect(header("Authorization", customAuth))
              .andRespond(withSuccess("{\"id\":1}", MediaType.APPLICATION_JSON));

        ApiExecutionService service = new ApiExecutionService(builder.build(), new com.fasterxml.jackson.databind.ObjectMapper());
        ApiExecutionResult result = service.execute("http://example.com", "POST", null, null, null, headersJson);
        assertEquals(200, result.getStatusCode());
    }
}
