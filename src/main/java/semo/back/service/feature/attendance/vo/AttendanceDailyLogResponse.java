package semo.back.service.feature.attendance.vo;

public record AttendanceDailyLogResponse(
        String attendanceDateLabel,
        int checkedInCount,
        int memberCount,
        boolean checkedIn,
        String checkedInAtLabel
) {
}
