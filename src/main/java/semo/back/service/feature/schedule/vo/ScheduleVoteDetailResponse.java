package semo.back.service.feature.schedule.vo;

import java.util.List;

public record ScheduleVoteDetailResponse(
        Long clubId,
        String clubName,
        boolean admin,
        Long voteId,
        String title,
        String voteStatus,
        String voteStartDate,
        String voteEndDate,
        String votePeriodLabel,
        String voteStartTime,
        String voteEndTime,
        String voteTimeLabel,
        boolean postedToBoard,
        boolean sharedToSchedule,
        Long linkedNoticeId,
        Long mySelectedOptionId,
        int totalResponses,
        List<ScheduleVoteOptionSummaryResponse> options,
        boolean canManage,
        boolean votingOpen
) {
}
