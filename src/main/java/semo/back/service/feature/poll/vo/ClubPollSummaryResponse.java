package semo.back.service.feature.poll.vo;

import semo.back.service.feature.schedule.vo.ScheduleVoteOptionSummaryResponse;

import java.util.List;

public record ClubPollSummaryResponse(
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
        String voteWindowLabel,
        int totalResponses,
        int optionCount,
        boolean postedToBoard,
        boolean postedToCalendar,
        boolean sharedToSchedule,
        boolean pinned,
        boolean canEdit,
        boolean canDelete,
        Long mySelectedOptionId,
        List<ScheduleVoteOptionSummaryResponse> options
) {
}
