package api_quality_platform.controller;

import api_quality_platform.dto.ContractComparisonHistoryResponse;
import api_quality_platform.dto.ContractComparisonResult;
import api_quality_platform.entity.TestExecution;
import api_quality_platform.repository.ContractComparisonHistoryRepository;
import api_quality_platform.service.ContractComparisonService;
import api_quality_platform.service.TestExecutionService;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/contracts")
public class ContractComparisonController {

    private final ContractComparisonService contractComparisonService;
    private final TestExecutionService testExecutionService;
    private final ContractComparisonHistoryRepository historyRepository;

    public ContractComparisonController(
            ContractComparisonService contractComparisonService,
            TestExecutionService testExecutionService,
            ContractComparisonHistoryRepository historyRepository) {

        this.contractComparisonService = contractComparisonService;
        this.testExecutionService = testExecutionService;
        this.historyRepository = historyRepository;
    }

    @GetMapping("/compare/{previousExecutionId}/{currentExecutionId}")
    public ContractComparisonResult compare(
            @PathVariable Long previousExecutionId,
            @PathVariable Long currentExecutionId) {

        TestExecution previousExecution =
                testExecutionService.getExecutionById(previousExecutionId);

        TestExecution currentExecution =
                testExecutionService.getExecutionById(currentExecutionId);

        ContractComparisonResult result = contractComparisonService.compareExecutions(
                previousExecution,
                currentExecution
        );

        return result;
    }

    @GetMapping("/history")
    public List<ContractComparisonHistoryResponse> getHistory() {
        return historyRepository.findAll()
                .stream()
                .map(history -> {
                    ContractComparisonHistoryResponse response = new ContractComparisonHistoryResponse();
                    response.setId(history.getId());
                    response.setPreviousExecutionId(history.getPreviousExecutionId());
                    response.setCurrentExecutionId(history.getCurrentExecutionId());
                    response.setContractChanged(history.isContractChanged());
                    response.setAddedFields(split(history.getAddedFields()));
                    response.setRemovedFields(split(history.getRemovedFields()));
                    response.setTypeChangedFields(split(history.getTypeChangedFields()));
                    response.setComparedAt(history.getComparedAt());
                    return response;
                })
                .toList();
    }

    private Set<String> split(String value) {
        if (value == null || value.isBlank()) {
            return Set.of();
        }
        String[] parts = value.split(",");
        Set<String> result = new java.util.HashSet<>();
        for (String part : parts) {
            if (!part.isBlank()) {
                result.add(part);
            }
        }
        return result;
    }
}