package semo.back.service.feature.finance.vo;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CorrectFinanceExpenseRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 40) String categoryCode,
        @DecimalMin(value = "0.01") BigDecimal amount,
        String spentAt,
        Long financePeriodId,
        Long financeAccountId,
        Long linkedScheduleEventId,
        @Size(max = 120) String relatedEventName,
        @Size(max = 1000) String note,
        @NotBlank @Size(max = 1000) String reason
) {
}
