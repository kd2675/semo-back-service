package semo.back.service.feature.position.vo;

import java.util.List;

public record ClubAdminRoleManagementResponse(
        Long clubId,
        String clubName,
        boolean admin,
        boolean roleManagementEnabled,
        boolean canCreate,
        boolean canUpdate,
        boolean canDelete,
        boolean canAssign,
        List<ClubPositionSummaryResponse> positions,
        List<ClubPermissionGroupResponse> permissionGroups
) {
}
