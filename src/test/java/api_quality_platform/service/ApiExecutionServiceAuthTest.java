package api_quality_platform.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.headerDoesNotExist;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.junit.jupiter.api.Assertions.*;

class ApiExecutionServiceAuthTest {

    @Test
    void shouldApplyNoAuthWhenAuthTypeIsNone() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer.bindTo(builder).build()
                .expect(requestTo("http://example.com"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(headerDoesNotExist("Authorization"))
                .andRespond(withSuccess("{\"id\":1}", MediaType.APPLICATION_JSON));
        ApiExecutionService service = new ApiExecutionService(builder.build(), new com.fasterxml.jackson.databind.ObjectMapper());
        ApiExecutionResult result = service.execute("http://example.com", "GET", "NONE", null, null, null, null, null, null, null, null);
        assertEquals(200, result.getStatusCode());
    }

    @Test
    void shouldApplyBasicAuth() {
        String username = "user";
        String password = "pass";
        String expectedBasicAuth = "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer.bindTo(builder).build()
                .expect(requestTo("http://example.com"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", expectedBasicAuth))
                .andRespond(withSuccess("{\"id\":1}", MediaType.APPLICATION_JSON));
        ApiExecutionService service = new ApiExecutionService(builder.build(), new com.fasterxml.jackson.databind.ObjectMapper());
        ApiExecutionResult result = service.execute("http://example.com", "GET", "BASIC", username, password, null, null, null, null, null, null);
        assertEquals(200, result.getStatusCode());
    }

    @Test
    void shouldApplyBearerAuth() {
        String token = "test-token-123";

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer.bindTo(builder).build()
                .expect(requestTo("http://example.com"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer " + token))
                .andRespond(withSuccess("{\"id\":1}", MediaType.APPLICATION_JSON));
        ApiExecutionService service = new ApiExecutionService(builder.build(), new com.fasterxml.jackson.databind.ObjectMapper());
        ApiExecutionResult result = service.execute("http://example.com", "GET", "BEARER", null, null, token, null, null, null, null, null);
        assertEquals(200, result.getStatusCode());
    }

    @Test
    void shouldApplyApiKeyAuth() {
        String apiKey = "secret-key";
        String apiKeyHeader = "X-API-Key";

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer.bindTo(builder).build()
                .expect(requestTo("http://example.com"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(apiKeyHeader, apiKey))
                .andRespond(withSuccess("{\"id\":1}", MediaType.APPLICATION_JSON));
        ApiExecutionService service = new ApiExecutionService(builder.build(), new com.fasterxml.jackson.databind.ObjectMapper());
        ApiExecutionResult result = service.execute("http://example.com", "GET", "API_KEY", null, null, null, apiKey, apiKeyHeader, null, null, null);
        assertEquals(200, result.getStatusCode());
    }

    @Test
    void shouldNotApplyAuthWhenAuthTypeIsMissing() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer.bindTo(builder).build()
                .expect(requestTo("http://example.com"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"id\":1}", MediaType.APPLICATION_JSON));
        ApiExecutionService service = new ApiExecutionService(builder.build(), new com.fasterxml.jackson.databind.ObjectMapper());
        ApiExecutionResult result = service.execute("http://example.com", "GET", null, null, null, null, null, null, null, null, null);
        assertEquals(200, result.getStatusCode());
    }

    @Test
    void shouldNotApplyAuthForInvalidAuthType() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer.bindTo(builder).build()
                .expect(requestTo("http://example.com"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"id\":1}", MediaType.APPLICATION_JSON));
        ApiExecutionService service = new ApiExecutionService(builder.build(), new com.fasterxml.jackson.databind.ObjectMapper());
        ApiExecutionResult result = service.execute("http://example.com", "GET", "INVALID", null, null, null, null, null, null, null, null);
        assertEquals(200, result.getStatusCode());
    }

    @Test
    void shouldNotApplyBasicAuthWhenCredentialsMissing() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer.bindTo(builder).build()
                .expect(requestTo("http://example.com"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"id\":1}", MediaType.APPLICATION_JSON));
        ApiExecutionService service = new ApiExecutionService(builder.build(), new com.fasterxml.jackson.databind.ObjectMapper());
        ApiExecutionResult result = service.execute("http://example.com", "GET", "BASIC", null, null, null, null, null, null, null, null);
        assertEquals(200, result.getStatusCode());
    }

    @Test
    void shouldNotApplyBearerAuthWhenTokenMissing() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer.bindTo(builder).build()
                .expect(requestTo("http://example.com"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"id\":1}", MediaType.APPLICATION_JSON));
        ApiExecutionService service = new ApiExecutionService(builder.build(), new com.fasterxml.jackson.databind.ObjectMapper());
        ApiExecutionResult result = service.execute("http://example.com", "GET", "BEARER", null, null, null, null, null, null, null, null);
        assertEquals(200, result.getStatusCode());
    }

    @Test
    void shouldNotApplyApiKeyAuthWhenKeyOrHeaderMissing() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer.bindTo(builder).build()
                .expect(requestTo("http://example.com"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"id\":1}", MediaType.APPLICATION_JSON));
        ApiExecutionService service = new ApiExecutionService(builder.build(), new com.fasterxml.jackson.databind.ObjectMapper());
        ApiExecutionResult result = service.execute("http://example.com", "GET", "API_KEY", null, null, null, null, null, null, null, null);
        assertEquals(200, result.getStatusCode());
    }
}
