package semo.back.service.feature.decision.vo;

public record DecisionResourceOptionResponse(
        String resourceType,
        Long resourceId,
        String title,
        String statusLabel,
        String targetPath
) {
}
