package semo.back.service.feature.schedule.vo;

import java.util.List;

public record ScheduleVoteSummaryResponse(
        Long voteId,
        String title,
        String authorDisplayName,
        String authorAvatarImageUrl,
        String authorAvatarThumbnailUrl,
        String voteStatus,
        String voteStartDate,
        String voteEndDate,
        String votePeriodLabel,
        String voteTimeLabel,
        int optionCount,
        int totalResponses,
        boolean postedToBoard,
        boolean postedToCalendar,
        boolean sharedToSchedule,
        Long linkedNoticeId,
        Long mySelectedOptionId,
        List<ScheduleVoteOptionSummaryResponse> options,
        boolean votingOpen,
        boolean canEdit,
        boolean canDelete
) {
}
