package semo.back.service.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.NoHandlerFoundException;
import web.common.core.response.base.dto.ResponseErrorDTO;
import web.common.core.response.base.exception.GeneralException;
import web.common.core.response.base.vo.Code;

import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(GeneralException.class)
    public ResponseEntity<ResponseErrorDTO> handleGeneralException(GeneralException ex) {
        Code errorCode = ex.getErrorCode();
        log.warn("GeneralException: code={}, status={}, message={}",
                errorCode,
                errorCode.getHttpStatus().value(),
                ex.getMessage());

        ResponseErrorDTO errorResponse = ResponseErrorDTO.of(errorCode, ex.getMessage());
        return new ResponseEntity<>(errorResponse, errorCode.getHttpStatus());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ResponseErrorDTO> handleValidationException(
            MethodArgumentNotValidException ex,
            WebRequest request) {
        BindingResult bindingResult = ex.getBindingResult();
        var fieldErrors = bindingResult.getFieldErrors().stream()
                .map(error -> String.format("%s=%s (%s)",
                        error.getField(),
                        error.getRejectedValue(),
                        error.getDefaultMessage()))
                .collect(Collectors.toList());

        log.warn("Validation failed: path={}, errors={}",
                request.getDescription(false).replace("uri=", ""),
                fieldErrors);

        ResponseErrorDTO errorResponse = ResponseErrorDTO.of(Code.VALIDATION_ERROR, "Validation failed");
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ResponseErrorDTO> handleNoHandlerFoundException(NoHandlerFoundException ex) {
        log.warn("Endpoint not found: method={}, path={}", ex.getHttpMethod(), ex.getRequestURL());

        ResponseErrorDTO errorResponse = ResponseErrorDTO.of(Code.NOT_FOUND, "Endpoint not found");
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseErrorDTO> handleGlobalException(Exception ex) {
        log.error("Unexpected exception occurred", ex);

        ResponseErrorDTO errorResponse = ResponseErrorDTO.of(Code.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
