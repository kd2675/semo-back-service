package semo.back.service.feature.tournament.vo;

public record TournamentApplicationSummaryResponse(
        Long tournamentApplicationId,
        Long clubProfileId,
        String applicantDisplayName,
        String applicantAvatarImageUrl,
        String applicantAvatarThumbnailUrl,
        String applicationStatus,
        String applicationNote,
        String appliedAtLabel,
        boolean mine,
        boolean canReview,
        boolean canCancel
) {
}
