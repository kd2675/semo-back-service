package semo.back.service.database.pub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import semo.back.service.database.pub.entity.ClubMemberPosition;

import java.util.List;

public interface ClubMemberPositionRepository extends JpaRepository<ClubMemberPosition, Long> {
    List<ClubMemberPosition> findByClubMemberId(Long clubMemberId);

    List<ClubMemberPosition> findByClubMemberIdIn(List<Long> clubMemberIds);

    List<ClubMemberPosition> findByClubPositionIdIn(List<Long> clubPositionIds);

    void deleteByClubMemberId(Long clubMemberId);

    void deleteByClubPositionId(Long clubPositionId);
}
