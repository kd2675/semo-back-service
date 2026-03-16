package semo.back.service.feature.schedule.vo;

import java.util.List;

public record ScheduleVoteSummaryResponse(
        Long voteId,
        String title,
        String voteStartDate,
        String voteEndDate,
        String votePeriodLabel,
        String voteTimeLabel,
        int optionCount,
        int totalResponses,
        boolean postedToBoard,
        Long linkedNoticeId,
        Long mySelectedOptionId,
        List<ScheduleVoteOptionSummaryResponse> options,
        boolean votingOpen
) {
}
