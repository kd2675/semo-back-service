package semo.back.service.feature.todo.vo;

public record TodoScheduleOptionResponse(
        Long eventId,
        String title,
        String startAt,
        String startAtLabel
) {
}
