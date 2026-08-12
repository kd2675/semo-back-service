package semo.back.service.database.pub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import semo.back.service.database.pub.entity.ClubPositionSensitiveGrant;

import java.util.Collection;
import java.util.List;

public interface ClubPositionSensitiveGrantRepository extends JpaRepository<ClubPositionSensitiveGrant, Long> {
    List<ClubPositionSensitiveGrant> findByClubPositionId(Long clubPositionId);

    List<ClubPositionSensitiveGrant> findByClubPositionIdIn(Collection<Long> clubPositionIds);

    void deleteByClubPositionId(Long clubPositionId);
}
