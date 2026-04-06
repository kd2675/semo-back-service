package semo.back.service.feature.club.vo;

public record MyClubSummaryResponse(
        Long clubId,
        String name,
        String summary,
        String description,
        String categoryKey,
        String regionScope,
        String regionDepth1Code,
        String regionDepth2Code,
        String regionDepth1Name,
        String regionDepth2Name,
        String regionLabel,
        String roleCode,
        boolean admin,
        String fileName,
        String imageUrl,
        String thumbnailUrl
) {
}
