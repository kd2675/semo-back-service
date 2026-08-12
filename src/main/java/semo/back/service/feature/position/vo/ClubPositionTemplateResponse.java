package semo.back.service.feature.position.vo;

import java.util.List;

public record ClubPositionTemplateResponse(
        String templateKey,
        String displayName,
        String description,
        String iconName,
        String colorHex,
        List<ClubPositionTemplateGrantResponse> featureGrants,
        List<String> permissionKeys,
        int featureCount
) {
}
