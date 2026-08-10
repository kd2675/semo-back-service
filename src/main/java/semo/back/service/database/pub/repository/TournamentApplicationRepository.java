package semo.back.service.database.pub.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import semo.back.service.database.pub.entity.TournamentApplication;

import java.util.List;
import java.util.Optional;
import java.util.Collection;

public interface TournamentApplicationRepository extends JpaRepository<TournamentApplication, Long> {
    List<TournamentApplication> findByTournamentRecordIdOrderByCreateDateAscTournamentApplicationIdAsc(Long tournamentRecordId);

    Optional<TournamentApplication> findByTournamentRecordIdAndClubProfileId(Long tournamentRecordId, Long clubProfileId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select application
            from TournamentApplication application
            where application.tournamentApplicationId = :tournamentApplicationId
              and application.tournamentRecordId = :tournamentRecordId
            """)
    Optional<TournamentApplication> findForUpdate(Long tournamentRecordId, Long tournamentApplicationId);

    List<TournamentApplication> findByTournamentRecordIdAndApplicationStatusOrderByCreateDateAscTournamentApplicationIdAsc(
            Long tournamentRecordId,
            String applicationStatus
    );

    List<TournamentApplication> findByTournamentRecordIdIn(Collection<Long> tournamentRecordIds);

    List<TournamentApplication> findByClubProfileId(Long clubProfileId);

    @Query("""
            select count(application)
            from TournamentApplication application, TournamentRecord tournament
            where application.tournamentRecordId = tournament.tournamentRecordId
              and tournament.clubId = :clubId
              and tournament.deleted = false
              and application.clubProfileId = :clubProfileId
              and application.applicationStatus = 'APPLIED'
            """)
    long countPendingApplicationsForMember(Long clubId, Long clubProfileId);

    @Query("""
            select count(application)
            from TournamentApplication application, TournamentRecord tournament
            where application.tournamentRecordId = tournament.tournamentRecordId
              and tournament.clubId = :clubId
              and tournament.deleted = false
              and application.applicationStatus = 'APPLIED'
            """)
    long countPendingApplicationsForClub(Long clubId);

    void deleteByTournamentRecordId(Long tournamentRecordId);
}
