package semo.back.service.feature.position.vo;

import java.util.List;

public record ClubFeatureAccessLevelResponse(
        String accessLevel,
        String displayName,
        String description,
        int policyVersion,
        List<String> permissionKeys
) {
}
