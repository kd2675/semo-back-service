package semo.back.service.feature.todo.vo;

public record TodoChecklistItemResponse(
        Long todoChecklistItemId,
        String content,
        int sortOrder,
        boolean completed,
        Long completedByClubProfileId,
        String completedByDisplayName,
        String completedAt
) {
}
