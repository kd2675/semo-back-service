package semo.back.service.feature.club.vo;

public record ClubCreateResponse(
        Long clubId,
        String name,
        String summary,
        String description,
        String categoryKey,
        String visibilityStatus,
        String membershipPolicy,
        String regionScope,
        String regionDepth1Code,
        String regionDepth2Code,
        String regionDepth1Name,
        String regionDepth2Name,
        String regionLabel,
        String roleCode,
        String fileName,
        String imageUrl,
        String thumbnailUrl
) {
}
