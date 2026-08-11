package semo.back.service.database.pub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import semo.back.service.database.pub.entity.ClubScheduleEvent;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;

public interface ClubScheduleEventRepository extends JpaRepository<ClubScheduleEvent, Long> {
    Optional<ClubScheduleEvent> findByEventIdAndClubId(Long eventId, Long clubId);

    Optional<ClubScheduleEvent> findByLinkedNoticeId(Long linkedNoticeId);

    List<ClubScheduleEvent> findByLinkedNoticeIdIn(Collection<Long> linkedNoticeIds);

    List<ClubScheduleEvent> findAllByEventIdIn(Collection<Long> eventIds);

    @Query("""
            select e
            from ClubScheduleEvent e
            where e.clubId = :clubId
              and e.sharedToBoard = true
              and e.eventStatus <> 'CANCELLED'
            order by e.startAt desc, e.eventId desc
            """)
    List<ClubScheduleEvent> findAllByClubIdAndSharedToBoardTrue(Long clubId);

    @Query("""
            select e
            from ClubScheduleEvent e
            where e.clubId = :clubId
              and e.eventStatus <> 'CANCELLED'
            order by e.startAt asc, e.eventId asc
            """)
    List<ClubScheduleEvent> findAllActiveEvents(Long clubId);

    @Query("""
            select e
            from ClubScheduleEvent e
            where e.clubId = :clubId
              and e.eventStatus <> 'CANCELLED'
            order by e.startAt desc, e.eventId desc
            """)
    List<ClubScheduleEvent> findRecentActiveEvents(Long clubId, Pageable pageable);

    @Query("""
            select e
            from ClubScheduleEvent e
            where e.clubId = :clubId
              and e.eventStatus <> 'CANCELLED'
              and e.startAt >= :from
            order by e.startAt asc, e.eventId asc
            """)
    List<ClubScheduleEvent> findUpcomingActiveEvents(Long clubId, LocalDateTime from, Pageable pageable);

    @Query("""
            select count(e)
            from ClubScheduleEvent e
            where e.clubId = :clubId
              and e.eventStatus <> 'CANCELLED'
              and e.startAt >= :from
            """)
    long countUpcomingActiveEvents(Long clubId, LocalDateTime from);

    @Query("""
            select count(e)
            from ClubScheduleEvent e
            where e.clubId = :clubId
              and e.eventStatus <> 'CANCELLED'
              and e.startAt >= :from
              and e.startAt < :toExclusive
            """)
    long countActiveEventsWithinTerm(Long clubId, LocalDateTime from, LocalDateTime toExclusive);

    @Query("""
            select e
            from ClubScheduleEvent e
            where e.clubId = :clubId
              and e.eventStatus <> 'CANCELLED'
              and e.startAt < :to
              and (
                    (e.endAt is null and e.startAt >= :from)
                    or (e.endAt is not null and e.endAt >= :from)
                  )
            order by e.startAt asc, e.eventId asc
            """)
    List<ClubScheduleEvent> findScheduledBetween(Long clubId, LocalDateTime from, LocalDateTime to);

    @Query("""
            select count(event)
            from ClubScheduleEvent event
            where event.clubId = :clubId
              and event.eventStatus <> 'CANCELLED'
              and event.participationEnabled = true
              and event.startAt >= :from
              and not exists (
                    select participant.clubEventParticipantId
                    from ClubEventParticipant participant
                    where participant.eventId = event.eventId
                      and participant.clubProfileId = :clubProfileId
                      and participant.participationStatus <> 'CANCELED'
                  )
            """)
    long countPendingParticipationResponses(Long clubId, Long clubProfileId, LocalDateTime from);
}
