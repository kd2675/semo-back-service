package semo.back.service.database.pub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import semo.back.service.database.pub.entity.TournamentRound;

import java.util.List;

public interface TournamentRoundRepository extends JpaRepository<TournamentRound, Long> {
    List<TournamentRound> findByTournamentRecordIdOrderBySortOrderAscTournamentRoundIdAsc(Long tournamentRecordId);

    void deleteByTournamentRecordId(Long tournamentRecordId);
}
