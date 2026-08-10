package semo.back.service.feature.decision.vo;

public record DecisionResourceLinkResponse(
        String relationType,
        String resourceType,
        Long resourceId,
        String title,
        String targetPath
) {
}
