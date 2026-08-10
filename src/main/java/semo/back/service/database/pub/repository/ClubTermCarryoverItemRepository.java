package semo.back.service.database.pub.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import semo.back.service.database.pub.entity.ClubTermCarryoverItem;

public interface ClubTermCarryoverItemRepository extends JpaRepository<ClubTermCarryoverItem, Long> {
    List<ClubTermCarryoverItem> findByToTermIdOrderByStatusCodeAscDueAtAscClubTermCarryoverItemIdAsc(Long toTermId);

    Optional<ClubTermCarryoverItem> findByClubTermCarryoverItemIdAndClubId(
            Long clubTermCarryoverItemId,
            Long clubId
    );

    boolean existsByToTermIdAndResourceTypeAndResourceId(Long toTermId, String resourceType, Long resourceId);

    long countByClubIdAndStatusCode(Long clubId, String statusCode);
}
