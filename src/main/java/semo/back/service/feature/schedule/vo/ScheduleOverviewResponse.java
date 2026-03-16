package semo.back.service.feature.schedule.vo;

public record ScheduleOverviewResponse(
        int upcomingEventCount,
        int recentEventCount,
        int voteCount,
        int boardPostedEventCount,
        int boardPostedVoteCount,
        int pendingAttendanceCount,
        int pendingVoteCount
) {
}
