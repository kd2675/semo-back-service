package semo.back.service.feature.dues.vo;

import java.util.List;

public record ClubAdminDuesChargeDetailResponse(
        ClubAdminDuesChargeResponse charge,
        List<ClubDuesInvoiceResponse> invoices
) {
}
