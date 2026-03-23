package semo.back.service.feature.notice.vo;

public record ClubAdminNoticeSettingsResponse(
        Long clubId,
        String clubName,
        boolean admin,
        boolean roleManagementEnabled,
        boolean allowMemberCreate,
        boolean allowMemberUpdate,
        boolean allowMemberDelete
) {
}
