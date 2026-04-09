package semo.back.service.common.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import web.common.core.response.base.dto.ResponseErrorDTO;
import web.common.core.response.base.vo.Code;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

    @Test
    void handleGeneralException_returnsCommonErrorWrapper() {
        ResponseEntity<ResponseErrorDTO> response = globalExceptionHandler.handleGeneralException(
                new SemoException.ForbiddenException("권한이 없습니다.")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getSuccess()).isFalse();
        assertThat(response.getBody().getCode()).isEqualTo(Code.FORBIDDEN.getCode());
        assertThat(response.getBody().getMessage()).isEqualTo("권한이 없습니다.");
    }

    @Test
    void handleGlobalException_returnsInternalServerErrorWrapper() {
        ResponseEntity<ResponseErrorDTO> response = globalExceptionHandler.handleGlobalException(
                new IllegalStateException("boom")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getSuccess()).isFalse();
        assertThat(response.getBody().getCode()).isEqualTo(Code.INTERNAL_SERVER_ERROR.getCode());
        assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred");
    }
}
