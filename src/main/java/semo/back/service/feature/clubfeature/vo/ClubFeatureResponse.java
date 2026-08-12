package semo.back.service.feature.clubfeature.vo;

import java.util.List;

public record ClubFeatureResponse(
        String featureKey,
        String displayName,
        String description,
        String iconName,
        String navigationScope,
        int sortOrder,
        boolean enabled,
        String userPath,
        String adminPath,
        List<String> requiredFeatureKeys,
        boolean mandatory,
        String mandatoryReason,
        boolean available,
        String unavailableReason
) {
}
