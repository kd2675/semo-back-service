package semo.back.service.database.pub.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import semo.back.service.database.pub.entity.BracketRecord;

import java.util.List;
import java.util.Optional;

public interface BracketRecordRepository extends JpaRepository<BracketRecord, Long> {
    Optional<BracketRecord> findByBracketRecordIdAndClubIdAndDeletedFalse(Long bracketRecordId, Long clubId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select bracket
            from BracketRecord bracket
            where bracket.bracketRecordId = :bracketRecordId
              and bracket.clubId = :clubId
              and bracket.deleted = false
            """)
    Optional<BracketRecord> findForUpdate(Long bracketRecordId, Long clubId);

    List<BracketRecord> findByClubIdAndDeletedFalseOrderByCreateDateDescBracketRecordIdDesc(Long clubId);

    long countByClubIdAndAuthorClubProfileIdAndDeletedFalseAndApprovalStatus(
            Long clubId,
            Long authorClubProfileId,
            String approvalStatus
    );

    long countByClubIdAndDeletedFalseAndApprovalStatus(Long clubId, String approvalStatus);
}
