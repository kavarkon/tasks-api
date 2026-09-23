package api.common;

public record ApiError(int status, String message, String path) {
}
