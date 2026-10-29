package api_quality_platform.service;

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
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.junit.jupiter.api.Assertions.*;

class ApiExecutionServiceQueryParamsTest {

    @ParameterizedTest(name = "should apply query params for {0}")
    @CsvSource({
        "GET, filter, active, http://example.com?filter=active",
        "POST, include, details, http://example.com?include=details",
        "PUT, version, 2, http://example.com?version=2",
        "PATCH, field, name, http://example.com?field=name",
        "DELETE, force, true, http://example.com?force=true"
    })
    void shouldApplyQueryParamsForAllMethods(String httpMethod, String paramName, String paramValue, String expectedUrl) {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer.bindTo(builder).build()
                .expect(requestTo(expectedUrl))
                .andExpect(method(HttpMethod.valueOf(httpMethod)))
                .andExpect(queryParam(paramName, paramValue))
                .andRespond(withSuccess("{\"id\":1}", MediaType.APPLICATION_JSON));
        ApiExecutionService service = new ApiExecutionService(builder.build(), new com.fasterxml.jackson.databind.ObjectMapper());
        ApiExecutionResult result = service.execute("http://example.com", httpMethod, null, null, null, null, "{\"" + paramName + "\":\"" + paramValue + "\"}");
        assertEquals(200, result.getStatusCode());
    }

    @Test
    void shouldApplyMultipleQueryParamsForGet() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer.bindTo(builder).build()
                .expect(requestTo("http://example.com?page=1&limit=10"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("page", "1"))
                .andExpect(queryParam("limit", "10"))
                .andRespond(withSuccess("{\"id\":1}", MediaType.APPLICATION_JSON));
        ApiExecutionService service = new ApiExecutionService(builder.build(), new com.fasterxml.jackson.databind.ObjectMapper());
        ApiExecutionResult result = service.execute("http://example.com", "GET", null, null, null, null, "{\"page\":\"1\",\"limit\":\"10\"}");
        assertEquals(200, result.getStatusCode());
    }

    @Test
    void shouldPreserveExistingBehaviorWithoutQueryParams() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer.bindTo(builder).build()
                .expect(requestTo("http://example.com"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"id\":1}", MediaType.APPLICATION_JSON));
        ApiExecutionService service = new ApiExecutionService(builder.build(), new com.fasterxml.jackson.databind.ObjectMapper());
        ApiExecutionResult result = service.execute("http://example.com", "GET", null, null);
        assertEquals(200, result.getStatusCode());
    }

    @Test
    void shouldPreserveExistingBehaviorWithNullQueryParams() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer.bindTo(builder).build()
                .expect(requestTo("http://example.com"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"id\":1}", MediaType.APPLICATION_JSON));
        ApiExecutionService service = new ApiExecutionService(builder.build(), new com.fasterxml.jackson.databind.ObjectMapper());
        ApiExecutionResult result = service.execute("http://example.com", "POST", null, null, null, null, null);
        assertEquals(200, result.getStatusCode());
    }

    @Test
    void shouldPreserveExistingBehaviorWithEmptyQueryParams() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer.bindTo(builder).build()
                .expect(requestTo("http://example.com"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"id\":1}", MediaType.APPLICATION_JSON));
        ApiExecutionService service = new ApiExecutionService(builder.build(), new com.fasterxml.jackson.databind.ObjectMapper());
        ApiExecutionResult result = service.execute("http://example.com", "POST", null, null, null, null, "");
        assertEquals(200, result.getStatusCode());
    }
}
