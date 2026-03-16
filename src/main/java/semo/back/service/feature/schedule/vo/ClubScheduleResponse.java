package semo.back.service.feature.schedule.vo;

import java.util.List;

public record ClubScheduleResponse(
        Long clubId,
        String clubName,
        boolean admin,
        int calendarYear,
        int calendarMonth,
        ScheduleOverviewResponse overview,
        List<ScheduleEventSummaryResponse> monthEvents,
        List<ScheduleVoteSummaryResponse> votes
) {
}
