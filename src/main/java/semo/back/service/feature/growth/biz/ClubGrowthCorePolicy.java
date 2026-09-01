package semo.back.service.feature.growth.biz;

import org.springframework.stereotype.Component;
import semo.back.service.database.pub.repository.ClubGrowthEvidenceRow;

@Component
public class ClubGrowthCorePolicy {
    public static final int POLICY_VERSION = 1;
    public static final int SCORE_PER_TIER = 100;
    public static final int MAX_TIER_LEVEL = ClubGrowthTier.DIAMOND.level();

    public Projection evaluate(ClubGrowthEvidenceRow evidence, int currentTierLevel) {
        long togetherScore = weightedSum(
                evidence.getAttendanceCount(), 8,
                evidence.getVoteSelectionCount(), 4,
                evidence.getCompletedTodoCount(), 6,
                evidence.getSubmittedFeedbackCount(), 5,
                evidence.getBoardReadCount(), 2
        );
        long operationsScore = weightedSum(
                evidence.getPublishedNoticeCount(), 8,
                evidence.getCompletedTodoCount(), 10,
                evidence.getAnsweredFeedbackCount(), 12,
                evidence.getClosedVoteCount(), 10,
                evidence.getAttendanceEventCount(), 10
        );
        long continuityScore = weightedSum(
                evidence.getConfirmedDecisionCount(), 18,
                evidence.getDecisionResourceLinkCount(), 6,
                evidence.getLinkedTodoCount(), 8,
                evidence.getRecurringTodoCount(), 12,
                evidence.getAcknowledgedHandoverCount(), 25,
                evidence.getResolvedCarryoverCount(), 20,
                evidence.getClosedTermCount(), 30
        );
        long completedTiers = Math.min(
                Math.min(togetherScore, operationsScore),
                continuityScore
        ) / SCORE_PER_TIER;
        int tierLevel = Math.max(
                Math.max(ClubGrowthTier.RAW.level(), currentTierLevel),
                (int) Math.min(MAX_TIER_LEVEL, completedTiers)
        );

        return new Projection(
                tierLevel,
                togetherScore,
                operationsScore,
                continuityScore,
                toActivityCount(evidence.getRecentActivityCount()),
                POLICY_VERSION
        );
    }

    public int progress(long score, int tierLevel) {
        if (tierLevel >= MAX_TIER_LEVEL) {
            return 100;
        }
        long tierFloor = (long) Math.max(0, tierLevel) * SCORE_PER_TIER;
        return (int) Math.max(0, Math.min(100, score - tierFloor));
    }

    public int activityLevel(int recentActivityCount) {
        if (recentActivityCount <= 0) {
            return 0;
        }
        if (recentActivityCount <= 3) {
            return 1;
        }
        if (recentActivityCount <= 9) {
            return 2;
        }
        if (recentActivityCount <= 24) {
            return 3;
        }
        return 4;
    }

    private int toActivityCount(long count) {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0, count));
    }

    private long weightedSum(long... countAndWeightPairs) {
        long result = 0;
        for (int index = 0; index < countAndWeightPairs.length; index += 2) {
            long count = Math.max(0, countAndWeightPairs[index]);
            long weight = Math.max(0, countAndWeightPairs[index + 1]);
            long weighted = count > Long.MAX_VALUE / Math.max(1, weight)
                    ? Long.MAX_VALUE
                    : count * weight;
            result = Long.MAX_VALUE - result < weighted ? Long.MAX_VALUE : result + weighted;
        }
        return result;
    }

    public record Projection(
            int tierLevel,
            long togetherScore,
            long operationsScore,
            long continuityScore,
            int recentActivityCount,
            int policyVersion
    ) {
    }
}
