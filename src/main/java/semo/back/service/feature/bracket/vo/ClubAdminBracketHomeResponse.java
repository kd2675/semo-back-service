package semo.back.service.feature.bracket.vo;

import java.util.List;

public record ClubAdminBracketHomeResponse(
        Long clubId,
        String clubName,
        boolean admin,
        int totalBracketCount,
        int draftBracketCount,
        int pendingBracketCount,
        int approvedBracketCount,
        int rejectedBracketCount,
        List<BracketSummaryResponse> brackets
) {
}
