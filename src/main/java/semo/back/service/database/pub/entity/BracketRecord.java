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
@Table(name = "bracket_record")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class BracketRecord extends CommonDateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bracket_record_id")
    private Long bracketRecordId;

    @Column(name = "club_id", nullable = false)
    private Long clubId;

    @Column(name = "author_club_profile_id", nullable = false)
    private Long authorClubProfileId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "summary_text", length = 500)
    private String summaryText;

    @Column(name = "bracket_type", nullable = false, length = 30)
    private String bracketType;

    @Column(name = "participant_type", nullable = false, length = 20)
    private String participantType;

    @Column(name = "source_type", nullable = false, length = 20)
    private String sourceType;

    @Column(name = "source_tournament_record_id")
    private Long sourceTournamentRecordId;

    @Column(name = "approval_status", nullable = false, length = 20)
    private String approvalStatus;

    @Column(name = "reviewed_by_club_profile_id")
    private Long reviewedByClubProfileId;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "participant_count", nullable = false)
    private int participantCount;

    @Column(name = "deleted", nullable = false)
    private boolean deleted;
}
