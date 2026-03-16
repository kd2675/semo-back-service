package semo.back.service.feature.notice.vo;

public record ClubNoticeDetailResponse(
        Long clubId,
        String clubName,
        boolean admin,
        Long noticeId,
        String title,
        String content,
        String categoryKey,
        String categoryLabel,
        String authorDisplayName,
        String authorRoleCode,
        String publishedAtLabel,
        String updatedAtLabel,
        boolean pinned,
        String locationLabel,
        String scheduleAt,
        String scheduleAtLabel,
        String scheduleEndAt,
        String scheduleEndAtLabel,
        boolean canManage,
        String linkedTargetType,
        Long linkedTargetId
) {
}
