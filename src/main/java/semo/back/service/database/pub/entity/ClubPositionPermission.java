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
        name = "club_position_permission",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_club_position_permission", columnNames = {"club_position_id", "permission_key"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ClubPositionPermission extends CommonDateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "club_position_permission_id")
    private Long clubPositionPermissionId;

    @Column(name = "club_position_id", nullable = false)
    private Long clubPositionId;

    @Column(name = "permission_key", nullable = false, length = 80)
    private String permissionKey;
}
