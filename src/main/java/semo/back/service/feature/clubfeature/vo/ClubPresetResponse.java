package semo.back.service.feature.clubfeature.vo;

import java.util.List;

public record ClubPresetResponse(
        String presetKey,
        String displayName,
        String description,
        String iconName,
        List<String> featureKeys,
        List<String> featureDisplayNames,
        List<String> recommendedWidgetKeys,
        List<String> delegatedPositionNames,
        boolean includedInCurrentConfiguration
) {
}
