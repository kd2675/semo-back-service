package semo.back.service.database.pub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import semo.back.service.database.pub.entity.TournamentRecord;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TournamentRecordRepository extends JpaRepository<TournamentRecord, Long> {
    Optional<TournamentRecord> findByTournamentRecordIdAndClubIdAndDeletedFalse(Long tournamentRecordId, Long clubId);

    List<TournamentRecord> findByClubIdAndDeletedFalseOrderByPinnedDescStartDateAscTournamentRecordIdDesc(Long clubId);

    List<TournamentRecord> findByClubIdAndDeletedFalseAndPinnedTrueOrderByStartDateAscTournamentRecordIdDesc(Long clubId);

    List<TournamentRecord> findByClubIdAndDeletedFalseAndStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByPinnedDescStartDateAscTournamentRecordIdDesc(
            Long clubId,
            LocalDate endDate,
            LocalDate startDate
    );

    List<TournamentRecord> findAllByTournamentRecordIdIn(List<Long> tournamentRecordIds);
}
