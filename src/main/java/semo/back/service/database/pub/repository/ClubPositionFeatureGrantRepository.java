package semo.back.service.database.pub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import semo.back.service.database.pub.entity.ClubPositionFeatureGrant;

import java.util.Collection;
import java.util.List;

public interface ClubPositionFeatureGrantRepository extends JpaRepository<ClubPositionFeatureGrant, Long> {
    List<ClubPositionFeatureGrant> findByClubPositionId(Long clubPositionId);

    List<ClubPositionFeatureGrant> findByClubPositionIdIn(Collection<Long> clubPositionIds);

    void deleteByClubPositionId(Long clubPositionId);
}
