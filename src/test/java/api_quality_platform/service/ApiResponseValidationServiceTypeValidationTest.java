package api_quality_platform.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ApiResponseValidationServiceTypeValidationTest {

    private final ApiResponseValidationService service =
            new ApiResponseValidationService(new ObjectMapper());

    @Test
    void shouldPassWhenTopLevelTypeMatches() {
        String body = "{\"id\": 1, \"name\": \"Widget\"}";
        String types = "{\"id\":\"NUMBER\",\"name\":\"STRING\"}";
        ValidationResult result = service.validate(200, 200, body, null, types);
        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    void shouldFailWhenTopLevelTypeMismatches() {
        String body = "{\"id\": \"1\", \"name\": \"Widget\"}";
        String types = "{\"id\":\"NUMBER\"}";
        ValidationResult result = service.validate(200, 200, body, null, types);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains(
                "Type mismatch for field 'id': expected NUMBER, got STRING"));
    }

    @Test
    void shouldPassWhenNestedTypeMatches() {
        String body = "{\"customer\": {\"email\": \"abc@test.com\"}}";
        String types = "{\"customer.email\":\"STRING\"}";
        ValidationResult result = service.validate(200, 200, body, null, types);
        assertTrue(result.isValid());
    }

    @Test
    void shouldFailWhenNestedTypeMismatches() {
        String body = "{\"customer\": {\"email\": 123}}";
        String types = "{\"customer.email\":\"STRING\"}";
        ValidationResult result = service.validate(200, 200, body, null, types);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains(
                "Type mismatch for field 'customer.email': expected STRING, got NUMBER"));
    }

    @Test
    void shouldPassWhenArrayTypeMatches() {
        String body = "{\"products\": [{\"price\": 99.0}]}";
        String types = "{\"products[].price\":\"NUMBER\"}";
        ValidationResult result = service.validate(200, 200, body, null, types);
        assertTrue(result.isValid());
    }

    @Test
    void shouldFailWhenArrayTypeMismatches() {
        String body = "{\"products\": [{\"price\": \"99.0\"}]}";
        String types = "{\"products[].price\":\"NUMBER\"}";
        ValidationResult result = service.validate(200, 200, body, null, types);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains(
                "Type mismatch for field 'products[].price': expected NUMBER, got STRING"));
    }

    @Test
    void shouldFailWhenTypeFieldIsMissing() {
        String body = "{\"id\": 1}";
        String types = "{\"name\":\"STRING\"}";
        ValidationResult result = service.validate(200, 200, body, null, types);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("Missing field: name"));
    }

    @Test
    void shouldFailWhenArrayTypeFieldIsMissing() {
        String body = "{\"products\": [{\"id\": 1}]}";
        String types = "{\"products[].price\":\"NUMBER\"}";
        ValidationResult result = service.validate(200, 200, body, null, types);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("Missing field: products[].price"));
    }

    @Test
    void shouldReportMultipleTypeErrors() {
        String body = "{\"id\": \"1\", \"name\": 123}";
        String types = "{\"id\":\"NUMBER\",\"name\":\"STRING\"}";
        ValidationResult result = service.validate(200, 200, body, null, types);
        assertFalse(result.isValid());
        assertEquals(2, result.getErrors().size());
        assertTrue(result.getErrors().contains(
                "Type mismatch for field 'id': expected NUMBER, got STRING"));
        assertTrue(result.getErrors().contains(
                "Type mismatch for field 'name': expected STRING, got NUMBER"));
    }

    @Test
    void shouldPassWhenBooleanTypeMatches() {
        String body = "{\"active\": true}";
        String types = "{\"active\":\"BOOLEAN\"}";
        ValidationResult result = service.validate(200, 200, body, null, types);
        assertTrue(result.isValid());
    }

    @Test
    void shouldPassWhenNullTypeMatches() {
        String body = "{\"discount\": null}";
        String types = "{\"discount\":\"NULL\"}";
        ValidationResult result = service.validate(200, 200, body, null, types);
        assertTrue(result.isValid());
    }

    @Test
    void shouldPassWhenObjectAndArrayTypeMatch() {
        String body = "{\"customer\": {\"name\": \"A\"}, \"tags\": [\"x\", \"y\"]}";
        String types = "{\"customer\":\"OBJECT\",\"tags\":\"ARRAY\"}";
        ValidationResult result = service.validate(200, 200, body, null, types);
        assertTrue(result.isValid());
    }
}
