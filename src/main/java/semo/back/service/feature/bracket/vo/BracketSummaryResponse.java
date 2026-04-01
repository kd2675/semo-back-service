package semo.back.service.feature.bracket.vo;

public record BracketSummaryResponse(
        Long bracketRecordId,
        String title,
        String summaryText,
        String approvalStatus,
        String bracketType,
        String participantType,
        String sourceType,
        Long sourceTournamentRecordId,
        String sourceTournamentTitle,
        String authorDisplayName,
        String authorAvatarImageUrl,
        String authorAvatarThumbnailUrl,
        String reviewedByDisplayName,
        String reviewedAtLabel,
        String rejectionReason,
        int participantCount,
        boolean mine,
        boolean canEdit,
        boolean canSubmit,
        boolean canDelete
) {
}
