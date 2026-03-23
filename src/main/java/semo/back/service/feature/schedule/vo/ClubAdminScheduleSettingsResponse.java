package semo.back.service.feature.schedule.vo;

public record ClubAdminScheduleSettingsResponse(
        Long clubId,
        String clubName,
        boolean admin,
        boolean roleManagementEnabled,
        boolean allowMemberCreate,
        boolean allowMemberUpdate,
        boolean allowMemberDelete
) {
}
