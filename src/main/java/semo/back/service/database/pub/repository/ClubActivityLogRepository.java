package semo.back.service.database.pub.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import semo.back.service.database.pub.entity.ClubActivityLog;

import java.util.Collection;
import java.time.LocalDateTime;
import java.util.List;

public interface ClubActivityLogRepository extends JpaRepository<ClubActivityLog, Long> {
    List<ClubActivityLog> findByClubIdOrderByCreateDateDescClubActivityLogIdDesc(Long clubId, Pageable pageable);

    @Query("""
            select a
            from ClubActivityLog a
            where a.clubId = :clubId
              and a.actorClubProfileId in :actorClubProfileIds
              and a.actorClubProfileId is not null
              and not exists (
                    select 1
                    from ClubActivityLog newer
                    where newer.clubId = a.clubId
                      and newer.actorClubProfileId = a.actorClubProfileId
                      and (
                            newer.createDate > a.createDate
                            or (newer.createDate = a.createDate and newer.clubActivityLogId > a.clubActivityLogId)
                          )
                  )
            order by a.createDate desc, a.clubActivityLogId desc
            """)
    List<ClubActivityLog> findLatestByClubIdAndActorClubProfileIdIn(
            Long clubId,
            Collection<Long> actorClubProfileIds
    );

    @Query("""
            select a
            from ClubActivityLog a
            where a.clubId = :clubId
              and (
                    :cursorCreatedAt is null
                    or a.createDate < :cursorCreatedAt
                    or (a.createDate = :cursorCreatedAt and a.clubActivityLogId < :cursorActivityId)
                  )
            order by a.createDate desc, a.clubActivityLogId desc
            """)
    List<ClubActivityLog> findFeed(
            Long clubId,
            LocalDateTime cursorCreatedAt,
            Long cursorActivityId,
            Pageable pageable
    );

    @Query("""
            select a
            from ClubActivityLog a
            where a.clubId = :clubId
              and exists (
                    select 1
                    from ClubMemberPositionHistory h
                    where h.clubId = a.clubId
                      and h.clubMemberId = a.actorClubMemberId
                      and h.clubPositionId = :clubPositionId
                      and h.deleted = false
                      and h.startedAt <= a.createDate
                      and (h.endedAt is null or h.endedAt >= a.createDate)
                  )
              and (
                    :cursorCreatedAt is null
                    or a.createDate < :cursorCreatedAt
                    or (a.createDate = :cursorCreatedAt and a.clubActivityLogId < :cursorActivityId)
                  )
            order by a.createDate desc, a.clubActivityLogId desc
            """)
    List<ClubActivityLog> findFeedByPosition(
            Long clubId,
            Long clubPositionId,
            LocalDateTime cursorCreatedAt,
            Long cursorActivityId,
            Pageable pageable
    );

    @Query("""
            select a
            from ClubActivityLog a
            where a.clubId = :clubId
              and a.actorClubProfileId = :actorClubProfileId
              and (
                    :cursorCreatedAt is null
                    or a.createDate < :cursorCreatedAt
                    or (a.createDate = :cursorCreatedAt and a.clubActivityLogId < :cursorActivityId)
                  )
            order by a.createDate desc, a.clubActivityLogId desc
            """)
    List<ClubActivityLog> findActorFeed(
            Long clubId,
            Long actorClubProfileId,
            LocalDateTime cursorCreatedAt,
            Long cursorActivityId,
            Pageable pageable
    );
}
