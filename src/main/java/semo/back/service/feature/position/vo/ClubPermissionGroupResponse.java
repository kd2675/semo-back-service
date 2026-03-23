package semo.back.service.feature.position.vo;

import java.util.List;

public record ClubPermissionGroupResponse(
        String featureKey,
        String displayName,
        String description,
        String iconName,
        List<ClubPermissionItemResponse> permissions
) {
}
