package semo.back.service.feature.schedule.vo;

public record ScheduleAttendanceEventSummaryResponse(
        Long eventId,
        String title,
        String dateLabel,
        String timeLabel,
        String participationStatus,
        String attendanceStatus,
        String checkedInAtLabel,
        int goingCount,
        int attendedCount
) {
}
