package semo.back.service.feature.finance.vo;

import java.util.List;

public record ClubFinanceRequestFeedResponse(
        Long clubId,
        String clubName,
        List<ClubFinanceRequestResponse> items
) {
}
