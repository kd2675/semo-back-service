package semo.back.service.feature.club.vo;

public record ClubAdminJoinRequestResponse(
        Long clubJoinRequestId,
        Long clubId,
        Long profileId,
        String displayName,
        String tagline,
        String profileColor,
        String requestMessage,
        String requestedAt,
        String requestedAtLabel,
        String requestStatus
) {
}
