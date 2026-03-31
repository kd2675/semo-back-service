package semo.back.service.feature.schedule.vo;

import semo.back.service.feature.notice.vo.ClubNoticeSummaryResponse;
import semo.back.service.feature.tournament.vo.TournamentSummaryResponse;

public record ClubCalendarFeedItemResponse(
        Long calendarItemId,
        Integer readCount,
        String contentType,
        ClubNoticeSummaryResponse notice,
        ScheduleEventSummaryResponse event,
        ScheduleVoteSummaryResponse vote,
        TournamentSummaryResponse tournament
) {
}
