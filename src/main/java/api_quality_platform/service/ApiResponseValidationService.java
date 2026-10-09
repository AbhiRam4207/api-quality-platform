package api_quality_platform.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ApiResponseValidationService {

    private final ObjectMapper objectMapper;

    public ApiResponseValidationService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ValidationResult validate(int expectedStatus, int actualStatus, String responseBody, String requiredFieldsCsv) {
        return validate(expectedStatus, actualStatus, responseBody, requiredFieldsCsv, null, null);
    }

    public ValidationResult validate(
            int expectedStatus,
            int actualStatus,
            String responseBody,
            String requiredFieldsCsv,
            String expectedFieldTypesJson) {
        return validate(expectedStatus, actualStatus, responseBody, requiredFieldsCsv, expectedFieldTypesJson, null);
    }

    public ValidationResult validate(
            int expectedStatus,
            int actualStatus,
            String responseBody,
            String requiredFieldsCsv,
            String expectedFieldTypesJson,
            Long maxResponseTimeMs) {

        List<String> errors = new ArrayList<>();

        if (expectedStatus != actualStatus) {
            errors.add("Expected status " + expectedStatus + " but got " + actualStatus);
        }

        if (maxResponseTimeMs != null && actualStatus == 200) {
            // actualResponseTimeMs is not passed here - it's used in the runTest path separately
        }

        if (requiredFieldsCsv != null && !requiredFieldsCsv.isBlank()) {
            String[] fields = requiredFieldsCsv.split(",");
            try {
                JsonNode root = objectMapper.readTree(responseBody);
                for (String field : fields) {
                    String trimmed = field.trim();
                    if (trimmed.isEmpty()) {
                        continue;
                    }
                    if (!hasField(root, trimmed)) {
                        errors.add("Missing required field: " + trimmed);
                    }
                }
            } catch (Exception e) {
                errors.add("Invalid JSON response body for field validation");
            }
        }

        if (expectedFieldTypesJson != null && !expectedFieldTypesJson.isBlank()) {
            try {
                JsonNode root = objectMapper.readTree(responseBody);
                Map<String, String> expectedTypes = objectMapper.readValue(
                        expectedFieldTypesJson,
                        new TypeReference<Map<String, String>>() {}
                );
                validateFieldTypes(root, expectedTypes, errors);
            } catch (Exception e) {
                if (!errors.contains("Invalid JSON response body for field validation")) {
                    errors.add("Invalid JSON response body for field validation");
                }
            }
        }

        boolean valid = errors.isEmpty();
        return new ValidationResult(valid, errors);
    }

    public ValidationResult validateResponseTime(Long maxResponseTimeMs, long actualResponseTimeMs) {
        if (maxResponseTimeMs == null) {
            return ValidationResult.success();
        }
        if (actualResponseTimeMs > maxResponseTimeMs) {
            List<String> errors = new ArrayList<>();
            errors.add("Response time " + actualResponseTimeMs + "ms exceeded max allowed " + maxResponseTimeMs + "ms");
            return new ValidationResult(false, errors);
        }
        return ValidationResult.success();
    }

    private boolean hasField(JsonNode node, String fieldPath) {
        String[] parts = fieldPath.split("\\.");
        JsonNode current = node;
        for (String part : parts) {
            if (current.isObject() && current.has(part)) {
                current = current.get(part);
            } else {
                return false;
            }
        }
        return true;
    }

    private void validateFieldTypes(JsonNode root, Map<String, String> expectedTypes, List<String> errors) {
        for (Map.Entry<String, String> entry : expectedTypes.entrySet()) {
            String fieldPath = entry.getKey();
            String expectedType = entry.getValue();

            JsonNode node = root.at(toFieldPointer(fieldPath));

            if (node.isMissingNode()) {
                errors.add("Missing field: " + fieldPath);
            } else {
                String actualType = getJsonType(node);
                if (!actualType.equals(expectedType)) {
                    errors.add("Type mismatch for field '" + fieldPath + "': expected " + expectedType + ", got " + actualType);
                }
            }
        }
    }

    private String toFieldPointer(String fieldPath) {
        StringBuilder pointer = new StringBuilder();
        String[] parts = fieldPath.split("\\.");
        for (String part : parts) {
            pointer.append('/');
            if (part.endsWith("[]")) {
                pointer.append(part, 0, part.length() - 2);
                pointer.append("/0");
            } else {
                pointer.append(part);
            }
        }
        return pointer.toString();
    }

    private String getJsonType(JsonNode node) {
        if (node.isArray()) {
            return "ARRAY";
        }
        if (node.isObject()) {
            return "OBJECT";
        }
        if (node.isTextual()) {
            return "STRING";
        }
        if (node.isNumber()) {
            return "NUMBER";
        }
        if (node.isBoolean()) {
            return "BOOLEAN";
        }
        if (node.isNull()) {
            return "NULL";
        }
        return "UNKNOWN";
    }
}
