package semo.back.service.common.exception;

import lombok.Getter;

@Getter
public class SemoException extends RuntimeException {
    private final String code;
    private final int status;

    public SemoException(String code, String message, int status) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public SemoException(String code, String message) {
        super(message);
        this.code = code;
        this.status = 400;
    }

    // 자주 사용되는 예외들
    public static class ResourceNotFoundException extends SemoException {
        public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
            super(
                    "RESOURCE_NOT_FOUND",
                    String.format("%s not found with %s: '%s'", resourceName, fieldName, fieldValue),
                    404
            );
        }
    }

    public static class ValidationException extends SemoException {
        public ValidationException(String message) {
            super("VALIDATION_ERROR", message, 400);
        }
    }

    public static class UnauthorizedException extends SemoException {
        public UnauthorizedException(String message) {
            super("UNAUTHORIZED", message, 401);
        }
    }

    public static class ForbiddenException extends SemoException {
        public ForbiddenException(String message) {
            super("FORBIDDEN", message, 403);
        }
    }

    public static class ConflictException extends SemoException {
        public ConflictException(String message) {
            super("CONFLICT", message, 409);
        }
    }
}
