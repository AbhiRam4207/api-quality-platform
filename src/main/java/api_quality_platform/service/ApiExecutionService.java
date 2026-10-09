package api_quality_platform.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
public class ApiExecutionService {

    private final RestClient.Builder restClientBuilder;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Autowired
    public ApiExecutionService(RestClient.Builder builder, ObjectMapper objectMapper) {
        this(builder, builder.build(), objectMapper);
    }

    public ApiExecutionService(RestClient restClient, ObjectMapper objectMapper) {
        this(null, restClient, objectMapper);
    }

    private ApiExecutionService(RestClient.Builder builder, RestClient restClient, ObjectMapper objectMapper) {
        this.restClientBuilder = builder;
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    public ApiExecutionResult executeGet(String url, String username, String password) {
        return execute(url, "GET", "BASIC", username, password, null, null, null, null, null, null, null, null);
    }

    public ApiExecutionResult execute(String url, String method, String username, String password) {
        return execute(url, method, "BASIC", username, password, null, null, null, null, null, null, null, null);
    }

    public ApiExecutionResult execute(String url, String method, String username, String password, String body) {
        return execute(url, method, "BASIC", username, password, null, null, null, body, null, null, null, null);
    }

    public ApiExecutionResult execute(String url, String method, String username, String password, String body, String headersJson) {
        return execute(url, method, "BASIC", username, password, null, null, null, body, headersJson, null, null, null);
    }

    public ApiExecutionResult execute(String url, String method, String username, String password, String body, String headersJson, String queryParamsJson) {
        return execute(url, method, "BASIC", username, password, null, null, null, body, headersJson, queryParamsJson, null, null);
    }

    public ApiExecutionResult execute(String url, String method, String authType, String username, String password, String token, String apiKey, String apiKeyHeader, String body, String headersJson, String queryParamsJson) {
        return execute(url, method, authType, username, password, token, apiKey, apiKeyHeader, body, headersJson, queryParamsJson, null, null);
    }

    public ApiExecutionResult execute(String url, String method, String authType, String username, String password, String token, String apiKey, String apiKeyHeader, String body, String headersJson, String queryParamsJson, String pathParamsJson) {
        return execute(url, method, authType, username, password, token, apiKey, apiKeyHeader, body, headersJson, queryParamsJson, pathParamsJson, null);
    }

    public ApiExecutionResult execute(String url, String method, String authType, String username, String password, String token, String apiKey, String apiKeyHeader, String body, String headersJson, String queryParamsJson, String pathParamsJson, Long timeoutMs) {
        String upperMethod = method != null && !method.isBlank() ? method.toUpperCase() : "GET";

        RestClient restClientToUse = restClient;
        if (timeoutMs != null && timeoutMs > 0) {
            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(timeoutMs.intValue());
            factory.setReadTimeout(timeoutMs.intValue());
            if (restClientBuilder != null) {
                restClientToUse = restClientBuilder.requestFactory(factory).build();
            } else {
                restClientToUse = RestClient.builder().requestFactory(factory).build();
            }
        }

        RestClient.RequestHeadersUriSpec<?> spec = switch (upperMethod) {
            case "GET" -> restClientToUse.get();
            case "POST" -> restClientToUse.post();
            case "PUT" -> restClientToUse.put();
            case "PATCH" -> restClientToUse.patch();
            case "DELETE" -> restClientToUse.delete();
            default -> throw new IllegalArgumentException("Unsupported HTTP method: " + method);
        };

        String resolvedUrl = resolvePathParams(url, pathParamsJson);
        URI uri = buildUri(resolvedUrl, queryParamsJson);

        long start = System.currentTimeMillis();

        try {
            ApiExecutionResult result = spec
                    .uri(uri)
                    .headers(headers -> {
                        if (headersJson != null && !headersJson.isBlank()) {
                            try {
                                Map<String, String> customHeaders = objectMapper.readValue(
                                        headersJson,
                                        new TypeReference<Map<String, String>>() {}
                                );
                                for (Map.Entry<String, String> entry : customHeaders.entrySet()) {
                                    headers.add(entry.getKey(), entry.getValue());
                                }
                            } catch (Exception e) {
                                throw new IllegalArgumentException("Invalid headers JSON: " + e.getMessage(), e);
                            }
                        }

                        String upperAuthType = authType != null ? authType.toUpperCase() : "NONE";
                        switch (upperAuthType) {
                            case "BASIC" -> {
                                if (username != null && password != null && !username.isBlank() && !password.isBlank()) {
                                    headers.setBasicAuth(username, password);
                                }
                            }
                            case "BEARER" -> {
                                if (token != null && !token.isBlank()) {
                                    headers.add("Authorization", "Bearer " + token);
                                }
                            }
                            case "API_KEY" -> {
                                if (apiKey != null && !apiKey.isBlank() && apiKeyHeader != null && !apiKeyHeader.isBlank()) {
                                    headers.add(apiKeyHeader, apiKey);
                                }
                            }
                            case "NONE" -> {
                                // no authentication
                            }
                            default -> {
                                // no authentication for unknown types
                            }
                        }
                    })
                    .httpRequest(request -> {
                        if (body != null && !body.isBlank()) {
                            try {
                                request.getBody().write(body.getBytes(StandardCharsets.UTF_8));
                            } catch (IOException e) {
                                throw new UncheckedIOException(e);
                            }
                        }
                    })
                    .exchange((request, response) -> {
                        String responseBody = response.bodyTo(String.class);
                        return new ApiExecutionResult(
                                response.getStatusCode().value(),
                                responseBody,
                                System.currentTimeMillis() - start
                        );
                    });

            return result;
        } catch (ResourceAccessException ex) {
            long elapsed = System.currentTimeMillis() - start;
            String errorType = classifyError(ex);
            return new ApiExecutionResult(
                    "Target API communication failed: " + ex.getMessage(),
                    errorType,
                    elapsed
            );
        }
    }

    private String classifyError(ResourceAccessException ex) {
        Throwable cause = ex.getCause();
        if (cause instanceof SocketTimeoutException) {
            return "TIMEOUT";
        }
        if (cause instanceof ConnectException || cause instanceof java.net.UnknownHostException) {
            return "CONNECTION_ERROR";
        }
        return "UNKNOWN_ERROR";
    }

    private String resolvePathParams(String url, String pathParamsJson) {
        if (pathParamsJson == null || pathParamsJson.isBlank()) {
            return url;
        }
        String resolved = url;
        try {
            Map<String, String> params = objectMapper.readValue(
                    pathParamsJson,
                    new TypeReference<Map<String, String>>() {}
            );
            for (Map.Entry<String, String> entry : params.entrySet()) {
                resolved = resolved.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid path params JSON: " + e.getMessage(), e);
        }
        if (resolved.contains("{") && resolved.contains("}")) {
            throw new IllegalArgumentException("Unresolved placeholders in URL: " + url);
        }
        return resolved;
    }

    private URI buildUri(String url, String queryParamsJson) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(url);
        if (queryParamsJson != null && !queryParamsJson.isBlank()) {
            try {
                Map<String, String> params = objectMapper.readValue(
                        queryParamsJson,
                        new TypeReference<Map<String, String>>() {}
                );
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    builder.queryParam(entry.getKey(), entry.getValue());
                }
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid query params JSON: " + e.getMessage(), e);
            }
        }
        return builder.build().toUri();
    }
}
