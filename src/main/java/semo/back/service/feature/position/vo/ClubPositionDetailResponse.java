package semo.back.service.feature.position.vo;

import java.util.List;

public record ClubPositionDetailResponse(
        Long clubId,
        String clubName,
        boolean admin,
        boolean roleManagementEnabled,
        boolean canUpdate,
        boolean canDelete,
        boolean canAssign,
        ClubPositionSummaryResponse position,
        List<ClubPermissionGroupResponse> permissionGroups
) {
}
