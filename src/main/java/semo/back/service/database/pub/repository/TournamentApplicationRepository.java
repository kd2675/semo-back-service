package semo.back.service.database.pub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import semo.back.service.database.pub.entity.TournamentApplication;

import java.util.List;
import java.util.Optional;
import java.util.Collection;

public interface TournamentApplicationRepository extends JpaRepository<TournamentApplication, Long> {
    List<TournamentApplication> findByTournamentRecordIdOrderByCreateDateAscTournamentApplicationIdAsc(Long tournamentRecordId);

    Optional<TournamentApplication> findByTournamentRecordIdAndClubProfileId(Long tournamentRecordId, Long clubProfileId);

    List<TournamentApplication> findByTournamentRecordIdAndApplicationStatusOrderByCreateDateAscTournamentApplicationIdAsc(
            Long tournamentRecordId,
            String applicationStatus
    );

    List<TournamentApplication> findByTournamentRecordIdIn(Collection<Long> tournamentRecordIds);

    List<TournamentApplication> findByClubProfileId(Long clubProfileId);

    void deleteByTournamentRecordId(Long tournamentRecordId);
}
