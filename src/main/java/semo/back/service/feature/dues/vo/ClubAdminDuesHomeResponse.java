package semo.back.service.feature.dues.vo;

import java.util.List;

public record ClubAdminDuesHomeResponse(
        Long clubId,
        String clubName,
        boolean admin,
        boolean canIssue,
        boolean canMarkPaid,
        boolean canMarkWaive,
        int activeMemberCount,
        int totalChargeCount,
        int totalInvoiceCount,
        int pendingInvoiceCount,
        int paidInvoiceCount,
        int waivedInvoiceCount,
        int overdueInvoiceCount,
        int collectionRate,
        List<ClubDuesMemberOptionResponse> availableMembers,
        List<ClubAdminDuesChargeResponse> charges
) {
}
