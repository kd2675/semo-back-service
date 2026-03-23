package semo.back.service.feature.clubfeature.vo;

public record ClubFeatureResponse(
        String featureKey,
        String displayName,
        String description,
        String iconName,
        String navigationScope,
        boolean enabled,
        String userPath,
        String adminPath
) {
}
