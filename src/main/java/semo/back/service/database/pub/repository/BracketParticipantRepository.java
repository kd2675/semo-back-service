package semo.back.service.database.pub.repository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import semo.back.service.database.pub.entity.BracketParticipant;

import java.util.Collection;
import java.util.List;

public interface BracketParticipantRepository extends JpaRepository<BracketParticipant, Long> {
    List<BracketParticipant> findByBracketRecordIdOrderBySeedNumberAscBracketParticipantIdAsc(Long bracketRecordId);

    List<BracketParticipant> findByBracketRecordIdIn(Collection<Long> bracketRecordIds);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update BracketParticipant bp
            set bp.sourceTournamentApplicationId = null
            where bp.sourceTournamentApplicationId in :tournamentApplicationIds
            """)
    int clearSourceTournamentApplicationIds(Collection<Long> tournamentApplicationIds);

    void deleteByBracketRecordId(Long bracketRecordId);
}
