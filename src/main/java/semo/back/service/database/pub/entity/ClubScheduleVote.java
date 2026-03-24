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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "club_schedule_vote")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ClubScheduleVote extends CommonDateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vote_id")
    private Long voteId;

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

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "vote_start_date", nullable = false)
    private LocalDate voteStartDate;

    @Column(name = "vote_end_date", nullable = false)
    private LocalDate voteEndDate;

    @Column(name = "vote_start_time")
    private LocalTime voteStartTime;

    @Column(name = "vote_end_time")
    private LocalTime voteEndTime;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;
}
