package semo.back.service.common.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean success;
    private String code;
    private String message;
    private int status;
    private LocalDateTime timestamp;
    private String path;
    private List<FieldError> fieldErrors;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldError {
        private String field;
        private String message;
        private Object rejectedValue;
    }

    public static ErrorResponse of(String code, String message, int status, String path) {
        return ErrorResponse.builder()
                .success(false)
                .code(code)
                .message(message)
                .status(status)
                .timestamp(LocalDateTime.now())
                .path(path)
                .build();
    }

    public static ErrorResponse of(String code, String message, int status) {
        return ErrorResponse.builder()
                .success(false)
                .code(code)
                .message(message)
                .status(status)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
