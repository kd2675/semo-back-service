package semo.back.service.feature.notice.vo;

import semo.back.service.feature.schedule.vo.ScheduleEventSummaryResponse;
import semo.back.service.feature.schedule.vo.ScheduleVoteSummaryResponse;
import semo.back.service.feature.tournament.vo.TournamentSummaryResponse;

public record ClubBoardFeedItemResponse(
        Long boardItemId,
        String contentType,
        ClubNoticeSummaryResponse notice,
        ScheduleEventSummaryResponse event,
        ScheduleVoteSummaryResponse vote,
        TournamentSummaryResponse tournament
) {
}
