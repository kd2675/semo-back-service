package semo.back.service.feature.club.vo;

public record ClubCreateResponse(
        Long clubId,
        String name,
        String summary,
        String description,
        String categoryKey,
        String visibilityStatus,
        String membershipPolicy,
        String roleCode,
        String fileName,
        String imageUrl,
        String thumbnailUrl
) {
}
