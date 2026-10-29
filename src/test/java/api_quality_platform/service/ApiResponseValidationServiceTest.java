package api_quality_platform.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ApiResponseValidationServiceTest {

    private final ApiResponseValidationService validationService = new ApiResponseValidationService(new ObjectMapper());

    @Test
    void shouldPassWhenStatusAndFieldsAreCorrect() {
        String body = "{\"id\": 1, \"name\": \"Widget\"}";
        ValidationResult result = validationService.validate(200, 200, body, "id,name");
        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    void shouldFailWhenStatusIsIncorrect() {
        String body = "{\"id\": 1}";
        ValidationResult result = validationService.validate(200, 404, body, null);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("Expected status 200 but got 404"));
    }

    @Test
    void shouldFailWhenRequiredFieldIsMissing() {
        String body = "{\"id\": 1}";
        ValidationResult result = validationService.validate(200, 200, body, "id,name");
        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("Missing required field: name"));
    }

    @Test
    void shouldPassWhenRequiredFieldIsPresent() {
        String body = "{\"id\": 1, \"name\": \"Widget\"}";
        ValidationResult result = validationService.validate(200, 200, body, "name");
        assertTrue(result.isValid());
    }

    @Test
    void shouldFailWithInvalidJson() {
        ValidationResult result = validationService.validate(200, 200, "not json", "id");
        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("Invalid JSON response body for field validation"));
    }

    @Test
    void shouldPassWithNoRequiredFields() {
        String body = "anything";
        ValidationResult result = validationService.validate(200, 200, body, null);
        assertTrue(result.isValid());
    }

    @Test
    void shouldHandleNestedRequiredField() {
        String body = "{\"customer\": {\"name\": \"Abhiram\"}}";
        ValidationResult result = validationService.validate(200, 200, body, "customer.name");
        assertTrue(result.isValid());
    }

    @Test
    void shouldFailWhenNestedRequiredFieldIsMissing() {
        String body = "{\"customer\": {}}";
        ValidationResult result = validationService.validate(200, 200, body, "customer.name");
        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("Missing required field: customer.name"));
    }

    @Test
    void shouldPassResponseTimeWhenWithinLimit() {
        ValidationResult result = validationService.validateResponseTime(100L, 50L);
        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    void shouldFailResponseTimeWhenExceededLimit() {
        ValidationResult result = validationService.validateResponseTime(100L, 150L);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("Response time 150ms exceeded max allowed 100ms"));
    }

    @Test
    void shouldPassResponseTimeWhenNoLimit() {
        ValidationResult result = validationService.validateResponseTime(null, 500L);
        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    void shouldPassResponseTimeAtExactLimit() {
        ValidationResult result = validationService.validateResponseTime(100L, 100L);
        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    void shouldPassWhenArrayPathIsValid() {
        String body = "[{\"id\": 1, \"name\": \"Widget\", \"price\": 9.99}]";
        ValidationResult result = validationService.validate(200, 200, body, "[0].id,[0].name,[0].price");
        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    void shouldFailWhenArrayFieldIsMissing() {
        String body = "[{\"id\": 1}]";
        ValidationResult result = validationService.validate(200, 200, body, "[0].id,[0].name");
        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("Missing required field: [0].name"));
    }

    @Test
    void shouldFailWhenArrayIndexOutOfBounds() {
        String body = "[{\"id\": 1}]";
        ValidationResult result = validationService.validate(200, 200, body, "[5].id");
        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("Missing required field: [5].id"));
    }

    @Test
    void shouldPassWhenNestedObjectInsideArrayIsValid() {
        String body = "{\"products\": [{\"details\": {\"sku\": \"ABC-123\"}}]}";
        ValidationResult result = validationService.validate(200, 200, body, "products[0].details.sku");
        assertTrue(result.isValid());
    }
}