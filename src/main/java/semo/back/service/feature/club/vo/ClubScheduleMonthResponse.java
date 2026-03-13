package semo.back.service.feature.club.vo;

import java.util.List;

public record ClubScheduleMonthResponse(
        String id,
        String label,
        String shortLabel,
        int year,
        int month,
        int leadingBlankDays,
        int daysInMonth,
        int defaultSelectedDay,
        List<ClubScheduleDayEventsResponse> days
) {
}
