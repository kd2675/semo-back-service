package semo.back.service.feature.schedule.vo;

public record ScheduleVoteOptionSummaryResponse(
        Long voteOptionId,
        String label,
        int sortOrder,
        int voteCount
) {
}
