package semo.back.service.database.pub.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import semo.back.service.database.pub.entity.ClubNotice;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ClubNoticeRepository extends JpaRepository<ClubNotice, Long> {
    Optional<ClubNotice> findByNoticeIdAndClubIdAndDeletedFalse(Long noticeId, Long clubId);

    List<ClubNotice> findAllByClubIdAndDeletedFalseOrderByPublishedAtDescNoticeIdDesc(Long clubId);

    List<ClubNotice> findAllByClubIdAndSharedToScheduleTrueAndDeletedFalseOrderByPublishedAtDescNoticeIdDesc(Long clubId);

    @Query("""
            select n
            from ClubNotice n
            where n.clubId = :clubId
              and n.authorClubProfileId = :authorClubProfileId
              and n.deleted = false
              and n.noticeId not in (
                    select e.linkedNoticeId
                    from ClubScheduleEvent e
                    where e.linkedNoticeId is not null
              )
              and n.noticeId not in (
                    select v.linkedNoticeId
                    from ClubScheduleVote v
                    where v.linkedNoticeId is not null
              )
            order by n.publishedAt desc, n.noticeId desc
            """)
    List<ClubNotice> findDirectNoticesByClubIdAndAuthorClubProfileIdOrderByPublishedAtDescNoticeIdDesc(
            Long clubId,
            Long authorClubProfileId
    );

    @Query("""
            select n
            from ClubNotice n
            where n.clubId = :clubId
              and n.deleted = false
              and (
                    :includePollLinkedNotices = true
                    or n.noticeId not in (
                        select v.linkedNoticeId
                        from ClubScheduleVote v
                        where v.linkedNoticeId is not null
                    )
                  )
              and (:categoryKey is null or n.categoryKey = :categoryKey)
              and (
                    :queryText is null
                    or lower(n.title) like lower(concat('%', :queryText, '%'))
                    or lower(n.content) like lower(concat('%', :queryText, '%'))
                  )
              and (
                    :cursorPublishedAt is null
                    or n.publishedAt < :cursorPublishedAt
                    or (n.publishedAt = :cursorPublishedAt and n.noticeId < :cursorNoticeId)
                  )
            order by n.publishedAt desc, n.noticeId desc
            """)
    List<ClubNotice> findFeed(
            Long clubId,
            boolean includePollLinkedNotices,
            String categoryKey,
            String queryText,
            LocalDateTime cursorPublishedAt,
            Long cursorNoticeId,
            Pageable pageable
    );

    @Query("""
            select n
            from ClubNotice n
            where n.clubId = :clubId
              and n.deleted = false
              and (
                    :includePollLinkedNotices = true
                    or n.noticeId not in (
                        select v.linkedNoticeId
                        from ClubScheduleVote v
                        where v.linkedNoticeId is not null
                    )
                  )
              and n.categoryKey in :visibleCategoryKeys
              and (:selectedCategoryKey is null or n.categoryKey = :selectedCategoryKey)
              and (
                    :cursorPublishedAt is null
                    or n.publishedAt < :cursorPublishedAt
                    or (n.publishedAt = :cursorPublishedAt and n.noticeId < :cursorNoticeId)
                  )
            order by n.publishedAt desc, n.noticeId desc
            """)
    List<ClubNotice> findTimelineFeed(
            Long clubId,
            boolean includePollLinkedNotices,
            Collection<String> visibleCategoryKeys,
            String selectedCategoryKey,
            LocalDateTime cursorPublishedAt,
            Long cursorNoticeId,
            Pageable pageable
    );

    @Query("""
            select n
            from ClubNotice n
            where n.clubId = :clubId
              and n.deleted = false
              and n.scheduleAt is not null
              and n.scheduleAt >= :from
              and n.scheduleAt < :to
            order by n.scheduleAt asc, n.noticeId asc
            """)
    List<ClubNotice> findScheduledBetween(Long clubId, LocalDateTime from, LocalDateTime to);
}
