package semo.back.service.feature.schedule.vo;

import java.util.List;

public record ScheduleEventAttendanceResponse(
        Long clubId,
        String clubName,
        Long eventId,
        String eventTitle,
        String dateLabel,
        String timeLabel,
        boolean canManage,
        ScheduleEventAttendanceSummaryResponse summary,
        List<ScheduleEventAttendanceMemberResponse> members
) {
}
