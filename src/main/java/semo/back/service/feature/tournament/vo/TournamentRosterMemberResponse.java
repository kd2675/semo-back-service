package semo.back.service.feature.tournament.vo;

public record TournamentRosterMemberResponse(
        Long clubProfileId,
        String displayName,
        String avatarImageUrl,
        String avatarThumbnailUrl,
        String rosterRoleCode
) {
}
