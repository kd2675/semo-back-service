package semo.back.service.feature.schedule.vo;

public record ScheduleEventDetailResponse(
        Long clubId,
        String clubName,
        boolean admin,
        Long eventId,
        String title,
        String description,
        String categoryKey,
        String locationLabel,
        String startAt,
        String startAtLabel,
        String endAt,
        String endAtLabel,
        boolean postedToBoard,
        Long linkedNoticeId,
        boolean canManage
) {
}
