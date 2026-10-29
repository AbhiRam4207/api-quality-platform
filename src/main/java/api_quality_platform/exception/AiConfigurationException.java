package api_quality_platform.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class AiConfigurationException extends RuntimeException {
    public AiConfigurationException(String message) {
        super(message);
    }
}
