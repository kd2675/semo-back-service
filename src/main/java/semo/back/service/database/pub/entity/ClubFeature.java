package semo.back.service.database.pub.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import semo.back.service.common.jpa.CommonDateEntity;

import java.time.LocalDateTime;

@Entity
@Table(name = "feature_activation")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ClubFeature extends CommonDateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "feature_activation_id")
    private Long clubFeatureId;

    @Column(name = "club_id", nullable = false)
    private Long clubId;

    @Column(name = "feature_key", nullable = false, length = 50)
    private String featureKey;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "enabled_by_club_profile_id")
    private Long enabledByClubProfileId;

    @Column(name = "enabled_at")
    private LocalDateTime enabledAt;
}
