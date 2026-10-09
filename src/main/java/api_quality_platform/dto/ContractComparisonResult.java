package api_quality_platform.dto;

import java.util.Set;

public class ContractComparisonResult {

    private final boolean contractChanged;
    private final Set<String> addedFields;
    private final Set<String> removedFields;
    private final Set<String> typeChangedFields;

    public ContractComparisonResult(
            boolean contractChanged,
            Set<String> addedFields,
            Set<String> removedFields,
            Set<String> typeChangedFields) {

        this.contractChanged = contractChanged;
        this.addedFields = addedFields;
        this.removedFields = removedFields;
        this.typeChangedFields = typeChangedFields;
    }
    public boolean isContractChanged() {
        return contractChanged;
    }

    public Set<String> getAddedFields() {
        return addedFields;
    }

    public Set<String> getRemovedFields() {
        return removedFields;
    }
    public Set<String> getTypeChangedFields() {
        return typeChangedFields;
    }
}