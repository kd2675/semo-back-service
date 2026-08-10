package semo.back.service.feature.schedule.vo;

public record ScheduleEventAttendanceMemberResponse(
        Long clubProfileId,
        String displayName,
        String roleCode,
        String participationStatus,
        String attendanceStatus,
        String checkedInAtLabel,
        String attendanceNote
) {
}
