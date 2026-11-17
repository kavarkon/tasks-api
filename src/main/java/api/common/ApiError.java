package api.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(int status, String message, String path, Map<String, String> fields) {

    public ApiError(int status, String message, String path) {
        this(status, message, path, null);
    }
}
