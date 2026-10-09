package api_quality_platform.dto;

import java.time.LocalDateTime;
import java.util.Set;

public class ContractComparisonHistoryResponse {

    private Long id;
    private Long previousExecutionId;
    private Long currentExecutionId;
    private boolean contractChanged;
    private Set<String> addedFields;
    private Set<String> removedFields;
    private Set<String> typeChangedFields;
    private LocalDateTime comparedAt;

    public ContractComparisonHistoryResponse() {
    }

    public ContractComparisonHistoryResponse(
            Long id,
            Long previousExecutionId,
            Long currentExecutionId,
            boolean contractChanged,
            Set<String> addedFields,
            Set<String> removedFields,
            Set<String> typeChangedFields,
            LocalDateTime comparedAt) {

        this.id = id;
        this.previousExecutionId = previousExecutionId;
        this.currentExecutionId = currentExecutionId;
        this.contractChanged = contractChanged;
        this.addedFields = addedFields;
        this.removedFields = removedFields;
        this.typeChangedFields = typeChangedFields;
        this.comparedAt = comparedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPreviousExecutionId() {
        return previousExecutionId;
    }

    public void setPreviousExecutionId(Long previousExecutionId) {
        this.previousExecutionId = previousExecutionId;
    }

    public Long getCurrentExecutionId() {
        return currentExecutionId;
    }

    public void setCurrentExecutionId(Long currentExecutionId) {
        this.currentExecutionId = currentExecutionId;
    }

    public boolean isContractChanged() {
        return contractChanged;
    }

    public void setContractChanged(boolean contractChanged) {
        this.contractChanged = contractChanged;
    }

    public Set<String> getAddedFields() {
        return addedFields;
    }

    public void setAddedFields(Set<String> addedFields) {
        this.addedFields = addedFields;
    }

    public Set<String> getRemovedFields() {
        return removedFields;
    }

    public void setRemovedFields(Set<String> removedFields) {
        this.removedFields = removedFields;
    }

    public Set<String> getTypeChangedFields() {
        return typeChangedFields;
    }

    public void setTypeChangedFields(Set<String> typeChangedFields) {
        this.typeChangedFields = typeChangedFields;
    }

    public LocalDateTime getComparedAt() {
        return comparedAt;
    }

    public void setComparedAt(LocalDateTime comparedAt) {
        this.comparedAt = comparedAt;
    }
}
