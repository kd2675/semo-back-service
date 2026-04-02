package semo.back.service.feature.club.vo;

public record ClubDiscoverSummaryResponse(
        Long clubId,
        String name,
        String summary,
        String description,
        String categoryKey,
        String visibilityStatus,
        String membershipPolicy,
        int activeMemberCount,
        String fileName,
        String imageUrl,
        String thumbnailUrl,
        String joinStatus,
        Long clubJoinRequestId,
        boolean recommendedByCategory
) {
}
