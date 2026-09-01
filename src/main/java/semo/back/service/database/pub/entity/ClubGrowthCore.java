package semo.back.service.database.pub.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import semo.back.service.common.jpa.CommonDateEntity;

import java.time.LocalDateTime;

@Entity
@Table(name = "club_growth_core")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ClubGrowthCore extends CommonDateEntity {
    @Id
    @Column(name = "club_id")
    private Long clubId;

    @Column(name = "tier_level", nullable = false)
    private int tierLevel;

    @Column(name = "together_score", nullable = false)
    private long togetherScore;

    @Column(name = "operations_score", nullable = false)
    private long operationsScore;

    @Column(name = "continuity_score", nullable = false)
    private long continuityScore;

    @Column(name = "recent_activity_count", nullable = false)
    private int recentActivityCount;

    @Column(name = "policy_version", nullable = false)
    private int policyVersion;

    @Column(name = "last_projected_at")
    private LocalDateTime lastProjectedAt;

    @Column(name = "tier_changed_at")
    private LocalDateTime tierChangedAt;

    @Version
    @Column(name = "row_version", nullable = false)
    private long rowVersion;

    public static ClubGrowthCore initial(Long clubId, int policyVersion) {
        return new ClubGrowthCore(
                clubId,
                0,
                0,
                0,
                0,
                0,
                policyVersion,
                null,
                null,
                0
        );
    }

    public void applyProjection(
            int nextTierLevel,
            long nextTogetherScore,
            long nextOperationsScore,
            long nextContinuityScore,
            int nextRecentActivityCount,
            int nextPolicyVersion,
            LocalDateTime projectedAt
    ) {
        if (nextTierLevel > tierLevel) {
            tierChangedAt = projectedAt;
        }
        tierLevel = Math.max(tierLevel, nextTierLevel);
        togetherScore = Math.max(0, nextTogetherScore);
        operationsScore = Math.max(0, nextOperationsScore);
        continuityScore = Math.max(0, nextContinuityScore);
        recentActivityCount = Math.max(0, nextRecentActivityCount);
        policyVersion = nextPolicyVersion;
        lastProjectedAt = projectedAt;
    }
}
