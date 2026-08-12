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
        int assignedMemberCount,
        List<ClubPositionSummaryResponse> positions,
        List<ClubPermissionGroupResponse> permissionGroups,
        List<ClubPositionTemplateResponse> positionTemplates
) {
}
