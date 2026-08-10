package semo.back.service.feature.clubfeature.vo;

import java.util.List;

public record ApplyClubPresetResponse(
        String presetKey,
        String displayName,
        String applyMode,
        List<String> appliedFeatureKeys,
        List<String> enabledWidgetKeys,
        List<String> createdPositionNames,
        List<ClubFeatureResponse> features
) {
}
