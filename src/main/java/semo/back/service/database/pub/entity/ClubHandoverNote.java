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
@Table(name = "club_handover_note")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ClubHandoverNote extends CommonDateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "club_handover_note_id")
    private Long clubHandoverNoteId;

    @Column(name = "club_id", nullable = false)
    private Long clubId;

    @Column(name = "from_term_id")
    private Long fromTermId;

    @Column(name = "to_term_id")
    private Long toTermId;

    @Column(name = "club_position_id")
    private Long clubPositionId;

    @Column(name = "assigned_club_profile_id")
    private Long assignedClubProfileId;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "status_code", nullable = false, length = 20)
    private String statusCode;

    @Column(name = "due_at")
    private LocalDateTime dueAt;

    @Column(name = "created_by_club_profile_id", nullable = false)
    private Long createdByClubProfileId;

    @Column(name = "acknowledged_by_club_profile_id")
    private Long acknowledgedByClubProfileId;

    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;

    @Column(name = "deleted", nullable = false)
    private boolean deleted;

    @Column(name = "deleted_by_club_profile_id")
    private Long deletedByClubProfileId;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public void update(
            Long fromTermId,
            Long toTermId,
            Long clubPositionId,
            Long assignedClubProfileId,
            String title,
            String content,
            String statusCode,
            LocalDateTime dueAt
    ) {
        this.fromTermId = fromTermId;
        this.toTermId = toTermId;
        this.clubPositionId = clubPositionId;
        this.assignedClubProfileId = assignedClubProfileId;
        this.title = title;
        this.content = content;
        this.statusCode = statusCode;
        this.dueAt = dueAt;
        if (!"ACKNOWLEDGED".equals(statusCode)) {
            this.acknowledgedByClubProfileId = null;
            this.acknowledgedAt = null;
        }
    }

    public void acknowledge(Long actorClubProfileId, LocalDateTime acknowledgedAt) {
        this.statusCode = "ACKNOWLEDGED";
        this.acknowledgedByClubProfileId = actorClubProfileId;
        this.acknowledgedAt = acknowledgedAt;
    }

    public void markDeleted(Long actorClubProfileId, LocalDateTime deletedAt) {
        this.deleted = true;
        this.deletedByClubProfileId = actorClubProfileId;
        this.deletedAt = deletedAt;
    }
}
