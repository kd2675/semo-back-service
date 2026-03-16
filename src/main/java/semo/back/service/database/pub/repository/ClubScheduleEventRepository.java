package semo.back.service.database.pub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import semo.back.service.database.pub.entity.ClubScheduleEvent;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Collection;
import java.util.Optional;

public interface ClubScheduleEventRepository extends JpaRepository<ClubScheduleEvent, Long> {
    Optional<ClubScheduleEvent> findByEventIdAndClubId(Long eventId, Long clubId);

    Optional<ClubScheduleEvent> findByLinkedNoticeId(Long linkedNoticeId);

    List<ClubScheduleEvent> findByLinkedNoticeIdIn(Collection<Long> linkedNoticeIds);

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
              and e.startAt < :to
              and (
                    (e.endAt is null and e.startAt >= :from)
                    or (e.endAt is not null and e.endAt >= :from)
                  )
            order by e.startAt asc, e.eventId asc
            """)
    List<ClubScheduleEvent> findScheduledBetween(Long clubId, LocalDateTime from, LocalDateTime to);
}
