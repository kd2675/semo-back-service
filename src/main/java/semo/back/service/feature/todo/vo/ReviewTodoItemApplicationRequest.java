package semo.back.service.feature.todo.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReviewTodoItemApplicationRequest(
        @NotBlank(message = "신청 상태는 필수입니다.")
        String applicationStatus,
        @Size(max = 500, message = "검토 메모는 500자 이하여야 합니다.")
        String reviewNote
) {
}
