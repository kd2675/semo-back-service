package semo.back.service.feature.todo.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTodoChecklistItemRequest(
        @NotBlank(message = "체크리스트 내용은 필수입니다.")
        @Size(max = 300, message = "체크리스트 내용은 300자 이하여야 합니다.")
        String content
) {
}
