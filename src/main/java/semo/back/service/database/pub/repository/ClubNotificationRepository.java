package semo.back.service.database.pub.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import semo.back.service.database.pub.entity.ClubNotification;

public interface ClubNotificationRepository extends JpaRepository<ClubNotification, Long> {
    boolean existsByEventKey(String eventKey);

    Optional<ClubNotification> findByClubNotificationIdAndProfileId(Long clubNotificationId, Long profileId);

    long countByProfileIdAndReadAtIsNull(Long profileId);

    @Query("""
            select notification
            from ClubNotification notification
            where notification.profileId = :profileId
              and (:unreadOnly = false or notification.readAt is null)
              and (:beforeId is null or notification.clubNotificationId < :beforeId)
            order by notification.clubNotificationId desc
            """)
    List<ClubNotification> findFeed(
            Long profileId,
            boolean unreadOnly,
            Long beforeId,
            Pageable pageable
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update ClubNotification notification
            set notification.readAt = :readAt
              , notification.updateDate = :readAt
            where notification.profileId = :profileId
              and notification.readAt is null
            """)
    int markAllRead(Long profileId, java.time.LocalDateTime readAt);
}
