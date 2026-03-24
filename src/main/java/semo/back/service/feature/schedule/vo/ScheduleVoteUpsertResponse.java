package semo.back.service.feature.schedule.vo;

public record ScheduleVoteUpsertResponse(
        Long voteId,
        Long linkedNoticeId,
        String title,
        String voteStartDate,
        String voteEndDate,
        String votePeriodLabel,
        String voteStartTime,
        String voteEndTime,
        String voteTimeLabel,
        int optionCount,
        boolean postedToBoard,
        boolean postedToCalendar,
        boolean sharedToSchedule,
        boolean pinned
) {
}
