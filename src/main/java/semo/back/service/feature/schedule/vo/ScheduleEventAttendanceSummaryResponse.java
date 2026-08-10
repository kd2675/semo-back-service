package semo.back.service.feature.schedule.vo;

public record ScheduleEventAttendanceSummaryResponse(
        int goingCount,
        int presentCount,
        int lateCount,
        int absentCount,
        int excusedCount,
        int unmarkedCount
) {
}
