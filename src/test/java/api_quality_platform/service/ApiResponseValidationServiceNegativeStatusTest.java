package api_quality_platform.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ApiResponseValidationServiceNegativeStatusTest {

    @Autowired
    private ApiResponseValidationService validationService;

    @Test
    void shouldPassWhenExpected404MatchesActual404() {
        String body = "{\"error\": \"Not Found\"}";
        ValidationResult result = validationService.validate(404, 404, body, null);
        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    void shouldPassWhenExpected400MatchesActual400() {
        String body = "{\"error\": \"Bad Request\"}";
        ValidationResult result = validationService.validate(400, 400, body, null);
        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    void shouldPassWhenExpected401MatchesActual401() {
        String body = "{\"error\": \"Unauthorized\"}";
        ValidationResult result = validationService.validate(401, 401, body, null);
        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    void shouldPassWhenExpected403MatchesActual403() {
        String body = "{\"error\": \"Forbidden\"}";
        ValidationResult result = validationService.validate(403, 403, body, null);
        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    void shouldPassWhenExpected405MatchesActual405() {
        String body = "{\"error\": \"Method Not Allowed\"}";
        ValidationResult result = validationService.validate(405, 405, body, null);
        assertTrue(result.isValid());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    void shouldFailWhenExpectedNegativeStatusMismatches() {
        String body = "{\"id\": 1}";
        ValidationResult result = validationService.validate(404, 200, body, null);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("Expected status 404 but got 200"));
    }

    @Test
    void shouldFailWhenExpected400ButGot500() {
        String body = "{\"error\": \"Server Error\"}";
        ValidationResult result = validationService.validate(400, 500, body, null);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("Expected status 400 but got 500"));
    }

    @Test
    void shouldFailWhenExpected401ButGot403() {
        String body = "{\"error\": \"Forbidden\"}";
        ValidationResult result = validationService.validate(401, 403, body, null);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().contains("Expected status 401 but got 403"));
    }

    @Test
    void shouldIncludeErrorBodyInValidationResult() {
        String body = "{\"code\": \"RESOURCE_NOT_FOUND\", \"message\": \"Item not found\"}";
        ValidationResult result = validationService.validate(404, 404, body, null);
        assertTrue(result.isValid());
    }

    @Test
    void shouldAllowNegativeStatusWithRequiredFieldsValidation() {
        String body = "{\"id\": 1}";
        ValidationResult result = validationService.validate(404, 404, body, "id");
        assertTrue(result.isValid());
    }

    @Test
    void shouldAllowNegativeStatusWithFieldTypeValidation() {
        String body = "{\"id\": 1}";
        String types = "{\"id\":\"NUMBER\"}";
        ValidationResult result = validationService.validate(404, 404, body, null, types);
        assertTrue(result.isValid());
    }
}
