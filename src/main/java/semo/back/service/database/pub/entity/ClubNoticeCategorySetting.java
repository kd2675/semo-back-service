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
@Table(name = "club_notice_category_setting")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ClubNoticeCategorySetting extends CommonDateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "club_notice_category_setting_id")
    private Long clubNoticeCategorySettingId;

    @Column(name = "club_id", nullable = false)
    private Long clubId;

    @Column(name = "category_key", nullable = false, length = 30)
    private String categoryKey;

    @Column(name = "visible_in_timeline", nullable = false)
    private boolean visibleInTimeline;

    @Column(name = "updated_by_club_profile_id")
    private Long updatedByClubProfileId;
}
