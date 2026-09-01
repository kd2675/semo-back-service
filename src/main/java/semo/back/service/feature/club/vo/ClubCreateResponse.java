package semo.back.service.feature.club.vo;

import java.util.List;

import semo.back.service.feature.growth.vo.ClubGrowthCoreResponse;

public record ClubCreateResponse(
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
        String roleCode,
        String fileName,
        String imageUrl,
        String thumbnailUrl,
        ClubGrowthCoreResponse growthCore
) {
}
