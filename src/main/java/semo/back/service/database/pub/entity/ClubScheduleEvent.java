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
@Table(name = "club_schedule_event")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ClubScheduleEvent extends CommonDateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_id")
    private Long eventId;

    @Column(name = "club_id", nullable = false)
    private Long clubId;

    @Column(name = "author_club_profile_id", nullable = false)
    private Long authorClubProfileId;

    @Column(name = "linked_notice_id")
    private Long linkedNoticeId;

    @Column(name = "shared_to_board", nullable = false)
    private boolean sharedToBoard;

    @Column(name = "shared_to_calendar", nullable = false)
    private boolean sharedToCalendar;

    @Column(name = "pinned", nullable = false)
    private boolean pinned;

    @Column(name = "category_key", nullable = false, length = 30)
    private String categoryKey;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "location_label", length = 200)
    private String locationLabel;

    @Column(name = "participation_condition_text", length = 1000)
    private String participationConditionText;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at")
    private LocalDateTime endAt;

    @Column(name = "attendee_limit")
    private Integer attendeeLimit;

    @Column(name = "participation_enabled", nullable = false)
    private boolean participationEnabled;

    @Column(name = "fee_required", nullable = false)
    private boolean feeRequired;

    @Column(name = "fee_amount")
    private Integer feeAmount;

    @Column(name = "fee_amount_undecided", nullable = false)
    private boolean feeAmountUndecided;

    @Column(name = "fee_n_way_split", nullable = false)
    private boolean feeNWaySplit;

    @Column(name = "visibility_status", nullable = false, length = 20)
    private String visibilityStatus;

    @Column(name = "event_status", nullable = false, length = 20)
    private String eventStatus;
}
