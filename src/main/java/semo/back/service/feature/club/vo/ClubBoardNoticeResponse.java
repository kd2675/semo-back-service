package semo.back.service.feature.club.vo;

public record ClubBoardNoticeResponse(
        String id,
        String icon,
        String title,
        String summary,
        String author,
        String timeAgo,
        String category
) {
}
