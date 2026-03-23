package semo.back.service.database.pub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import semo.back.service.database.pub.entity.ClubPositionPermission;

import java.util.List;

public interface ClubPositionPermissionRepository extends JpaRepository<ClubPositionPermission, Long> {
    List<ClubPositionPermission> findByClubPositionIdIn(List<Long> clubPositionIds);

    List<ClubPositionPermission> findByClubPositionId(Long clubPositionId);

    void deleteByClubPositionId(Long clubPositionId);
}
