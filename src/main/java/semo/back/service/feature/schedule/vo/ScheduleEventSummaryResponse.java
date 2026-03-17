package semo.back.service.feature.schedule.vo;

public record ScheduleEventSummaryResponse(
        Long eventId,
        String title,
        String startDate,
        String endDate,
        String dateLabel,
        String timeLabel,
        Integer attendeeLimit,
        String locationLabel,
        String participationConditionText,
        boolean participationEnabled,
        boolean feeRequired,
        Integer feeAmount,
        boolean feeAmountUndecided,
        boolean feeNWaySplit,
        boolean postedToBoard,
        Long linkedNoticeId,
        String myParticipationStatus,
        int goingCount,
        int notGoingCount
) {
}
