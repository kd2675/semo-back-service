package semo.back.service.feature.dashboard.vo;

import java.util.List;

public record ClubDashboardEditorResponse(
        String scope,
        List<ClubDashboardWidgetResponse> widgets
) {
}
