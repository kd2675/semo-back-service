package semo.back.service.feature.schedule.vo;

import java.util.List;

public record ScheduleVoteDetailResponse(
        Long clubId,
        String clubName,
        boolean admin,
        Long voteId,
        String title,
        String voteStartDate,
        String voteEndDate,
        String votePeriodLabel,
        String voteStartTime,
        String voteEndTime,
        String voteTimeLabel,
        boolean postedToBoard,
        Long linkedNoticeId,
        Long mySelectedOptionId,
        int totalResponses,
        List<ScheduleVoteOptionSummaryResponse> options,
        boolean canManage,
        boolean votingOpen
) {
}
