package semo.back.service.feature.todo.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

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
        String dueAt,
        List<Long> assignedClubProfileIds,
        String priorityCode,
        Integer recruitmentCapacity,
        String workStartAt,
        String workEndAt,
        Long linkedScheduleEventId
) {
    public CreateClubTodoRequest(
            String title,
            String description,
            String todoType,
            String assignmentMode,
            Long assignedClubProfileId,
            String dueAt
    ) {
        this(
                title,
                description,
                todoType,
                assignmentMode,
                assignedClubProfileId,
                dueAt,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }
}
