package semo.back.service.feature.schedule.vo;

import java.util.List;

public record ClubScheduleHomeResponse(
        Long clubId,
        String clubName,
        boolean admin,
        boolean canCreate,
        int totalEventCount,
        int upcomingEventCount,
        int manageableItemCount,
        List<ScheduleEventSummaryResponse> events
) {
}
