package semo.back.service.database.pub.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import semo.back.service.common.jpa.CommonDateEntity;

@Entity
@Table(name = "club_notification")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ClubNotification extends CommonDateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "club_notification_id")
    private Long clubNotificationId;

    @Column(name = "profile_id", nullable = false)
    private Long profileId;

    @Column(name = "club_id")
    private Long clubId;

    @Column(name = "notification_type", nullable = false, length = 50)
    private String notificationType;

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Column(name = "message", nullable = false, length = 500)
    private String message;

    @Column(name = "resource_type", length = 50)
    private String resourceType;

    @Column(name = "resource_id")
    private Long resourceId;

    @Column(name = "target_path", length = 500)
    private String targetPath;

    @Column(name = "event_key", nullable = false, unique = true, length = 190)
    private String eventKey;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    public void markRead(LocalDateTime markedAt) {
        if (readAt == null) {
            readAt = markedAt;
        }
    }
}
