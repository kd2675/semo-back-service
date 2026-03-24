package semo.back.service.database.pub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import semo.back.service.database.pub.entity.ClubBoardItem;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;

public interface ClubBoardItemRepository extends JpaRepository<ClubBoardItem, Long> {
    Optional<ClubBoardItem> findByClubIdAndContentTypeAndContentId(Long clubId, String contentType, Long contentId);

    List<ClubBoardItem> findAllByClubIdAndContentTypeOrderByBoardItemIdDesc(Long clubId, String contentType);

    @Query("""
            select bi
            from ClubBoardItem bi
            left join ClubNotice n
              on bi.contentType = 'NOTICE'
             and n.noticeId = bi.contentId
             and n.clubId = bi.clubId
            left join ClubScheduleEvent e
              on bi.contentType = 'SCHEDULE_EVENT'
             and e.eventId = bi.contentId
             and e.clubId = bi.clubId
            left join ClubScheduleVote v
              on bi.contentType = 'SCHEDULE_VOTE'
             and v.voteId = bi.contentId
             and v.clubId = bi.clubId
            where bi.clubId = :clubId
              and (
                    (bi.contentType = 'NOTICE' and n.noticeId is not null and n.deleted = false)
                    or (bi.contentType = 'SCHEDULE_EVENT' and e.eventId is not null and e.eventStatus <> 'CANCELLED')
                    or (bi.contentType = 'SCHEDULE_VOTE' and v.voteId is not null)
                  )
              and (
                    :queryText is null
                    or (bi.contentType = 'NOTICE' and (
                        lower(n.title) like lower(concat('%', :queryText, '%'))
                        or lower(n.content) like lower(concat('%', :queryText, '%'))
                    ))
                    or (bi.contentType = 'SCHEDULE_EVENT' and lower(e.title) like lower(concat('%', :queryText, '%')))
                    or (bi.contentType = 'SCHEDULE_VOTE' and lower(v.title) like lower(concat('%', :queryText, '%')))
                  )
              and (
                    :pinnedOnly = false
                    or (bi.contentType = 'NOTICE' and n.pinned = true)
                    or (bi.contentType = 'SCHEDULE_EVENT' and e.pinned = true)
                    or (bi.contentType = 'SCHEDULE_VOTE' and v.pinned = true)
                  )
              and (
                    :cursorBoardItemId is null
                    or bi.boardItemId < :cursorBoardItemId
                  )
            order by bi.boardItemId desc
            """)
    List<ClubBoardItem> findFeedItems(
            Long clubId,
            String queryText,
            boolean pinnedOnly,
            Long cursorBoardItemId,
            Pageable pageable
    );

    void deleteByClubIdAndContentTypeAndContentId(Long clubId, String contentType, Long contentId);
}
