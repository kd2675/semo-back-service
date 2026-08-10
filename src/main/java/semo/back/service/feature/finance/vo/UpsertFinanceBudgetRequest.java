package semo.back.service.feature.finance.vo;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpsertFinanceBudgetRequest(
        @NotBlank @Size(max = 40) String categoryCode,
        @DecimalMin(value = "0.00") BigDecimal allocatedAmount,
        @Size(max = 500) String note
) {
}
