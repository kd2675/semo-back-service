package semo.back.service.feature.position.vo;

import java.util.List;

public record ClubPositionDetailResponse(
        Long clubId,
        String clubName,
        boolean admin,
        boolean roleManagementEnabled,
        ClubPositionSummaryResponse position,
        List<ClubPermissionGroupResponse> permissionGroups
) {
}
