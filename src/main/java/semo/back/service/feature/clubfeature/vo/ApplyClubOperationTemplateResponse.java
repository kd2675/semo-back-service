package semo.back.service.feature.clubfeature.vo;

public record ApplyClubOperationTemplateResponse(
        String templateKey,
        String displayName,
        Long todoItemId,
        int checklistItemCount,
        String dueAt,
        String targetPath
) {
}
