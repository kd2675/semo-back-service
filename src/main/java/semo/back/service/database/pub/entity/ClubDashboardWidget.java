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
@Table(name = "club_dashboard_widget")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ClubDashboardWidget extends CommonDateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "club_dashboard_widget_id")
    private Long clubDashboardWidgetId;

    @Column(name = "club_id", nullable = false)
    private Long clubId;

    @Column(name = "widget_key", nullable = false, length = 50)
    private String widgetKey;

    @Column(name = "title_override", length = 100)
    private String titleOverride;

    @Column(name = "column_span", nullable = false)
    private int columnSpan;

    @Column(name = "row_span", nullable = false)
    private int rowSpan;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @Column(name = "visibility_scope", nullable = false, length = 20)
    private String visibilityScope;
}
