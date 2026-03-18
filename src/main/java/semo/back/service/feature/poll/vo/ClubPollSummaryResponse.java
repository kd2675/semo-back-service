package semo.back.service.feature.poll.vo;

import semo.back.service.feature.schedule.vo.ScheduleVoteOptionSummaryResponse;

import java.util.List;

public record ClubPollSummaryResponse(
        Long voteId,
        String title,
        String voteStatus,
        String voteStartDate,
        String voteEndDate,
        String votePeriodLabel,
        String voteTimeLabel,
        String voteWindowLabel,
        int totalResponses,
        int optionCount,
        boolean postedToBoard,
        boolean sharedToSchedule,
        boolean canManage,
        Long mySelectedOptionId,
        List<ScheduleVoteOptionSummaryResponse> options
) {
}
