package semo.back.service.feature.finance.vo;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateFinanceExpenseRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 40) String categoryCode,
        @DecimalMin(value = "0.01", message = "지출 금액은 0보다 커야 합니다.") BigDecimal amount,
        String spentAt,
        @Size(max = 120) String relatedEventName,
        @Size(max = 1000) String note
) {
}
