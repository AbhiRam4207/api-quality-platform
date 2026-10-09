package api_quality_platform.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.SocketTimeoutException;

import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.junit.jupiter.api.Assertions.*;

class ApiExecutionServiceErrorHandlingTest {

    @Test
    void shouldClassifySocketTimeoutAsTimeout() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer.bindTo(builder).build()
                .expect(requestTo("http://example.com"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(MockRestResponseCreators.withException(new SocketTimeoutException("Read timed out")));
        ApiExecutionService service = new ApiExecutionService(builder.build(), new com.fasterxml.jackson.databind.ObjectMapper());
        ApiExecutionResult result = service.execute("http://example.com", "GET", null, null, null, null, null, null, null, null, null, null);
        assertTrue(result.isError());
        assertEquals("TIMEOUT", result.getErrorType());
        assertNotNull(result.getErrorMessage());
        assertTrue(result.getErrorMessage().contains("Target API communication failed"));
    }

    @Test
    void shouldClassifyConnectionRefusedAsConnectionError() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer.bindTo(builder).build()
                .expect(requestTo("http://example.com"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(MockRestResponseCreators.withException(new ConnectException("Connection refused")));
        ApiExecutionService service = new ApiExecutionService(builder.build(), new com.fasterxml.jackson.databind.ObjectMapper());
        ApiExecutionResult result = service.execute("http://example.com", "GET", null, null, null, null, null, null, null, null, null, null);
        assertTrue(result.isError());
        assertEquals("CONNECTION_ERROR", result.getErrorType());
        assertNotNull(result.getErrorMessage());
    }

    @Test
    void shouldClassifyUnknownHostAsConnectionError() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer.bindTo(builder).build()
                .expect(requestTo("http://example.com"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(MockRestResponseCreators.withException(new java.net.UnknownHostException("example.com")));
        ApiExecutionService service = new ApiExecutionService(builder.build(), new com.fasterxml.jackson.databind.ObjectMapper());
        ApiExecutionResult result = service.execute("http://example.com", "GET", null, null, null, null, null, null, null, null, null, null);
        assertTrue(result.isError());
        assertEquals("CONNECTION_ERROR", result.getErrorType());
        assertNotNull(result.getErrorMessage());
    }

    @Test
    void shouldClassifyUnknownIoExceptionAsUnknownError() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer.bindTo(builder).build()
                .expect(requestTo("http://example.com"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(MockRestResponseCreators.withException(new IOException("Some I/O error")));
        ApiExecutionService service = new ApiExecutionService(builder.build(), new com.fasterxml.jackson.databind.ObjectMapper());
        ApiExecutionResult result = service.execute("http://example.com", "GET", null, null, null, null, null, null, null, null, null, null);
        assertTrue(result.isError());
        assertEquals("UNKNOWN_ERROR", result.getErrorType());
        assertNotNull(result.getErrorMessage());
    }

    @Test
    void shouldAcceptTimeoutParameter() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer.bindTo(builder).build()
                .expect(requestTo("http://example.com"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"id\":1}", MediaType.APPLICATION_JSON));
        ApiExecutionService service = new ApiExecutionService(builder.build(), new com.fasterxml.jackson.databind.ObjectMapper());
        ApiExecutionResult result = service.execute("http://example.com", "GET", null, null, null, null, null, null, null, null, null, null, 5000L);
        assertFalse(result.isError());
        assertTrue(result.getStatusCode() > 0);
    }

    @Test
    void shouldFailWithActualTimeout() throws Exception {
        ServerSocket serverSocket = new ServerSocket(0);
        int port = serverSocket.getLocalPort();

        Thread serverThread = new Thread(() -> {
            try {
                java.net.Socket socket = serverSocket.accept();
                Thread.sleep(5000);
            } catch (Exception e) {
                // ignore
            }
        });
        serverThread.start();

        try {
            ApiExecutionService service = new ApiExecutionService(
                    RestClient.builder().build(),
                    new com.fasterxml.jackson.databind.ObjectMapper()
            );
            ApiExecutionResult result = service.execute(
                    "http://localhost:" + port + "/",
                    "GET",
                    null, null, null, null, null, null, null, null, null, null,
                    100L
            );
            assertTrue(result.isError());
            assertEquals("TIMEOUT", result.getErrorType());
            assertNotNull(result.getErrorMessage());
        } finally {
            serverSocket.close();
            serverThread.interrupt();
        }
    }
}
