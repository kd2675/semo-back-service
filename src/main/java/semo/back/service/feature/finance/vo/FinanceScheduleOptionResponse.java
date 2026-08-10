package semo.back.service.feature.finance.vo;

public record FinanceScheduleOptionResponse(
        Long eventId,
        String title,
        String startAt,
        String startAtLabel
) {
}
