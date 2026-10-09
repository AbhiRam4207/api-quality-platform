package api_quality_platform.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.junit.jupiter.api.Assertions.*;

class ApiExecutionServicePathParamsTest {

    @Test
    void shouldResolveSinglePathParam() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer.bindTo(builder).build()
                .expect(requestTo("http://example.com/api/users/123"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"id\":1}", MediaType.APPLICATION_JSON));
        ApiExecutionService service = new ApiExecutionService(builder.build(), new com.fasterxml.jackson.databind.ObjectMapper());
        ApiExecutionResult result = service.execute(
                "http://example.com/api/users/{id}", "GET", null, null, null, null, null, null, null, null, null,
                "{\"id\":\"123\"}"
        );
        assertEquals(200, result.getStatusCode());
    }

    @Test
    void shouldResolveMultiplePathParams() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer.bindTo(builder).build()
                .expect(requestTo("http://example.com/api/accounts/1/users/42"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"id\":1}", MediaType.APPLICATION_JSON));
        ApiExecutionService service = new ApiExecutionService(builder.build(), new com.fasterxml.jackson.databind.ObjectMapper());
        ApiExecutionResult result = service.execute(
                "http://example.com/api/accounts/{accountId}/users/{userId}", "GET", null, null, null, null, null, null, null, null, null,
                "{\"accountId\":\"1\",\"userId\":\"42\"}"
        );
        assertEquals(200, result.getStatusCode());
    }

    @Test
    void shouldEncodePathParamValues() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer.bindTo(builder).build()
                .expect(requestTo("http://example.com/api/users/john%20doe"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"id\":1}", MediaType.APPLICATION_JSON));
        ApiExecutionService service = new ApiExecutionService(builder.build(), new com.fasterxml.jackson.databind.ObjectMapper());
        ApiExecutionResult result = service.execute(
                "http://example.com/api/users/{name}", "GET", null, null, null, null, null, null, null, null, null,
                "{\"name\":\"john doe\"}"
        );
        assertEquals(200, result.getStatusCode());
    }

    @Test
    void shouldFailForUnresolvedPlaceholder() {
        RestClient.Builder builder = RestClient.builder();
        ApiExecutionService service = new ApiExecutionService(builder.build(), new com.fasterxml.jackson.databind.ObjectMapper());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.execute(
                        "http://example.com/api/users/{id}", "GET", null, null, null, null, null, null, null, null, null,
                        "{\"userId\":\"123\"}"
                )
        );

        assertTrue(exception.getMessage().contains("Unresolved placeholders in URL"));
    }

    @Test
    void shouldPreserveExistingBehaviorWithoutPathParams() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer.bindTo(builder).build()
                .expect(requestTo("http://example.com/api/users"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"id\":1}", MediaType.APPLICATION_JSON));
        ApiExecutionService service = new ApiExecutionService(builder.build(), new com.fasterxml.jackson.databind.ObjectMapper());
        ApiExecutionResult result = service.execute("http://example.com/api/users", "GET", null, null);
        assertEquals(200, result.getStatusCode());
    }

    @Test
    void shouldPreserveExistingBehaviorWithNullPathParams() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer.bindTo(builder).build()
                .expect(requestTo("http://example.com/api/users/123"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"id\":1}", MediaType.APPLICATION_JSON));
        ApiExecutionService service = new ApiExecutionService(builder.build(), new com.fasterxml.jackson.databind.ObjectMapper());
        ApiExecutionResult result = service.execute(
                "http://example.com/api/users/123", "GET", null, null, null, null, null, null, null, null, null, null
        );
        assertEquals(200, result.getStatusCode());
    }

    @Test
    void shouldFailForInvalidPathParamsJson() {
        RestClient.Builder builder = RestClient.builder();
        ApiExecutionService service = new ApiExecutionService(builder.build(), new com.fasterxml.jackson.databind.ObjectMapper());

        assertThrows(
                IllegalArgumentException.class,
                () -> service.execute(
                        "http://example.com/api/users/{id}", "GET", null, null, null, null, null, null, null, null, null,
                        "not-json"
                )
        );
    }

    @Test
    void shouldCombinePathParamsWithQueryParams() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer.bindTo(builder).build()
                .expect(requestTo("http://example.com/api/users/123?active=true"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"id\":1}", MediaType.APPLICATION_JSON));
        ApiExecutionService service = new ApiExecutionService(builder.build(), new com.fasterxml.jackson.databind.ObjectMapper());
        ApiExecutionResult result = service.execute(
                "http://example.com/api/users/{id}", "GET", null, null, null, null, null, null, null, null,
                "{\"active\":\"true\"}",
                "{\"id\":\"123\"}"
        );
        assertEquals(200, result.getStatusCode());
    }
}
