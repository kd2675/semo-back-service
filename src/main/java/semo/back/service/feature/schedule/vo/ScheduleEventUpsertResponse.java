package semo.back.service.feature.schedule.vo;

public record ScheduleEventUpsertResponse(
        Long eventId,
        Long linkedNoticeId,
        String title,
        String startDate,
        String endDate,
        String dateLabel,
        String timeLabel,
        boolean postedToBoard,
        boolean postedToCalendar,
        boolean pinned
) {
}
