package semo.back.service.feature.dashboard.vo;

import java.util.List;

public record UpdateClubDashboardLayoutRequest(
        String scope,
        List<UpdateClubDashboardWidgetItemRequest> widgets
) {
}
