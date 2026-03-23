package semo.back.service.feature.club.vo;

public record ClubBoardNoticeResponse(
        String id,
        String icon,
        String title,
        String summary,
        String imageUrl,
        String thumbnailUrl,
        String author,
        String timeAgo,
        String category
) {
}
