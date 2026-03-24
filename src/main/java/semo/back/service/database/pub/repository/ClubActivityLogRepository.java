package semo.back.service.database.pub.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import semo.back.service.database.pub.entity.ClubActivityLog;

import java.time.LocalDateTime;
import java.util.List;

public interface ClubActivityLogRepository extends JpaRepository<ClubActivityLog, Long> {
    List<ClubActivityLog> findByClubIdOrderByCreateDateDescClubActivityLogIdDesc(Long clubId, Pageable pageable);

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
