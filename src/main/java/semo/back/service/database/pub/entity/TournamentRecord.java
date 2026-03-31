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

@Entity
@Table(name = "tournament_record")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class TournamentRecord extends CommonDateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tournament_record_id")
    private Long tournamentRecordId;

    @Column(name = "club_id", nullable = false)
    private Long clubId;

    @Column(name = "author_club_profile_id", nullable = false)
    private Long authorClubProfileId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "summary_text", length = 500)
    private String summaryText;

    @Column(name = "detail_text")
    private String detailText;

    @Column(name = "tournament_status", nullable = false, length = 30)
    private String tournamentStatus;

    @Column(name = "application_start_at", nullable = false)
    private LocalDateTime applicationStartAt;

    @Column(name = "application_end_at", nullable = false)
    private LocalDateTime applicationEndAt;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "location_label", length = 200)
    private String locationLabel;

    @Column(name = "match_format", nullable = false, length = 20)
    private String matchFormat;

    @Column(name = "team_member_limit")
    private Integer teamMemberLimit;

    @Column(name = "participant_limit")
    private Integer participantLimit;

    @Column(name = "fee_required", nullable = false)
    private boolean feeRequired;

    @Column(name = "fee_amount")
    private Integer feeAmount;

    @Column(name = "fee_currency_code", nullable = false, length = 10)
    private String feeCurrencyCode;

    @Column(name = "shared_to_board", nullable = false)
    private boolean sharedToBoard;

    @Column(name = "shared_to_calendar", nullable = false)
    private boolean sharedToCalendar;

    @Column(name = "pinned", nullable = false)
    private boolean pinned;

    @Column(name = "bracket_mode", nullable = false, length = 20)
    private String bracketMode;

    @Column(name = "bracket_confirmed", nullable = false)
    private boolean bracketConfirmed;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancel_reason", length = 500)
    private String cancelReason;

    @Column(name = "deleted", nullable = false)
    private boolean deleted;
}
