package api_quality_platform.service;

import api_quality_platform.dto.ApiTestSuggestionResponse;
import api_quality_platform.entity.Api;
import api_quality_platform.exception.NotFoundException;
import api_quality_platform.repository.ApiRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

class TestSuggestionServiceTest {

    private ApiRepository apiRepository;
    private TestSuggestionService testSuggestionService;

    @BeforeEach
    void setUp() {
        apiRepository = Mockito.mock(ApiRepository.class);
        testSuggestionService = new TestSuggestionService(apiRepository);
    }

    @Test
    void shouldGenerateSuggestionsForGetApi() {
        Api api = new Api();
        api.setId(1L);
        api.setName("ShopAPI Products");
        api.setUrl("http://localhost:8081/api/products");
        api.setMethod("GET");
        api.setAuthType("NONE");

        when(apiRepository.findById(1L)).thenReturn(Optional.of(api));

        ApiTestSuggestionResponse response = testSuggestionService.generateSuggestions(1L);

        assertNotNull(response);
        assertEquals(1L, response.getApiId());
        assertEquals("ShopAPI Products", response.getApiName());
        assertFalse(response.getSuggestions().isEmpty());
        assertEquals(200, response.getSuggestions().get(0).getTestRequest().getExpectedStatus());
    }

    @Test
    void shouldGenerateSuggestionsForPostApi() {
        Api api = new Api();
        api.setId(2L);
        api.setName("Create Item API");
        api.setUrl("http://localhost:8081/api/items");
        api.setMethod("POST");
        api.setAuthType("NONE");

        when(apiRepository.findById(2L)).thenReturn(Optional.of(api));

        ApiTestSuggestionResponse response = testSuggestionService.generateSuggestions(2L);

        assertNotNull(response);
        assertEquals(2L, response.getApiId());
        assertFalse(response.getSuggestions().isEmpty());
        assertEquals(201, response.getSuggestions().get(0).getTestRequest().getExpectedStatus());
    }

    @Test
    void shouldThrowNotFoundExceptionForUnknownApiId() {
        when(apiRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> {
            testSuggestionService.generateSuggestions(999L);
        });
    }
}
