package semo.back.service.feature.schedule.vo;

import java.util.List;

public record ClubScheduleAttendanceSummaryResponse(
        Long clubId,
        String clubName,
        boolean enabled,
        ScheduleAttendanceEventSummaryResponse nextEvent,
        List<ScheduleAttendanceEventSummaryResponse> recentEvents
) {
}
