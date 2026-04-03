package semo.back.service.feature.todo.vo;

public record TodoActionResponse(
        Long todoItemId,
        String statusCode,
        String statusLabel,
        Long assignedClubProfileId,
        String assignedDisplayName,
        Long completedByClubProfileId,
        String completedByDisplayName,
        String completedAt,
        String completedAtLabel
) {
}
