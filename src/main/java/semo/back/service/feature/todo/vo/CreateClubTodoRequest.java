package semo.back.service.feature.todo.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateClubTodoRequest(
        @NotBlank(message = "업무 이름은 필수입니다.")
        @Size(max = 150, message = "업무 이름은 150자 이하여야 합니다.")
        String title,
        @Size(max = 2000, message = "업무 설명은 2000자 이하여야 합니다.")
        String description,
        @NotBlank(message = "업무 유형은 필수입니다.")
        String todoType,
        @NotBlank(message = "배정 방식은 필수입니다.")
        String assignmentMode,
        Long assignedClubProfileId,
        String dueAt
) {
}
