package semo.back.service.feature.tournament.vo;

public record TournamentRosterOptionResponse(
        Long clubProfileId,
        String displayName,
        String avatarImageUrl,
        String avatarThumbnailUrl
) {
}
