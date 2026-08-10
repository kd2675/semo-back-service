package semo.back.service.database.pub.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import semo.back.service.database.pub.entity.ClubTermExecutiveAssignment;

public interface ClubTermExecutiveAssignmentRepository extends JpaRepository<ClubTermExecutiveAssignment, Long> {
    List<ClubTermExecutiveAssignment> findByClubOperatingTermIdInOrderBySortOrderAscClubTermExecutiveAssignmentIdAsc(
            Collection<Long> clubOperatingTermIds
    );

    List<ClubTermExecutiveAssignment> findByClubOperatingTermIdOrderBySortOrderAscClubTermExecutiveAssignmentIdAsc(
            Long clubOperatingTermId
    );

    Optional<ClubTermExecutiveAssignment> findByClubTermExecutiveAssignmentIdAndClubId(
            Long clubTermExecutiveAssignmentId,
            Long clubId
    );

    Optional<ClubTermExecutiveAssignment> findByClubOperatingTermIdAndClubMemberIdAndClubPositionId(
            Long clubOperatingTermId,
            Long clubMemberId,
            Long clubPositionId
    );

    long countByClubOperatingTermId(Long clubOperatingTermId);
}
