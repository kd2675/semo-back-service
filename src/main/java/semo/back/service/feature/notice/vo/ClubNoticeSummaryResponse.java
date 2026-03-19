package semo.back.service.feature.notice.vo;

public record ClubNoticeSummaryResponse(
        Long noticeId,
        String title,
        String summary,
        String fileName,
        String imageUrl,
        String thumbnailUrl,
        String authorDisplayName,
        String authorRoleCode,
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
