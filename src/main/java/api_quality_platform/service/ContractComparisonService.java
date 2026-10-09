package api_quality_platform.service;

import api_quality_platform.dto.ContractComparisonResult;
import api_quality_platform.entity.ContractComparisonHistory;
import api_quality_platform.entity.TestExecution;
import api_quality_platform.repository.ContractComparisonHistoryRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Service
public class ContractComparisonService {

    private final ObjectMapper objectMapper;
    private final ContractComparisonHistoryRepository historyRepository;

    @Autowired
    public ContractComparisonService(
            ObjectMapper objectMapper,
            ContractComparisonHistoryRepository historyRepository) {

        this.objectMapper = objectMapper;
        this.historyRepository = historyRepository;
    }

    public ContractComparisonService(ObjectMapper objectMapper) {
        this(objectMapper, null);
    }

    public Set<String> getFieldNames(String responseBody) {

        if (responseBody == null || responseBody.isBlank()) {
            return Set.of();
        }

        try {
            JsonNode root = objectMapper.readTree(responseBody);

            if (root.isArray() && !root.isEmpty()) {
                root = root.get(0);
            }

            Set<String> fields = new HashSet<>();

            collectFieldNames(root, "", fields);

            return fields;

        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(
                    "Invalid JSON response body: " + e.getOriginalMessage(), e);
        }
    }

    private void collectFieldNames(
            JsonNode node,
            String path,
            Set<String> fields) {

        if (node.isObject()) {

            node.fieldNames().forEachRemaining(field -> {

                String currentPath =
                        path.isEmpty()
                                ? field
                                : path + "." + field;

                fields.add(currentPath);

                JsonNode child = node.get(field);

                if (child != null && !child.isNull()) {
                    collectFieldNames(child, currentPath, fields);
                }
            });
        } else if (node.isArray() && !node.isEmpty()) {
            JsonNode firstElement = node.get(0);
            String arrayPath = path.isEmpty() ? path : path + "[]";
            collectFieldNames(firstElement, arrayPath, fields);
        }
    }

    private String toJsonPointer(String field) {
        StringBuilder pointer = new StringBuilder();
        String[] parts = field.split("\\.");
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

    private Set<String> getTypeChangedFields(
            String previousResponse,
            String currentResponse) {

        if (previousResponse == null || previousResponse.isBlank()) {
            return Set.of();
        }

        if (currentResponse == null || currentResponse.isBlank()) {
            return Set.of();
        }

        try {
            JsonNode previousRoot =
                    objectMapper.readTree(previousResponse);

            JsonNode currentRoot =
                    objectMapper.readTree(currentResponse);

            if (previousRoot.isArray() && !previousRoot.isEmpty()) {
                previousRoot = previousRoot.get(0);
            }

            if (currentRoot.isArray() && !currentRoot.isEmpty()) {
                currentRoot = currentRoot.get(0);
            }

            Set<String> typeChangedFields = new HashSet<>();

            Set<String> previousFields = getFieldNames(previousResponse);
            Set<String> currentFields = getFieldNames(currentResponse);

            Set<String> commonFields = new HashSet<>(previousFields);
            commonFields.retainAll(currentFields);

            for (String field : commonFields) {

                JsonNode previousValue =
                        previousRoot.at(toJsonPointer(field));

                JsonNode currentValue =
                        currentRoot.at(toJsonPointer(field));

                if (!previousValue.isMissingNode() &&
                        !currentValue.isMissingNode() &&
                        !previousValue.isNull() &&
                        !currentValue.isNull() &&
                        !previousValue.getNodeType()
                                .equals(currentValue.getNodeType())) {

                    typeChangedFields.add(field);
                }
            }

            return typeChangedFields;

        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(
                    "Invalid JSON response body: " + e.getOriginalMessage(), e);
        }
    }

    public ContractComparisonResult compareContracts(
            String previousResponse,
            String currentResponse) {

        Set<String> previousFields =
                getFieldNames(previousResponse);

        Set<String> currentFields =
                getFieldNames(currentResponse);

        Set<String> addedFields =
                new HashSet<>(currentFields);

        addedFields.removeAll(previousFields);

        Set<String> removedFields =
                new HashSet<>(previousFields);

        removedFields.removeAll(currentFields);

        Set<String> typeChangedFields =
                getTypeChangedFields(
                        previousResponse,
                        currentResponse
                );

        boolean contractChanged =
                !addedFields.isEmpty() ||
                        !removedFields.isEmpty() ||
                        !typeChangedFields.isEmpty();

        return new ContractComparisonResult(
                contractChanged,
                addedFields,
                removedFields,
                typeChangedFields
        );
    }

    public ContractComparisonResult compareExecutions(
            TestExecution previousExecution,
            TestExecution currentExecution) {

        ContractComparisonResult result = compareContracts(
                previousExecution.getResponseBody(),
                currentExecution.getResponseBody()
        );

        saveHistory(
                previousExecution.getId(),
                currentExecution.getId(),
                result
        );

        return result;
    }

    public ContractComparisonResult compareAndReturn(
            TestExecution previousExecution,
            TestExecution currentExecution) {

        return compareContracts(
                previousExecution.getResponseBody(),
                currentExecution.getResponseBody()
        );
    }

    public void saveHistory(
            Long previousExecutionId,
            Long currentExecutionId,
            ContractComparisonResult result) {

        if (historyRepository == null) {
            return;
        }

        ContractComparisonHistory history = new ContractComparisonHistory(
                previousExecutionId,
                currentExecutionId,
                result.isContractChanged(),
                String.join(",", result.getAddedFields()),
                String.join(",", result.getRemovedFields()),
                String.join(",", result.getTypeChangedFields()),
                LocalDateTime.now()
        );

        historyRepository.save(history);
    }
}
