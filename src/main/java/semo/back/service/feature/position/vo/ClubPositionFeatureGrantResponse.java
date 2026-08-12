package semo.back.service.feature.position.vo;

import java.util.List;

public record ClubPositionFeatureGrantResponse(
        String featureKey,
        String accessLevel,
        int policyVersion,
        int currentPolicyVersion,
        String status,
        List<String> sensitivePermissionKeys,
        int effectivePermissionCount
) {
}
