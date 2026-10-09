package api_quality_platform.service;

import api_quality_platform.repository.ContractComparisonHistoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class ContractComparisonHistoryPersistenceTest {

    @Autowired
    private ContractComparisonService contractComparisonService;

    @Autowired
    private ContractComparisonHistoryRepository historyRepository;

    @Test
    void shouldWireHistoryRepository() {
        assertNotNull(historyRepository);
        assertNotNull(contractComparisonService);
    }
}
