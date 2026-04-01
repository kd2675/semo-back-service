package semo.back.service.feature.dues.vo;

import java.util.List;

public record ClubDuesHomeResponse(
        Long clubId,
        String clubName,
        boolean admin,
        int pendingInvoiceCount,
        int paidInvoiceCount,
        int overdueInvoiceCount,
        String totalPendingAmountLabel,
        ClubDuesSummaryResponse nextInvoice,
        List<ClubDuesSummaryResponse> myInvoices
) {
}
