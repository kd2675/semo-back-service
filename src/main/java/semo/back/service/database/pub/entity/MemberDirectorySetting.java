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

@Entity
@Table(name = "member_directory_setting")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class MemberDirectorySetting extends CommonDateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_directory_setting_id")
    private Long memberDirectorySettingId;

    @Column(name = "club_id", nullable = false, unique = true)
    private Long clubId;

    @Column(name = "show_positions", nullable = false)
    private boolean showPositions;

    @Column(name = "show_tagline", nullable = false)
    private boolean showTagline;

    @Column(name = "show_recent_activity", nullable = false)
    private boolean showRecentActivity;

    @Column(name = "updated_by_club_profile_id")
    private Long updatedByClubProfileId;
}
