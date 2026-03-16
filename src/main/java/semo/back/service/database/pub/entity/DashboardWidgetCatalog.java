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
@Table(name = "dashboard_widget_catalog")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class DashboardWidgetCatalog extends CommonDateEntity {
    @Id
    @Column(name = "widget_key", nullable = false, length = 50)
    private String widgetKey;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "icon_name", nullable = false, length = 50)
    private String iconName;

    @Column(name = "required_feature_key", length = 50)
    private String requiredFeatureKey;

    @Column(name = "default_visibility_scope", nullable = false, length = 20)
    private String defaultVisibilityScope;

    @Column(name = "default_column_span", nullable = false)
    private int defaultColumnSpan;

    @Column(name = "default_row_span", nullable = false)
    private int defaultRowSpan;

    @Column(name = "default_sort_order", nullable = false)
    private int defaultSortOrder;

    @Column(name = "active", nullable = false)
    private boolean active;
}
