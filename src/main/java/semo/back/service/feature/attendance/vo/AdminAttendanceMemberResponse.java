package semo.back.service.feature.attendance.vo;

public record AdminAttendanceMemberResponse(
        Long clubProfileId,
        String displayName,
        String roleCode,
        boolean checkedIn,
        String checkedInAtLabel
) {
}
