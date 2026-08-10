package semo.back.service.database.pub.entity;

import java.time.LocalDate;
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
@Table(name = "decision_record")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class DecisionRecord extends CommonDateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "decision_record_id")
    private Long decisionRecordId;

    @Column(name = "club_id", nullable = false)
    private Long clubId;

    @Column(name = "club_operating_term_id")
    private Long clubOperatingTermId;

    @Column(name = "record_type", nullable = false, length = 30)
    private String recordType;

    @Column(name = "status_code", nullable = false, length = 20)
    private String statusCode;

    @Column(name = "visibility_scope", nullable = false, length = 20)
    private String visibilityScope;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "decision_content", nullable = false, columnDefinition = "TEXT")
    private String decisionContent;

    @Column(name = "background_context", columnDefinition = "TEXT")
    private String backgroundContext;

    @Column(name = "rationale", columnDefinition = "TEXT")
    private String rationale;

    @Column(name = "meeting_at")
    private LocalDateTime meetingAt;

    @Column(name = "effective_date")
    private LocalDate effectiveDate;

    @Column(name = "review_date")
    private LocalDate reviewDate;

    @Column(name = "supersedes_decision_record_id")
    private Long supersedesDecisionRecordId;

    @Column(name = "created_by_club_profile_id", nullable = false)
    private Long createdByClubProfileId;

    @Column(name = "confirmed_by_club_profile_id")
    private Long confirmedByClubProfileId;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "archived_by_club_profile_id")
    private Long archivedByClubProfileId;

    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    @Column(name = "deleted", nullable = false)
    private boolean deleted;

    @Column(name = "deleted_by_club_profile_id")
    private Long deletedByClubProfileId;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public void updateDraft(
            Long clubOperatingTermId,
            String recordType,
            String visibilityScope,
            String title,
            String decisionContent,
            String backgroundContext,
            String rationale,
            LocalDateTime meetingAt,
            LocalDate effectiveDate,
            LocalDate reviewDate,
            Long supersedesDecisionRecordId
    ) {
        this.clubOperatingTermId = clubOperatingTermId;
        this.recordType = recordType;
        this.visibilityScope = visibilityScope;
        this.title = title;
        this.decisionContent = decisionContent;
        this.backgroundContext = backgroundContext;
        this.rationale = rationale;
        this.meetingAt = meetingAt;
        this.effectiveDate = effectiveDate;
        this.reviewDate = reviewDate;
        this.supersedesDecisionRecordId = supersedesDecisionRecordId;
    }

    public void confirm(Long actorClubProfileId, LocalDateTime confirmedAt) {
        this.statusCode = "CONFIRMED";
        this.confirmedByClubProfileId = actorClubProfileId;
        this.confirmedAt = confirmedAt;
    }

    public void markSuperseded() {
        this.statusCode = "SUPERSEDED";
    }

    public void archive(Long actorClubProfileId, LocalDateTime archivedAt) {
        this.statusCode = "ARCHIVED";
        this.archivedByClubProfileId = actorClubProfileId;
        this.archivedAt = archivedAt;
    }

    public void markDeleted(Long actorClubProfileId, LocalDateTime deletedAt) {
        this.deleted = true;
        this.deletedByClubProfileId = actorClubProfileId;
        this.deletedAt = deletedAt;
    }
}
