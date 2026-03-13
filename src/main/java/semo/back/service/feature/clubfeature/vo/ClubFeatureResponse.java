package semo.back.service.feature.clubfeature.vo;

public record ClubFeatureResponse(
        String featureKey,
        String displayName,
        String description,
        String iconName,
        boolean enabled,
        String userPath,
        String adminPath
) {
}
