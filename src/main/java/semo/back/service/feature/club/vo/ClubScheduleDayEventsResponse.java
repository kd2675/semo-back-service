package semo.back.service.feature.club.vo;

import java.util.List;

public record ClubScheduleDayEventsResponse(
        int day,
        List<ClubScheduleEventResponse> events
) {
}
