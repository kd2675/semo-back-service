package semo.back.service.database.pub.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import semo.back.service.database.pub.entity.ClubCalendarItem;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ClubCalendarItemRepository extends JpaRepository<ClubCalendarItem, Long> {
    Optional<ClubCalendarItem> findByClubIdAndContentTypeAndContentId(Long clubId, String contentType, Long contentId);

    List<ClubCalendarItem> findAllByClubIdAndContentTypeOrderByCalendarItemIdDesc(Long clubId, String contentType);

    @Query("""
            select ci
            from ClubCalendarItem ci
            left join ClubNotice n
              on ci.contentType = 'NOTICE'
             and n.noticeId = ci.contentId
             and n.clubId = ci.clubId
            left join ClubScheduleEvent e
              on ci.contentType = 'SCHEDULE_EVENT'
             and e.eventId = ci.contentId
             and e.clubId = ci.clubId
            left join ClubScheduleVote v
              on ci.contentType = 'SCHEDULE_VOTE'
             and v.voteId = ci.contentId
             and v.clubId = ci.clubId
            where ci.clubId = :clubId
              and (
                    (ci.contentType = 'NOTICE'
                        and n.noticeId is not null
                        and n.deleted = false
                        and n.scheduleAt is not null
                        and n.scheduleAt < :monthEndExclusive
                        and (
                            (n.scheduleEndAt is null and n.scheduleAt >= :monthStartAt)
                            or (n.scheduleEndAt is not null and n.scheduleEndAt >= :monthStartAt)
                        ))
                    or (ci.contentType = 'SCHEDULE_EVENT'
                        and e.eventId is not null
                        and e.eventStatus <> 'CANCELLED'
                        and e.startAt < :monthEndExclusive
                        and (
                            (e.endAt is null and e.startAt >= :monthStartAt)
                            or (e.endAt is not null and e.endAt >= :monthStartAt)
                        ))
                    or (ci.contentType = 'SCHEDULE_VOTE'
                        and v.voteId is not null
                        and v.voteStartDate <= :monthEndDate
                        and v.voteEndDate >= :monthStartDate)
                  )
            order by ci.calendarItemId desc
            """)
    List<ClubCalendarItem> findMonthItems(
            Long clubId,
            LocalDateTime monthStartAt,
            LocalDateTime monthEndExclusive,
            LocalDate monthStartDate,
            LocalDate monthEndDate
    );

    void deleteByClubIdAndContentTypeAndContentId(Long clubId, String contentType, Long contentId);
}
