package semo.back.service.feature.tournament.vo;

public record TournamentEntryMemberResponse(
        Long clubProfileId,
        String displayName,
        String avatarImageUrl,
        String avatarThumbnailUrl,
        String memberRole
) {
}
