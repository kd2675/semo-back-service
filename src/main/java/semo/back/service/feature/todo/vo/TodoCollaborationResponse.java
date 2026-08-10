package semo.back.service.feature.todo.vo;

import java.util.List;

public record TodoCollaborationResponse(
        Long todoItemId,
        String title,
        boolean canManageChecklist,
        boolean canComment,
        int completedChecklistCount,
        int totalChecklistCount,
        List<TodoChecklistItemResponse> checklistItems,
        List<TodoCommentResponse> comments
) {
}
