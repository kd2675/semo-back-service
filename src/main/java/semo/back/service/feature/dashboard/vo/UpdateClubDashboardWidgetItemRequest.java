package semo.back.service.feature.dashboard.vo;

public record UpdateClubDashboardWidgetItemRequest(
        String widgetKey,
        Boolean enabled,
        Integer sortOrder,
        Integer columnSpan,
        Integer rowSpan,
        String titleOverride
) {
}
