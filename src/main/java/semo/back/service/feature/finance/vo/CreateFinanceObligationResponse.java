package semo.back.service.feature.finance.vo;

public record CreateFinanceObligationResponse(
        Long obligationId,
        String obligationTypeCode,
        String title,
        String targetScopeCode,
        String targetScopeLabel,
        int createdCount
) {
}
