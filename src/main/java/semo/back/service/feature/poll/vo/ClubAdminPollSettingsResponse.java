package semo.back.service.feature.poll.vo;

public record ClubAdminPollSettingsResponse(
        Long clubId,
        String clubName,
        boolean admin,
        boolean allowMemberCreate,
        boolean allowMemberUpdate,
        boolean allowMemberDelete
) {
}
