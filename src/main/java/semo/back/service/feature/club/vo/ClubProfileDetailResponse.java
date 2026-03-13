package semo.back.service.feature.club.vo;

public record ClubProfileDetailResponse(
        Long clubProfileId,
        String displayName,
        String tagline,
        String introText,
        String avatarFileName,
        String avatarImageUrl,
        String avatarThumbnailUrl,
        String roleCode,
        String membershipStatus,
        String joinedLabel
) {
}
