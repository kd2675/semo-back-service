package semo.back.service.feature.decision.vo;

import java.util.List;

public record ClubDecisionAdminCenterResponse(
        Long clubId,
        String clubName,
        boolean fullAdmin,
        boolean canManage,
        int draftCount,
        int confirmedCount,
        int reviewDueCount,
        List<DecisionRecordResponse> records,
        List<DecisionMemberOptionResponse> memberOptions,
        List<DecisionResourceOptionResponse> resourceOptions,
        List<DecisionTermOptionResponse> termOptions
) {
}
