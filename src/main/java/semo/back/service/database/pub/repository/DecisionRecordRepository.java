package semo.back.service.database.pub.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import semo.back.service.database.pub.entity.DecisionRecord;

public interface DecisionRecordRepository extends JpaRepository<DecisionRecord, Long> {
    List<DecisionRecord> findByClubIdAndDeletedFalseOrderByDecisionRecordIdDesc(Long clubId);

    Optional<DecisionRecord> findByDecisionRecordIdAndClubIdAndDeletedFalse(Long decisionRecordId, Long clubId);

    long countByClubIdAndDeletedFalseAndStatusCode(Long clubId, String statusCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select decision
            from DecisionRecord decision
            where decision.decisionRecordId = :decisionRecordId
              and decision.clubId = :clubId
              and decision.deleted = false
            """)
    Optional<DecisionRecord> findForUpdate(Long decisionRecordId, Long clubId);

    @Query("""
            select decision
            from DecisionRecord decision
            where decision.clubId = :clubId
              and decision.deleted = false
              and decision.visibilityScope = 'MEMBERS'
              and decision.statusCode in :statusCodes
            order by
              case when decision.confirmedAt is null then 1 else 0 end,
              decision.confirmedAt desc,
              decision.decisionRecordId desc
            """)
    List<DecisionRecord> findMemberFeed(Long clubId, Collection<String> statusCodes);

    @Query("""
            select count(decision)
            from DecisionRecord decision
            where decision.clubId = :clubId
              and decision.deleted = false
              and decision.statusCode = 'CONFIRMED'
              and decision.reviewDate is not null
              and decision.reviewDate <= :today
            """)
    long countReviewDue(Long clubId, LocalDate today);

    @Query("""
            select decision
            from DecisionRecord decision
            where decision.clubId = :clubId
              and decision.deleted = false
              and decision.statusCode in ('CONFIRMED', 'SUPERSEDED')
            order by decision.confirmedAt desc, decision.decisionRecordId desc
            """)
    List<DecisionRecord> findRecentConfirmed(Long clubId, Pageable pageable);

    @Query("""
            select decision
            from DecisionRecord decision
            where decision.clubId = :clubId
              and decision.deleted = false
              and decision.visibilityScope = 'MEMBERS'
              and decision.statusCode in ('CONFIRMED', 'SUPERSEDED')
            order by decision.confirmedAt desc, decision.decisionRecordId desc
            """)
    List<DecisionRecord> findTodoLinkOptions(Long clubId, Pageable pageable);
}
