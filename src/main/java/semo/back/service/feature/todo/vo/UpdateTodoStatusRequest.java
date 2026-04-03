package semo.back.service.feature.todo.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateTodoStatusRequest(
        @NotBlank(message = "변경할 상태는 필수입니다.")
        @Size(max = 20, message = "상태 코드는 20자 이하여야 합니다.")
        String statusCode
) {
}
