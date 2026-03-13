package semo.back.service.feature.schedule.vo;

public record ScheduleEventUpsertResponse(
        Long eventId,
        Long linkedNoticeId,
        String title,
        String startAt,
        String startAtLabel,
        String endAt,
        String endAtLabel,
        boolean postedToBoard
) {
}
