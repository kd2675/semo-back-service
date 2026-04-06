package semo.back.service.feature.finance.vo;

import java.util.List;

public record ClubFinanceExpenseFeedResponse(
        Long clubId,
        String clubName,
        List<ClubFinanceExpenseResponse> items
) {
}
