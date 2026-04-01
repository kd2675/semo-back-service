package semo.back.service.feature.dues.vo;

public record CreateClubDuesChargeResponse(
        Long chargeId,
        String title,
        String targetScope,
        String targetScopeLabel,
        int createdCount
) {
}
