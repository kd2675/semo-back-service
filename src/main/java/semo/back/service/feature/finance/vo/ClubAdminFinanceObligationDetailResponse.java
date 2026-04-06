package semo.back.service.feature.finance.vo;

import java.util.List;

public record ClubAdminFinanceObligationDetailResponse(
        ClubAdminFinanceObligationResponse obligation,
        List<ClubFinancePaymentResponse> payments
) {
}
