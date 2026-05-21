package semo.back.service.database.pub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import semo.back.service.database.pub.entity.ClubMemberPositionHistory;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ClubMemberPositionHistoryRepository extends JpaRepository<ClubMemberPositionHistory, Long> {
    Optional<ClubMemberPositionHistory> findByClubMemberPositionHistoryIdAndClubId(Long clubMemberPositionHistoryId, Long clubId);

    List<ClubMemberPositionHistory> findByClubIdAndDeletedFalseOrderByStartedAtDescClubMemberPositionHistoryIdDesc(Long clubId);

    @Query("""
            select h
            from ClubMemberPositionHistory h
            where h.clubMemberId = :clubMemberId
              and h.clubPositionId = :clubPositionId
              and h.deleted = false
              and h.endedAt is null
            order by h.startedAt desc, h.clubMemberPositionHistoryId desc
            """)
    List<ClubMemberPositionHistory> findOpenHistories(Long clubMemberId, Long clubPositionId);

    @Query("""
            select h
            from ClubMemberPositionHistory h
            where h.clubId = :clubId
              and h.clubPositionId = :clubPositionId
              and h.deleted = false
              and h.endedAt is null
            """)
    List<ClubMemberPositionHistory> findOpenHistoriesByPosition(Long clubId, Long clubPositionId);

    @Query("""
            select h
            from ClubMemberPositionHistory h
            where h.clubId = :clubId
              and h.clubMemberId in :clubMemberIds
              and h.deleted = false
              and h.startedAt <= :maxCreatedAt
              and (h.endedAt is null or h.endedAt >= :minCreatedAt)
            """)
    List<ClubMemberPositionHistory> findEffectiveHistoriesForActivityWindow(
            Long clubId,
            Collection<Long> clubMemberIds,
            LocalDateTime minCreatedAt,
            LocalDateTime maxCreatedAt
    );
}
