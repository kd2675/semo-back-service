package semo.back.service.feature.timeline.vo;

public record TimelineEntryResponse(
        Long noticeId,
        String title,
        String summary,
        String authorDisplayName,
        String publishedAt,
        String publishedAtLabel,
        String timeAgo,
        boolean pinned,
        String scheduleAtLabel,
        String locationLabel,
        String linkedTargetType,
        Long linkedTargetId
) {
}
