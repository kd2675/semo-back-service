package semo.back.service.feature.schedule.vo;

import semo.back.service.feature.poll.vo.ClubPollSummaryResponse;

import java.util.List;

public record ClubScheduleVoteSummaryResponse(
        Long clubId,
        String clubName,
        boolean admin,
        boolean canCreate,
        int waitingCount,
        int ongoingCount,
        int closedCount,
        List<ClubPollSummaryResponse> polls
) {
}
