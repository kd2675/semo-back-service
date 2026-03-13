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
@Table(name = "club_member")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ClubMember extends CommonDateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "club_member_id")
    private Long clubMemberId;

    @Column(name = "club_id", nullable = false)
    private Long clubId;

    @Column(name = "profile_id", nullable = false)
    private Long profileId;

    @Column(name = "role_code", nullable = false, length = 20)
    private String roleCode;

    @Column(name = "membership_status", nullable = false, length = 20)
    private String membershipStatus;

    @Column(name = "join_message", length = 500)
    private String joinMessage;

    @Column(name = "invited_by_profile_id")
    private Long invitedByProfileId;

    @Column(name = "joined_at")
    private LocalDateTime joinedAt;

    @Column(name = "last_activity_at")
    private LocalDateTime lastActivityAt;
}
