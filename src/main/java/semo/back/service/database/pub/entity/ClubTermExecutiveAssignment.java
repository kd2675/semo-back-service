package semo.back.service.database.pub.entity;

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
        name = "club_term_executive_assignment",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_club_term_executive_member_position",
                        columnNames = {"club_operating_term_id", "club_member_id", "club_position_id"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ClubTermExecutiveAssignment extends CommonDateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "club_term_executive_assignment_id")
    private Long clubTermExecutiveAssignmentId;

    @Column(name = "club_id", nullable = false)
    private Long clubId;

    @Column(name = "club_operating_term_id", nullable = false)
    private Long clubOperatingTermId;

    @Column(name = "club_member_id", nullable = false)
    private Long clubMemberId;

    @Column(name = "club_profile_id", nullable = false)
    private Long clubProfileId;

    @Column(name = "club_position_id", nullable = false)
    private Long clubPositionId;

    @Column(name = "responsibility", length = 1000)
    private String responsibility;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_by_club_profile_id", nullable = false)
    private Long createdByClubProfileId;

    @Column(name = "updated_by_club_profile_id", nullable = false)
    private Long updatedByClubProfileId;

    public void update(String responsibility, int sortOrder, Long updatedByClubProfileId) {
        this.responsibility = responsibility;
        this.sortOrder = sortOrder;
        this.updatedByClubProfileId = updatedByClubProfileId;
    }
}
