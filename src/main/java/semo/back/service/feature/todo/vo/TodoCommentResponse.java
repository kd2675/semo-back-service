package semo.back.service.feature.todo.vo;

public record TodoCommentResponse(
        Long todoCommentId,
        Long authorClubProfileId,
        String authorDisplayName,
        String content,
        String createdAt,
        boolean mine,
        boolean canDelete
) {
}
