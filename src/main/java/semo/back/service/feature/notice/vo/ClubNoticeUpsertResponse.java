package semo.back.service.feature.notice.vo;

public record ClubNoticeUpsertResponse(
        Long noticeId,
        String title,
        String fileName,
        String imageUrl,
        String thumbnailUrl,
        String scheduleAt,
        String scheduleAtLabel,
        String locationLabel
) {
}
