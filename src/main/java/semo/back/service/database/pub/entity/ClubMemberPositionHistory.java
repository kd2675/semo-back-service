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
@Table(name = "club_member_position_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ClubMemberPositionHistory extends CommonDateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "club_member_position_history_id")
    private Long clubMemberPositionHistoryId;

    @Column(name = "club_id", nullable = false)
    private Long clubId;

    @Column(name = "club_member_id", nullable = false)
    private Long clubMemberId;

    @Column(name = "club_profile_id")
    private Long clubProfileId;

    @Column(name = "club_position_id", nullable = false)
    private Long clubPositionId;

    @Column(name = "position_code_snapshot", nullable = false, length = 50)
    private String positionCodeSnapshot;

    @Column(name = "position_display_name_snapshot", nullable = false, length = 100)
    private String positionDisplayNameSnapshot;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "assigned_by_club_profile_id")
    private Long assignedByClubProfileId;

    @Column(name = "ended_by_club_profile_id")
    private Long endedByClubProfileId;

    @Column(name = "deleted", nullable = false)
    private boolean deleted;

    @Column(name = "deleted_by_club_profile_id")
    private Long deletedByClubProfileId;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "delete_reason", length = 500)
    private String deleteReason;

    public void close(Long endedByClubProfileId, LocalDateTime endedAt) {
        if (this.endedAt != null) {
            return;
        }
        this.endedByClubProfileId = endedByClubProfileId;
        this.endedAt = endedAt;
    }

    public void markDeleted(Long deletedByClubProfileId, LocalDateTime deletedAt, String deleteReason) {
        this.deleted = true;
        this.deletedByClubProfileId = deletedByClubProfileId;
        this.deletedAt = deletedAt;
        this.deleteReason = deleteReason;
    }
}
