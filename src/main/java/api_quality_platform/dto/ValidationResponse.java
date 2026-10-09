package api_quality_platform.dto;

import java.util.ArrayList;
import java.util.List;

public class ValidationResponse {

    private final boolean valid;
    private final List<String> errors;

    public ValidationResponse(boolean valid, List<String> errors) {
        this.valid = valid;
        this.errors = errors;
    }

    public static ValidationResponse success() {
        return new ValidationResponse(true, List.of());
    }

    public static ValidationResponse failure(List<String> errors) {
        return new ValidationResponse(false, new ArrayList<>(errors));
    }

    public boolean isValid() {
        return valid;
    }

    public List<String> getErrors() {
        return errors;
    }
}
