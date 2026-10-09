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

class ApiExecutionServiceRequestBodyTest {

    private RestClient.Builder createBuilder(String expectedUrl, HttpMethod expectedMethod) {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer.bindTo(builder).build()
                .expect(requestTo(expectedUrl))
                .andExpect(method(expectedMethod))
                .andRespond(withSuccess("{\"id\":1}", MediaType.APPLICATION_JSON));
        return builder;
    }

    @Test
    void shouldSendBodyForPost() {
        RestClient.Builder builder = createBuilder("http://example.com", HttpMethod.POST);
        ApiExecutionService service = new ApiExecutionService(builder.build(), new ObjectMapper());
        ApiExecutionResult result = service.execute("http://example.com", "POST", null, null, "{\"name\":\"Widget\"}");
        assertEquals(200, result.getStatusCode());
    }

    @Test
    void shouldSendBodyForPut() {
        RestClient.Builder builder = createBuilder("http://example.com", HttpMethod.PUT);
        ApiExecutionService service = new ApiExecutionService(builder.build(), new ObjectMapper());
        ApiExecutionResult result = service.execute("http://example.com", "PUT", null, null, "{\"name\":\"Widget\"}");
        assertEquals(200, result.getStatusCode());
    }

    @Test
    void shouldSendBodyForPatch() {
        RestClient.Builder builder = createBuilder("http://example.com", HttpMethod.PATCH);
        ApiExecutionService service = new ApiExecutionService(builder.build(), new ObjectMapper());
        ApiExecutionResult result = service.execute("http://example.com", "PATCH", null, null, "{\"name\":\"Widget\"}");
        assertEquals(200, result.getStatusCode());
    }

    @Test
    void shouldNotSendBodyForGet() {
        RestClient.Builder builder = createBuilder("http://example.com", HttpMethod.GET);
        ApiExecutionService service = new ApiExecutionService(builder.build(), new ObjectMapper());
        ApiExecutionResult result = service.execute("http://example.com", "GET", null, null, "{\"ignored\":true}");
        assertEquals(200, result.getStatusCode());
    }

    @Test
    void shouldNotSendBodyForDelete() {
        RestClient.Builder builder = createBuilder("http://example.com", HttpMethod.DELETE);
        ApiExecutionService service = new ApiExecutionService(builder.build(), new ObjectMapper());
        ApiExecutionResult result = service.execute("http://example.com", "DELETE", null, null, "{\"ignored\":true}");
        assertEquals(200, result.getStatusCode());
    }

    @Test
    void shouldWorkWithoutBodyForPost() {
        RestClient.Builder builder = createBuilder("http://example.com", HttpMethod.POST);
        ApiExecutionService service = new ApiExecutionService(builder.build(), new ObjectMapper());
        ApiExecutionResult result = service.execute("http://example.com", "POST", null, null, null);
        assertEquals(200, result.getStatusCode());
    }

    @Test
    void shouldWorkWithoutBodyForPut() {
        RestClient.Builder builder = createBuilder("http://example.com", HttpMethod.PUT);
        ApiExecutionService service = new ApiExecutionService(builder.build(), new ObjectMapper());
        ApiExecutionResult result = service.execute("http://example.com", "PUT", null, null, null);
        assertEquals(200, result.getStatusCode());
    }

    @Test
    void shouldWorkWithoutBodyForPatch() {
        RestClient.Builder builder = createBuilder("http://example.com", HttpMethod.PATCH);
        ApiExecutionService service = new ApiExecutionService(builder.build(), new ObjectMapper());
        ApiExecutionResult result = service.execute("http://example.com", "PATCH", null, null, null);
        assertEquals(200, result.getStatusCode());
    }
}
