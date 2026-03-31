package semo.back.service.database.pub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import semo.back.service.database.pub.entity.TournamentMatch;

import java.util.Collection;
import java.util.List;

public interface TournamentMatchRepository extends JpaRepository<TournamentMatch, Long> {
    List<TournamentMatch> findByTournamentRecordIdOrderBySortOrderAscTournamentMatchIdAsc(Long tournamentRecordId);

    List<TournamentMatch> findByTournamentRoundIdInOrderBySortOrderAscTournamentMatchIdAsc(Collection<Long> tournamentRoundIds);

    void deleteByTournamentRecordId(Long tournamentRecordId);
}
