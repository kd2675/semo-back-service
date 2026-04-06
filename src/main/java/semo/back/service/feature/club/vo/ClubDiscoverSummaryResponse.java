package semo.back.service.feature.club.vo;

import java.util.List;

public record ClubDiscoverSummaryResponse(
        Long clubId,
        String name,
        String summary,
        String description,
        String categoryKey,
        String activityCategory,
        List<String> activityTags,
        String affiliationType,
        String visibilityStatus,
        String membershipPolicy,
        String regionScope,
        String regionDepth1Code,
        String regionDepth2Code,
        String regionDepth1Name,
        String regionDepth2Name,
        String regionLabel,
        int activeMemberCount,
        String fileName,
        String imageUrl,
        String thumbnailUrl,
        String joinStatus,
        Long clubJoinRequestId,
        boolean recommendedByCategory,
        boolean recommendedByTags
) {
}
