package semo.back.service.feature.schedule.vo;

import semo.back.service.feature.notice.vo.ClubNoticeSummaryResponse;

public record ClubCalendarFeedItemResponse(
        Long calendarItemId,
        String contentType,
        ClubNoticeSummaryResponse notice,
        ScheduleEventSummaryResponse event,
        ScheduleVoteSummaryResponse vote
) {
}
