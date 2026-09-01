package semo.back.service.feature.growth.vo;

public record ClubGrowthCoreResponse(
        String tierCode,
        String tierLabel,
        String nextTierCode,
        String nextTierLabel,
        int togetherProgress,
        int operationsProgress,
        int continuityProgress,
        int memberCount,
        int activityLevel,
        int policyVersion,
        String lastProjectedAt
) {
}
