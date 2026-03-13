package semo.back.service.feature.notice.vo;

public record ClubNoticeSummaryResponse(
        Long noticeId,
        String title,
        String summary,
        String authorDisplayName,
        String authorRoleCode,
        String categoryKey,
        String categoryLabel,
        String publishedAtLabel,
        String timeAgo,
        boolean pinned,
        String scheduleAtLabel,
        String locationLabel
) {
}
