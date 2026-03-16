package semo.back.service.database.pub.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import semo.back.service.common.jpa.CommonDateEntity;

@Entity
@Table(name = "notice_category_catalog")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class NoticeCategoryCatalog extends CommonDateEntity {
    @Id
    @Column(name = "category_key", nullable = false, length = 30)
    private String categoryKey;

    @Column(name = "display_name", nullable = false, length = 60)
    private String displayName;

    @Column(name = "icon_name", nullable = false, length = 50)
    private String iconName;

    @Column(name = "accent_tone", nullable = false, length = 30)
    private String accentTone;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
}
