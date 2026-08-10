package semo.back.service.database.pub.repository;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import semo.back.service.database.pub.entity.TournamentScheduleSlot;

public interface TournamentScheduleSlotRepository extends JpaRepository<TournamentScheduleSlot, Long> {
    List<TournamentScheduleSlot> findByTournamentRecordIdOrderByStartAtAscTournamentScheduleSlotIdAsc(Long tournamentRecordId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select slot from TournamentScheduleSlot slot
            where slot.tournamentRecordId = :tournamentRecordId
              and slot.tournamentScheduleSlotId = :slotId
            """)
    Optional<TournamentScheduleSlot> findForUpdate(Long tournamentRecordId, Long slotId);

    void deleteByTournamentRecordId(Long tournamentRecordId);
}
