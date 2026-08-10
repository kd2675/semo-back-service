package semo.back.service.database.pub.entity;

import java.time.LocalDateTime;

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

@Entity
@Table(name = "club_more_preference")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ClubMorePreference extends CommonDateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "club_more_preference_id")
    private Long clubMorePreferenceId;

    @Column(name = "club_id", nullable = false)
    private Long clubId;

    @Column(name = "club_profile_id", nullable = false)
    private Long clubProfileId;

    @Column(name = "feature_key", nullable = false, length = 50)
    private String featureKey;

    @Column(name = "favorite", nullable = false)
    private boolean favorite;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    public void updateFavorite(boolean nextFavorite) {
        favorite = nextFavorite;
    }

    public void markUsed(LocalDateTime usedAt) {
        lastUsedAt = usedAt;
    }
}
