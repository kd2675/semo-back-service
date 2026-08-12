package semo.back.service.database.pub.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import semo.back.service.common.jpa.CommonDateEntity;

@Entity
@Table(
        name = "club_position_feature_grant",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_club_position_feature_grant",
                        columnNames = {"club_position_id", "feature_key"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ClubPositionFeatureGrant extends CommonDateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "club_position_feature_grant_id")
    private Long clubPositionFeatureGrantId;

    @Column(name = "club_position_id", nullable = false)
    private Long clubPositionId;

    @Column(name = "feature_key", nullable = false, length = 50)
    private String featureKey;

    @Column(name = "access_level", nullable = false, length = 20)
    private String accessLevel;

    @Column(name = "policy_version", nullable = false)
    private int policyVersion;
}
