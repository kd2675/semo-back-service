package semo.back.service.feature.club.vo;

public record ClubScheduleEventResponse(
        String id,
        String icon,
        String title,
        String subtitle,
        String startTime,
        String durationLabel,
        String tone
) {
}
