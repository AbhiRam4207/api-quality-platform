package api_quality_platform.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "contract_comparison_history")
public class ContractComparisonHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "previous_execution_id", nullable = false)
    private Long previousExecutionId;

    @Column(name = "current_execution_id", nullable = false)
    private Long currentExecutionId;

    @Column(name = "contract_changed", nullable = false)
    private boolean contractChanged;

    @Column(name = "added_fields", columnDefinition = "TEXT")
    private String addedFields;

    @Column(name = "removed_fields", columnDefinition = "TEXT")
    private String removedFields;

    @Column(name = "type_changed_fields", columnDefinition = "TEXT")
    private String typeChangedFields;

    @Column(name = "compared_at", nullable = false)
    private LocalDateTime comparedAt;

    public ContractComparisonHistory() {
    }

    public ContractComparisonHistory(
            Long previousExecutionId,
            Long currentExecutionId,
            boolean contractChanged,
            String addedFields,
            String removedFields,
            String typeChangedFields,
            LocalDateTime comparedAt) {

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

    public String getAddedFields() {
        return addedFields;
    }

    public void setAddedFields(String addedFields) {
        this.addedFields = addedFields;
    }

    public String getRemovedFields() {
        return removedFields;
    }

    public void setRemovedFields(String removedFields) {
        this.removedFields = removedFields;
    }

    public String getTypeChangedFields() {
        return typeChangedFields;
    }

    public void setTypeChangedFields(String typeChangedFields) {
        this.typeChangedFields = typeChangedFields;
    }

    public LocalDateTime getComparedAt() {
        return comparedAt;
    }

    public void setComparedAt(LocalDateTime comparedAt) {
        this.comparedAt = comparedAt;
    }
}
