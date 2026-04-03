package semo.back.service.feature.todo.vo;

public record TodoSummaryResponse(
        Long todoItemId,
        String title,
        String description,
        String todoType,
        String todoTypeLabel,
        String assignmentMode,
        String assignmentModeLabel,
        String statusCode,
        String statusLabel,
        String dueAt,
        String dueAtLabel,
        boolean overdue,
        Long assignedClubProfileId,
        String assignedDisplayName,
        String createdByDisplayName,
        String completedByDisplayName,
        String completedAt,
        String completedAtLabel,
        boolean canClaim,
        boolean canComplete,
        boolean canEdit,
        boolean canManageStatus
) {
}
