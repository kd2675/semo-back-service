package semo.back.service.feature.timeline.vo;

public record TimelineEntryResponse(
        Long noticeId,
        String title,
        String summary,
        String categoryKey,
        String categoryLabel,
        String categoryIconName,
        String categoryAccentTone,
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
