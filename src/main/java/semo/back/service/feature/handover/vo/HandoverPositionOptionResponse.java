package semo.back.service.feature.handover.vo;

public record HandoverPositionOptionResponse(
        Long clubPositionId,
        String displayName,
        String description,
        String iconName,
        String colorHex
) {
}
