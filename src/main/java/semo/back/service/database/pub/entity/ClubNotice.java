package semo.back.service.database.pub.entity;

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

import java.time.LocalDateTime;

@Entity
@Table(name = "club_notice")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ClubNotice extends CommonDateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notice_id")
    private Long noticeId;

    @Column(name = "club_id", nullable = false)
    private Long clubId;

    @Column(name = "author_club_profile_id", nullable = false)
    private Long authorClubProfileId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "image_file_name", length = 255)
    private String imageFileName;

    @Column(name = "location_label", length = 200)
    private String locationLabel;

    @Column(name = "schedule_at")
    private LocalDateTime scheduleAt;

    @Column(name = "schedule_end_at")
    private LocalDateTime scheduleEndAt;

    @Column(name = "shared_to_board", nullable = false)
    private boolean sharedToBoard;

    @Column(name = "shared_to_calendar", nullable = false)
    private boolean sharedToCalendar;

    @Column(name = "pinned", nullable = false)
    private boolean pinned;

    @Column(name = "published_at", nullable = false)
    private LocalDateTime publishedAt;

    @Column(name = "deleted", nullable = false)
    private boolean deleted;
}
