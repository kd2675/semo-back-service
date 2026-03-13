package semo.back.service.feature.attendance.vo;

public record AttendanceSessionResponse(
        Long sessionId,
        String title,
        String attendanceDateLabel,
        String status,
        String openAtLabel,
        String closeAtLabel,
        boolean checkedIn,
        String checkedInAtLabel,
        boolean canCheckIn,
        int checkedInCount,
        int memberCount
) {
}
