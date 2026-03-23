package semo.back.service.feature.position.vo;

import java.util.List;

public record ClubAdminRoleManagementResponse(
        Long clubId,
        String clubName,
        boolean admin,
        boolean roleManagementEnabled,
        List<ClubPositionSummaryResponse> positions,
        List<ClubPermissionGroupResponse> permissionGroups
) {
}
