package semo.back.service.database.pub.repository;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import semo.back.service.database.pub.entity.ClubMorePreference;

public interface ClubMorePreferenceRepository extends JpaRepository<ClubMorePreference, Long> {
    List<ClubMorePreference> findByClubIdAndClubProfileId(Long clubId, Long clubProfileId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select preference
            from ClubMorePreference preference
            where preference.clubId = :clubId
              and preference.clubProfileId = :clubProfileId
              and preference.featureKey = :featureKey
            """)
    Optional<ClubMorePreference> findForUpdate(
            @Param("clubId") Long clubId,
            @Param("clubProfileId") Long clubProfileId,
            @Param("featureKey") String featureKey
    );
}
