package semo.back.service.database.pub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import semo.back.service.database.pub.entity.TournamentMatchSide;

import java.util.Collection;
import java.util.List;

public interface TournamentMatchSideRepository extends JpaRepository<TournamentMatchSide, Long> {
    List<TournamentMatchSide> findByTournamentMatchIdInOrderBySideNoAsc(Collection<Long> tournamentMatchIds);

    void deleteByTournamentMatchIdIn(Collection<Long> tournamentMatchIds);
}
