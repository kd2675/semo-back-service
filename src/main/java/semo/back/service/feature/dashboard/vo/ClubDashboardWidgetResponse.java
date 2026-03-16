package semo.back.service.feature.dashboard.vo;

public record ClubDashboardWidgetResponse(
        String widgetKey,
        String displayName,
        String description,
        String iconName,
        String requiredFeatureKey,
        String visibilityScope,
        boolean available,
        boolean enabled,
        int sortOrder,
        int columnSpan,
        int rowSpan,
        String title,
        String userPath,
        String adminPath
) {
}
