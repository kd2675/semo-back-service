package semo.back.service.feature.notice.vo;

public record ClubNoticeSummaryResponse(
        Long noticeId,
        String title,
        String summary,
        String authorDisplayName,
        String authorRoleCode,
        String categoryKey,
        String categoryLabel,
        String categoryIconName,
        String categoryAccentTone,
        String publishedAtLabel,
        String timeAgo,
        boolean pinned,
        String scheduleAtLabel,
        String locationLabel,
        boolean canManage,
        String linkedTargetType,
        Long linkedTargetId
) {
}
