package semo.back.service.feature.clubfeature.vo;

import java.util.List;

public record ClubMoreSummaryResponse(
        Long clubId,
        String clubName,
        boolean fullAdmin,
        List<String> grantedPermissionKeys,
        List<String> adminToolFeatureKeys,
        List<ClubFeatureResponse> features
) {
}
