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

import java.time.LocalDateTime;

@Entity
@Table(
        name = "club_member_position",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_club_member_position", columnNames = {"club_member_id", "club_position_id"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ClubMemberPosition extends CommonDateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "club_member_position_id")
    private Long clubMemberPositionId;

    @Column(name = "club_member_id", nullable = false)
    private Long clubMemberId;

    @Column(name = "club_position_id", nullable = false)
    private Long clubPositionId;

    @Column(name = "assigned_by_club_profile_id")
    private Long assignedByClubProfileId;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;
}
