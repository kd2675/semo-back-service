package semo.back.service.database.pub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import semo.back.service.database.pub.entity.ClubPosition;

import java.util.List;
import java.util.Optional;

public interface ClubPositionRepository extends JpaRepository<ClubPosition, Long> {
    List<ClubPosition> findByClubIdOrderByDisplayNameAscClubPositionIdAsc(Long clubId);

    List<ClubPosition> findByClubIdAndActiveTrueOrderByDisplayNameAscClubPositionIdAsc(Long clubId);

    Optional<ClubPosition> findByClubPositionIdAndClubId(Long clubPositionId, Long clubId);

    boolean existsByClubIdAndPositionCode(Long clubId, String positionCode);

    boolean existsByClubIdAndPositionCodeAndClubPositionIdNot(Long clubId, String positionCode, Long clubPositionId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update ClubPosition position
            set position.version = position.version + 1,
                position.updateDate = CURRENT_TIMESTAMP
            where position.clubPositionId = :clubPositionId
              and position.clubId = :clubId
              and position.version = :expectedVersion
            """)
    int claimVersion(Long clubPositionId, Long clubId, Long expectedVersion);
}
