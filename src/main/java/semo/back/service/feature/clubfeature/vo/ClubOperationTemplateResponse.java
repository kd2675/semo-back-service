package semo.back.service.feature.clubfeature.vo;

import java.util.List;

public record ClubOperationTemplateResponse(
        String templateKey,
        String displayName,
        String description,
        String iconName,
        List<String> requiredFeatureKeys,
        List<String> checklistItems,
        int defaultDueDays,
        String recurrenceFrequency,
        String targetPath
) {
}
