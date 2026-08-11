package semo.back.service.database.pub.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import semo.back.service.database.pub.entity.TournamentRecord;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;

public interface TournamentRecordRepository extends JpaRepository<TournamentRecord, Long> {
    Optional<TournamentRecord> findByTournamentRecordIdAndClubIdAndDeletedFalse(Long tournamentRecordId, Long clubId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select tournament
            from TournamentRecord tournament
            where tournament.tournamentRecordId = :tournamentRecordId
              and tournament.clubId = :clubId
              and tournament.deleted = false
            """)
    Optional<TournamentRecord> findForUpdate(
            @Param("tournamentRecordId") Long tournamentRecordId,
            @Param("clubId") Long clubId
    );

    List<TournamentRecord> findByClubIdAndDeletedFalseOrderByPinnedDescStartDateAscTournamentRecordIdDesc(Long clubId);

    List<TournamentRecord> findByClubIdAndDeletedFalseOrderByPinnedDescStartDateAscTournamentRecordIdDesc(
            Long clubId,
            Pageable pageable
    );

    List<TournamentRecord> findByClubIdAndDeletedFalseAndPinnedTrueOrderByStartDateAscTournamentRecordIdDesc(Long clubId);

    List<TournamentRecord> findByClubIdAndDeletedFalseAndStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByPinnedDescStartDateAscTournamentRecordIdDesc(
            Long clubId,
            LocalDate endDate,
            LocalDate startDate
    );

    List<TournamentRecord> findAllByTournamentRecordIdIn(List<Long> tournamentRecordIds);

    long countByClubIdAndDeletedFalseAndApprovalStatus(Long clubId, String approvalStatus);

    long countByClubIdAndDeletedFalseAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            Long clubId,
            LocalDate endDate,
            LocalDate startDate
    );
}
