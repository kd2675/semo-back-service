package semo.back.service.feature.decision.vo;

import java.util.List;

public record ClubDecisionLogResponse(
        Long clubId,
        String clubName,
        List<DecisionRecordResponse> records
) {
}
