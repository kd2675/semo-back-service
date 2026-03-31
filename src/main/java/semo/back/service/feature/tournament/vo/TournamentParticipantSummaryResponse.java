package semo.back.service.feature.tournament.vo;

public record TournamentParticipantSummaryResponse(
        Long clubProfileId,
        String displayName,
        String avatarImageUrl,
        String avatarThumbnailUrl,
        String approvedAtLabel
) {
}
