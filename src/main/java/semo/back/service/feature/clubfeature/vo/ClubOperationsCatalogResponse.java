package semo.back.service.feature.clubfeature.vo;

import java.util.List;

public record ClubOperationsCatalogResponse(
        Long clubId,
        String clubName,
        List<ClubPresetResponse> presets,
        List<ClubOperationTemplateResponse> templates
) {
}
