package api_quality_platform.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class AiProviderException extends RuntimeException {
    public AiProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
