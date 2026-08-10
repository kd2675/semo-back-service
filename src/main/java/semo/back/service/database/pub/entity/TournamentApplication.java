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
@Table(name = "tournament_application")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class TournamentApplication extends CommonDateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tournament_application_id")
    private Long tournamentApplicationId;

    @Column(name = "tournament_record_id", nullable = false)
    private Long tournamentRecordId;

    @Column(name = "club_profile_id", nullable = false)
    private Long clubProfileId;

    @Column(name = "application_status", nullable = false, length = 20)
    private String applicationStatus;

    @Column(name = "application_note", length = 500)
    private String applicationNote;

    @Column(name = "team_name", length = 100)
    private String teamName;

    @Column(name = "waitlist_position")
    private Integer waitlistPosition;

    @Column(name = "finance_payment_id")
    private Long financePaymentId;

    @Column(name = "checked_in_at")
    private LocalDateTime checkedInAt;

    @Column(name = "checked_in_by_club_profile_id")
    private Long checkedInByClubProfileId;

    @Column(name = "placement")
    private Integer placement;

    @Column(name = "result_note", length = 1000)
    private String resultNote;

    @Column(name = "reviewed_by_club_profile_id")
    private Long reviewedByClubProfileId;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    public void markApplied(String applicationStatus, String applicationNote, String teamName, Integer waitlistPosition) {
        this.applicationStatus = applicationStatus;
        this.applicationNote = applicationNote;
        this.teamName = teamName;
        this.waitlistPosition = waitlistPosition;
        this.checkedInAt = null;
        this.checkedInByClubProfileId = null;
        this.placement = null;
        this.resultNote = null;
        this.reviewedByClubProfileId = null;
        this.reviewedAt = null;
    }

    public void review(String applicationStatus, Long reviewedByClubProfileId, LocalDateTime reviewedAt) {
        this.applicationStatus = applicationStatus;
        if (!"WAITLISTED".equals(applicationStatus)) {
            this.waitlistPosition = null;
        }
        this.reviewedByClubProfileId = reviewedByClubProfileId;
        this.reviewedAt = reviewedAt;
    }

    public void updateWaitlistPosition(Integer waitlistPosition) {
        this.waitlistPosition = waitlistPosition;
    }

    public void linkFinancePayment(Long financePaymentId) {
        this.financePaymentId = financePaymentId;
    }

    public void updateOperations(
            LocalDateTime checkedInAt,
            Long checkedInByClubProfileId,
            Integer placement,
            String resultNote
    ) {
        this.checkedInAt = checkedInAt;
        this.checkedInByClubProfileId = checkedInByClubProfileId;
        this.placement = placement;
        this.resultNote = resultNote;
    }
}
