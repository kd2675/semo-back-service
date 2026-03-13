package semo.back.service.feature.attendance.vo;

public record AttendanceHistoryItemResponse(
        Long sessionId,
        String title,
        String attendanceDateLabel,
        String status,
        boolean checkedIn,
        String checkedInAtLabel
) {
}
