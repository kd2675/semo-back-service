package semo.back.service.database.pub.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import semo.back.service.database.pub.entity.ClubEventParticipant;

public interface ClubEventParticipantRepository extends JpaRepository<ClubEventParticipant, Long> {
    List<ClubEventParticipant> findByEventIdIn(Collection<Long> eventIds);

    Optional<ClubEventParticipant> findByEventIdAndClubProfileId(Long eventId, Long clubProfileId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select participant
            from ClubEventParticipant participant
            where participant.eventId = :eventId
              and participant.clubProfileId = :clubProfileId
            """)
    Optional<ClubEventParticipant> findForUpdateByEventIdAndClubProfileId(
            @Param("eventId") Long eventId,
            @Param("clubProfileId") Long clubProfileId
    );

    @Query("""
            select count(participant)
            from ClubEventParticipant participant, ClubScheduleEvent event
            where participant.eventId = event.eventId
              and event.clubId = :clubId
              and event.eventStatus <> 'CANCELLED'
              and event.participationEnabled = true
              and event.startAt <= :now
              and participant.participationStatus = 'GOING'
              and participant.attendanceStatus is null
            """)
    long countStartedEventAttendancePending(Long clubId, java.time.LocalDateTime now);

    void deleteByEventId(Long eventId);
}
