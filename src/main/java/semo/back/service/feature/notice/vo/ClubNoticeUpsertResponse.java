package semo.back.service.feature.notice.vo;

public record ClubNoticeUpsertResponse(
        Long noticeId,
        String title,
        String categoryKey,
        String scheduleAt,
        String scheduleAtLabel,
        String locationLabel
) {
}
