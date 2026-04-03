package semo.back.service.feature.todo.vo;

import java.util.List;

public record TodoItemApplicationsResponse(
        Long todoItemId,
        String title,
        String assignmentMode,
        String assignmentModeLabel,
        String statusCode,
        String statusLabel,
        Long assignedClubProfileId,
        String assignedDisplayName,
        int applicationCount,
        int pendingApplicationCount,
        boolean canReview,
        List<TodoItemApplicationResponse> applications
) {
}
