package api_quality_platform.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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

    @ParameterizedTest(name = "should send body for {0}")
    @CsvSource({"POST", "PUT", "PATCH"})
    void shouldSendBodyForBodySupportingMethods(String httpMethod) {
        RestClient.Builder builder = createBuilder("http://example.com", HttpMethod.valueOf(httpMethod));
        ApiExecutionService service = new ApiExecutionService(builder.build(), new ObjectMapper());
        ApiExecutionResult result = service.execute("http://example.com", httpMethod, null, null, "{\"name\":\"Widget\"}");
        assertEquals(200, result.getStatusCode());
    }

    @ParameterizedTest(name = "should not send body for {0}")
    @CsvSource({"GET", "DELETE"})
    void shouldNotSendBodyForNonBodyMethods(String httpMethod) {
        RestClient.Builder builder = createBuilder("http://example.com", HttpMethod.valueOf(httpMethod));
        ApiExecutionService service = new ApiExecutionService(builder.build(), new ObjectMapper());
        ApiExecutionResult result = service.execute("http://example.com", httpMethod, null, null, "{\"ignored\":true}");
        assertEquals(200, result.getStatusCode());
    }

    @ParameterizedTest(name = "should work without body for {0}")
    @CsvSource({"POST", "PUT", "PATCH"})
    void shouldWorkWithoutBodyForBodySupportingMethods(String httpMethod) {
        RestClient.Builder builder = createBuilder("http://example.com", HttpMethod.valueOf(httpMethod));
        ApiExecutionService service = new ApiExecutionService(builder.build(), new ObjectMapper());
        ApiExecutionResult result = service.execute("http://example.com", httpMethod, null, null, null);
        assertEquals(200, result.getStatusCode());
    }
}
