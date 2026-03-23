package semo.back.service.feature.attendance.vo;

public record AttendanceTodayResponse(
        String attendanceDateLabel,
        boolean checkedIn,
        String checkedInAtLabel,
        boolean canCheckIn,
        int checkedInCount,
        int memberCount
) {
}
