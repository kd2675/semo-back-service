package semo.back.service.feature.position.vo;

public record ClubPermissionItemResponse(
        String permissionKey,
        String displayName,
        String description,
        String ownershipScope
) {
}
