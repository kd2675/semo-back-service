package semo.back.service.feature.finance.vo;

import java.util.List;

public record ClubAdminFinanceObligationFeedResponse(
        Long clubId,
        String clubName,
        List<ClubAdminFinanceObligationResponse> items,
        Long nextCursorObligationId,
        boolean hasNext
) {
}
