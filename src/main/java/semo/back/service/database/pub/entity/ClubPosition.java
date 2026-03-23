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
        name = "club_position",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_club_position_code", columnNames = {"club_id", "position_code"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ClubPosition extends CommonDateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "club_position_id")
    private Long clubPositionId;

    @Column(name = "club_id", nullable = false)
    private Long clubId;

    @Column(name = "position_code", nullable = false, length = 50)
    private String positionCode;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "icon_name", length = 50)
    private String iconName;

    @Column(name = "color_hex", length = 20)
    private String colorHex;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_by_club_profile_id")
    private Long createdByClubProfileId;
}
