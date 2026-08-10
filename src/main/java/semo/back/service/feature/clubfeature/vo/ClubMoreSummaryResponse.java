package semo.back.service.feature.clubfeature.vo;

import java.util.List;

public record ClubMoreSummaryResponse(
        Long clubId,
        String clubName,
        boolean fullAdmin,
        List<String> grantedPermissionKeys,
        List<String> adminToolFeatureKeys,
        int userPendingCount,
        int userOverdueCount,
        int adminPendingCount,
        int adminOverdueCount,
        List<ClubMoreFeatureStatusResponse> featureStatuses,
        List<ClubFeatureResponse> features
) {
}
