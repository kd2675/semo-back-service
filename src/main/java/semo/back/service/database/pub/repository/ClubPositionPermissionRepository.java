package semo.back.service.database.pub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import semo.back.service.database.pub.entity.ClubPositionPermission;

import java.util.List;

public interface ClubPositionPermissionRepository extends JpaRepository<ClubPositionPermission, Long> {
    List<ClubPositionPermission> findByClubPositionIdIn(List<Long> clubPositionIds);

    List<ClubPositionPermission> findByClubPositionId(Long clubPositionId);

    @Query("""
            select distinct permission.permissionKey
            from ClubMemberPosition assignment
            join ClubPosition position on position.clubPositionId = assignment.clubPositionId
            join ClubPositionPermission permission on permission.clubPositionId = position.clubPositionId
            where assignment.clubMemberId = :clubMemberId
              and position.clubId = :clubId
              and position.active = true
            """)
    List<String> findEffectivePermissionKeys(Long clubId, Long clubMemberId);

    void deleteByClubPositionId(Long clubPositionId);
}
