package semo.back.service.feature.dues.vo;

public record ClubAdminDuesSummaryAggregate(
        long totalInvoiceCount,
        long pendingInvoiceCount,
        long paidInvoiceCount,
        long waivedInvoiceCount,
        long overdueInvoiceCount
) {
}
