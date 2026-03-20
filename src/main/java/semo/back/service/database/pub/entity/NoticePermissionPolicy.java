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
        name = "notice_permission_policy",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_notice_permission_policy_club", columnNames = "club_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class NoticePermissionPolicy extends CommonDateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notice_permission_policy_id")
    private Long noticePermissionPolicyId;

    @Column(name = "club_id", nullable = false)
    private Long clubId;

    @Column(name = "allow_member_create", nullable = false)
    private boolean allowMemberCreate;

    @Column(name = "allow_member_update", nullable = false)
    private boolean allowMemberUpdate;

    @Column(name = "allow_member_delete", nullable = false)
    private boolean allowMemberDelete;
}
