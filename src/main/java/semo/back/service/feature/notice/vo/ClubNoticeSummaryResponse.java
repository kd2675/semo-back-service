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
        String authorAvatarImageUrl,
        String authorAvatarThumbnailUrl,
        String publishedAtLabel,
        String timeAgo,
        boolean pinned,
        String scheduleAt,
        String scheduleEndAt,
        String scheduleAtLabel,
        String locationLabel,
        boolean postedToBoard,
        boolean postedToCalendar,
        boolean canManage,
        boolean canEdit,
        boolean canDelete,
        String linkedTargetType,
        Long linkedTargetId
) {
}
