package semo.back.service.feature.finance.vo;

public record FinanceAccountResponse(
        Long financeAccountId,
        String displayName,
        String accountTypeCode,
        String accountTypeLabel,
        String providerName,
        String maskedIdentifier,
        String holderName,
        String usageScopeCode,
        String usageScopeLabel,
        boolean active,
        boolean defaultCollection,
        boolean defaultExpense
) {
}
