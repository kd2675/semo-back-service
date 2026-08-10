package semo.back.service.feature.finance.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpsertFinanceAccountRequest(
        @NotBlank @Size(max = 100) String displayName,
        @NotBlank @Size(max = 20) String accountTypeCode,
        @Size(max = 100) String providerName,
        @Size(max = 120) String maskedIdentifier,
        @Size(max = 100) String holderName,
        @NotBlank @Size(max = 20) String usageScopeCode,
        boolean defaultCollection,
        boolean defaultExpense
) {
}
