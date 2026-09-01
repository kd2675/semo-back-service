package semo.back.service.feature.growth.biz;

import org.junit.jupiter.api.Test;
import semo.back.service.database.pub.repository.ClubGrowthEvidenceRow;

import static org.assertj.core.api.Assertions.assertThat;

class ClubGrowthCorePolicyTest {
    private final ClubGrowthCorePolicy policy = new ClubGrowthCorePolicy();

    @Test
    void evaluate_allAxesCrossFirstThreshold_upgradesToIron() {
        ClubGrowthCorePolicy.Projection projection = policy.evaluate(new Evidence(
                13, 0, 0, 0, 0,
                13, 0, 0, 0,
                6, 0, 0, 0, 0, 0, 0,
                10
        ), 0);

        assertThat(projection)
                .extracting(
                        ClubGrowthCorePolicy.Projection::tierLevel,
                        ClubGrowthCorePolicy.Projection::togetherScore,
                        ClubGrowthCorePolicy.Projection::operationsScore,
                        ClubGrowthCorePolicy.Projection::continuityScore
                )
                .containsExactly(1, 104L, 104L, 108L);
    }

    @Test
    void evaluate_sourceRowsDisappear_preservesAchievedTier() {
        ClubGrowthCorePolicy.Projection projection = policy.evaluate(Evidence.empty(), 2);

        assertThat(projection.tierLevel()).isEqualTo(2);
    }

    @Test
    void progress_afterTierUpgrade_usesOnlyNextTierRemainder() {
        assertThat(policy.progress(148, 1)).isEqualTo(48);
    }

    @Test
    void activityLevel_highAbsoluteActivity_usesHighestLightLevel() {
        assertThat(policy.activityLevel(25)).isEqualTo(4);
    }

    private record Evidence(
            long attendanceCount,
            long voteSelectionCount,
            long completedTodoCount,
            long submittedFeedbackCount,
            long boardReadCount,
            long publishedNoticeCount,
            long answeredFeedbackCount,
            long closedVoteCount,
            long attendanceEventCount,
            long confirmedDecisionCount,
            long decisionResourceLinkCount,
            long linkedTodoCount,
            long recurringTodoCount,
            long acknowledgedHandoverCount,
            long resolvedCarryoverCount,
            long closedTermCount,
            long recentActivityCount
    ) implements ClubGrowthEvidenceRow {
        private static Evidence empty() {
            return new Evidence(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        }

        @Override
        public long getAttendanceCount() {
            return attendanceCount;
        }

        @Override
        public long getVoteSelectionCount() {
            return voteSelectionCount;
        }

        @Override
        public long getCompletedTodoCount() {
            return completedTodoCount;
        }

        @Override
        public long getSubmittedFeedbackCount() {
            return submittedFeedbackCount;
        }

        @Override
        public long getBoardReadCount() {
            return boardReadCount;
        }

        @Override
        public long getPublishedNoticeCount() {
            return publishedNoticeCount;
        }

        @Override
        public long getAnsweredFeedbackCount() {
            return answeredFeedbackCount;
        }

        @Override
        public long getClosedVoteCount() {
            return closedVoteCount;
        }

        @Override
        public long getAttendanceEventCount() {
            return attendanceEventCount;
        }

        @Override
        public long getConfirmedDecisionCount() {
            return confirmedDecisionCount;
        }

        @Override
        public long getDecisionResourceLinkCount() {
            return decisionResourceLinkCount;
        }

        @Override
        public long getLinkedTodoCount() {
            return linkedTodoCount;
        }

        @Override
        public long getRecurringTodoCount() {
            return recurringTodoCount;
        }

        @Override
        public long getAcknowledgedHandoverCount() {
            return acknowledgedHandoverCount;
        }

        @Override
        public long getResolvedCarryoverCount() {
            return resolvedCarryoverCount;
        }

        @Override
        public long getClosedTermCount() {
            return closedTermCount;
        }

        @Override
        public long getRecentActivityCount() {
            return recentActivityCount;
        }
    }
}
