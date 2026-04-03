package semo.back.service.feature.todo.vo;

import jakarta.validation.constraints.Size;

public record CreateTodoApplicationRequest(
        @Size(max = 500, message = "신청 메모는 500자 이하여야 합니다.")
        String applicationNote
) {
}
