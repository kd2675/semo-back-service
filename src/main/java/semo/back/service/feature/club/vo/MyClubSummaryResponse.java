package semo.back.service.feature.club.vo;

public record MyClubSummaryResponse(
        Long clubId,
        String name,
        String summary,
        String description,
        String categoryKey,
        String roleCode,
        boolean admin,
        String fileName,
        String imageUrl,
        String thumbnailUrl
) {
}
