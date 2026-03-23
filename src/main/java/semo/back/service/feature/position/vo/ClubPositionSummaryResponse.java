package semo.back.service.feature.position.vo;

import java.util.List;

public record ClubPositionSummaryResponse(
        Long clubPositionId,
        String positionCode,
        String displayName,
        String description,
        String iconName,
        String colorHex,
        boolean active,
        int permissionCount,
        int memberCount,
        List<String> permissionKeys
) {
}
