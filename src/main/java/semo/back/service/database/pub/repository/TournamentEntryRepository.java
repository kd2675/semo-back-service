package semo.back.service.database.pub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import semo.back.service.database.pub.entity.TournamentEntry;

import java.util.Collection;
import java.util.List;

public interface TournamentEntryRepository extends JpaRepository<TournamentEntry, Long> {
    List<TournamentEntry> findByTournamentRecordIdOrderBySortOrderAscTournamentEntryIdAsc(Long tournamentRecordId);

    List<TournamentEntry> findByTournamentRecordIdIn(Collection<Long> tournamentRecordIds);

    void deleteByTournamentRecordId(Long tournamentRecordId);
}
