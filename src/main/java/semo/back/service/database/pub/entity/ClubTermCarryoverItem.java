package semo.back.service.database.pub.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import semo.back.service.common.jpa.CommonDateEntity;

@Entity
@Table(
        name = "club_term_carryover_item",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_club_term_carryover_resource",
                        columnNames = {"to_term_id", "resource_type", "resource_id"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ClubTermCarryoverItem extends CommonDateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "club_term_carryover_item_id")
    private Long clubTermCarryoverItemId;

    @Column(name = "club_id", nullable = false)
    private Long clubId;

    @Column(name = "from_term_id", nullable = false)
    private Long fromTermId;

    @Column(name = "to_term_id", nullable = false)
    private Long toTermId;

    @Column(name = "resource_type", nullable = false, length = 40)
    private String resourceType;

    @Column(name = "resource_id", nullable = false)
    private Long resourceId;

    @Column(name = "title_snapshot", nullable = false, length = 200)
    private String titleSnapshot;

    @Column(name = "status_snapshot", nullable = false, length = 50)
    private String statusSnapshot;

    @Column(name = "target_path", nullable = false, length = 500)
    private String targetPath;

    @Column(name = "due_at")
    private LocalDateTime dueAt;

    @Column(name = "status_code", nullable = false, length = 20)
    private String statusCode;

    @Column(name = "transferred_by_club_profile_id", nullable = false)
    private Long transferredByClubProfileId;

    @Column(name = "transferred_at", nullable = false)
    private LocalDateTime transferredAt;

    @Column(name = "resolved_by_club_profile_id")
    private Long resolvedByClubProfileId;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    public void resolve(Long actorClubProfileId, LocalDateTime resolvedAt) {
        this.statusCode = "RESOLVED";
        this.resolvedByClubProfileId = actorClubProfileId;
        this.resolvedAt = resolvedAt;
    }

    public void reopen() {
        this.statusCode = "OPEN";
        this.resolvedByClubProfileId = null;
        this.resolvedAt = null;
    }
}
