package api_quality_platform.service;

import org.junit.jupiter.api.Test;
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

    @Test
    void shouldApplySingleQueryParamForGet() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer.bindTo(builder).build()
                .expect(requestTo("http://example.com?filter=active"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("filter", "active"))
                .andRespond(withSuccess("{\"id\":1}", MediaType.APPLICATION_JSON));
        ApiExecutionService service = new ApiExecutionService(builder.build(), new com.fasterxml.jackson.databind.ObjectMapper());
        ApiExecutionResult result = service.execute("http://example.com", "GET", null, null, null, null, "{\"filter\":\"active\"}");
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
    void shouldApplyQueryParamsForPost() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer.bindTo(builder).build()
                .expect(requestTo("http://example.com?include=details"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(queryParam("include", "details"))
                .andRespond(withSuccess("{\"id\":1}", MediaType.APPLICATION_JSON));
        ApiExecutionService service = new ApiExecutionService(builder.build(), new com.fasterxml.jackson.databind.ObjectMapper());
        ApiExecutionResult result = service.execute("http://example.com", "POST", null, null, null, null, "{\"include\":\"details\"}");
        assertEquals(200, result.getStatusCode());
    }

    @Test
    void shouldApplyQueryParamsForPut() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer.bindTo(builder).build()
                .expect(requestTo("http://example.com?version=2"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(queryParam("version", "2"))
                .andRespond(withSuccess("{\"id\":1}", MediaType.APPLICATION_JSON));
        ApiExecutionService service = new ApiExecutionService(builder.build(), new com.fasterxml.jackson.databind.ObjectMapper());
        ApiExecutionResult result = service.execute("http://example.com", "PUT", null, null, null, null, "{\"version\":\"2\"}");
        assertEquals(200, result.getStatusCode());
    }

    @Test
    void shouldApplyQueryParamsForPatch() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer.bindTo(builder).build()
                .expect(requestTo("http://example.com?field=name"))
                .andExpect(method(HttpMethod.PATCH))
                .andExpect(queryParam("field", "name"))
                .andRespond(withSuccess("{\"id\":1}", MediaType.APPLICATION_JSON));
        ApiExecutionService service = new ApiExecutionService(builder.build(), new com.fasterxml.jackson.databind.ObjectMapper());
        ApiExecutionResult result = service.execute("http://example.com", "PATCH", null, null, null, null, "{\"field\":\"name\"}");
        assertEquals(200, result.getStatusCode());
    }

    @Test
    void shouldApplyQueryParamsForDelete() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer.bindTo(builder).build()
                .expect(requestTo("http://example.com?force=true"))
                .andExpect(method(HttpMethod.DELETE))
                .andExpect(queryParam("force", "true"))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON));
        ApiExecutionService service = new ApiExecutionService(builder.build(), new com.fasterxml.jackson.databind.ObjectMapper());
        ApiExecutionResult result = service.execute("http://example.com", "DELETE", null, null, null, null, "{\"force\":\"true\"}");
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
