package api_quality_platform.service;

import api_quality_platform.dto.ContractComparisonResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ContractComparisonServiceTest {

    private final ContractComparisonService service =
            new ContractComparisonService(
                    new com.fasterxml.jackson.databind.ObjectMapper()
            );

    @Test
    void shouldDetectTypeChangeAtTopLevel() {
        String previous = "{\"price\": 499.0}";
        String current = "{\"price\": \"499.0\"}";

        ContractComparisonResult result = service.compareContracts(previous, current);

        assertTrue(result.isContractChanged());
        assertTrue(result.getTypeChangedFields().contains("price"));
        assertTrue(result.getAddedFields().isEmpty());
        assertTrue(result.getRemovedFields().isEmpty());
    }

    @Test
    void shouldDetectNestedTypeChange() {
        String previous = """
                {
                    "id": 1,
                    "customer": {
                        "name": "Abhiram",
                        "email": "abc@test.com"
                    }
                }
                """;

        String current = """
                {
                    "id": 1,
                    "customer": {
                        "name": "Abhiram",
                        "email": true
                    }
                }
                """;

        ContractComparisonResult result = service.compareContracts(previous, current);

        assertTrue(result.isContractChanged());
        assertTrue(result.getTypeChangedFields().contains("customer.email"));
    }

    @Test
    void shouldDetectTypeChangeInArray() {
        String previous = """
                {
                    "products": [
                        {
                            "id": 1,
                            "name": "Widget"
                        }
                    ]
                }
                """;

        String current = """
                {
                    "products": [
                        {
                            "id": "1",
                            "name": "Widget"
                        }
                    ]
                }
                """;

        ContractComparisonResult result = service.compareContracts(previous, current);

        assertTrue(result.isContractChanged());
        assertTrue(result.getTypeChangedFields().contains("products[].id"));
    }

    @Test
    void shouldDetectAddedFieldInArray() {
        String previous = """
                {
                    "products": [
                        {
                            "id": 1
                        }
                    ]
                }
                """;

        String current = """
                {
                    "products": [
                        {
                            "id": 1,
                            "name": "Widget"
                        }
                    ]
                }
                """;

        ContractComparisonResult result = service.compareContracts(previous, current);

        assertTrue(result.isContractChanged());
        assertTrue(result.getAddedFields().contains("products[].name"));
    }

    @Test
    void shouldDetectRemovedFieldInArray() {
        String previous = """
                {
                    "products": [
                        {
                            "id": 1,
                            "name": "Widget"
                        }
                    ]
                }
                """;

        String current = """
                {
                    "products": [
                        {
                            "id": 1
                        }
                    ]
                }
                """;

        ContractComparisonResult result = service.compareContracts(previous, current);

        assertTrue(result.isContractChanged());
        assertTrue(result.getRemovedFields().contains("products[].name"));
    }

    @Test
    void shouldDetectNestedArrayFieldChange() {
        String previous = """
                {
                    "order": {
                        "items": [
                            {
                                "product": {
                                    "id": 1
                                }
                            }
                        ]
                    }
                }
                """;

        String current = """
                {
                    "order": {
                        "items": [
                            {
                                "product": {
                                    "id": 1,
                                    "price": 99.0
                                }
                            }
                        ]
                    }
                }
                """;

        ContractComparisonResult result = service.compareContracts(previous, current);

        assertTrue(result.isContractChanged());
        assertTrue(result.getAddedFields().contains("order.items[].product.price"));
    }

    @Test
    void shouldHandleEmptyArraysSafely() {
        String previous = "{\"products\": []}";
        String current = "{\"products\": []}";

        ContractComparisonResult result = service.compareContracts(previous, current);

        assertFalse(result.isContractChanged());
        assertTrue(result.getAddedFields().isEmpty());
        assertTrue(result.getRemovedFields().isEmpty());
        assertTrue(result.getTypeChangedFields().isEmpty());
    }

    @Test
    void shouldHandleNullValueSafely() {
        String previous = "{\"discount\": null}";
        String current = "{\"discount\": null}";

        ContractComparisonResult result = service.compareContracts(previous, current);

        assertFalse(result.isContractChanged());
    }

    @Test
    void shouldDetectChangeFromNullToObject() {
        String previous = "{\"customer\": null}";
        String current = "{\"customer\": {\"name\": \"Abhiram\"}}";

        ContractComparisonResult result = service.compareContracts(previous, current);

        assertTrue(result.isContractChanged());
        assertTrue(result.getAddedFields().contains("customer.name"));
    }

    @Test
    void shouldDetectChangeFromEmptyArrayToPopulatedArray() {
        String previous = "{\"products\": []}";
        String current = """
                {
                    "products": [
                        {
                            "id": 1
                        }
                    ]
                }
                """;

        ContractComparisonResult result = service.compareContracts(previous, current);

        assertTrue(result.isContractChanged());
        assertTrue(result.getAddedFields().contains("products[].id"));
    }

    @Test
    void shouldReturnNoChangeForIdenticalResponses() {
        String response = """
                {
                    "id": 1,
                    "name": "Widget",
                    "price": 99.0
                }
                """;

        ContractComparisonResult result = service.compareContracts(response, response);

        assertFalse(result.isContractChanged());
        assertTrue(result.getAddedFields().isEmpty());
        assertTrue(result.getRemovedFields().isEmpty());
        assertTrue(result.getTypeChangedFields().isEmpty());
    }

    @Test
    void shouldThrowOnInvalidJson() {
        String invalidJson = "{invalid json";

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.getFieldNames(invalidJson)
        );

        assertTrue(exception.getMessage().startsWith("Invalid JSON response body"));
    }

    @Test
    void shouldHandleMalformedJsonGracefully() {
        String previous = "{\"price\": 100}";
        String current = "{malformed";

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.compareContracts(previous, current)
        );

        assertTrue(exception.getMessage().startsWith("Invalid JSON response body"));
    }

    @Test
    void shouldHandleNullInputGracefully() {
        ContractComparisonResult result = service.compareContracts(null, null);

        assertFalse(result.isContractChanged());
        assertTrue(result.getAddedFields().isEmpty());
        assertTrue(result.getRemovedFields().isEmpty());
        assertTrue(result.getTypeChangedFields().isEmpty());
    }

    @Test
    void shouldHandleBlankInputGracefully() {
        ContractComparisonResult result = service.compareContracts("", "");

        assertFalse(result.isContractChanged());
        assertTrue(result.getAddedFields().isEmpty());
        assertTrue(result.getRemovedFields().isEmpty());
        assertTrue(result.getTypeChangedFields().isEmpty());
    }

    @Test
    void shouldDetectAddedTopLevelField() {
        String previous = "{\"id\": 1}";
        String current = "{\"id\": 1, \"name\": \"Widget\"}";

        ContractComparisonResult result = service.compareContracts(previous, current);

        assertTrue(result.isContractChanged());
        assertTrue(result.getAddedFields().contains("name"));
    }

    @Test
    void shouldDetectRemovedTopLevelField() {
        String previous = "{\"id\": 1, \"name\": \"Widget\"}";
        String current = "{\"id\": 1}";

        ContractComparisonResult result = service.compareContracts(previous, current);

        assertTrue(result.isContractChanged());
        assertTrue(result.getRemovedFields().contains("name"));
    }

    @Test
    void shouldDetectAddedNestedField() {
        String previous = """
                {
                    "customer": {
                        "name": "Abhiram"
                    }
                }
                """;

        String current = """
                {
                    "customer": {
                        "name": "Abhiram",
                        "email": "abc@test.com"
                    }
                }
                """;

        ContractComparisonResult result = service.compareContracts(previous, current);

        assertTrue(result.isContractChanged());
        assertTrue(result.getAddedFields().contains("customer.email"));
    }

    @Test
    void shouldDetectRemovedNestedField() {
        String previous = """
                {
                    "customer": {
                        "name": "Abhiram",
                        "email": "abc@test.com"
                    }
                }
                """;

        String current = """
                {
                    "customer": {
                        "name": "Abhiram"
                    }
                }
                """;

        ContractComparisonResult result = service.compareContracts(previous, current);

        assertTrue(result.isContractChanged());
        assertTrue(result.getRemovedFields().contains("customer.email"));
    }
}
