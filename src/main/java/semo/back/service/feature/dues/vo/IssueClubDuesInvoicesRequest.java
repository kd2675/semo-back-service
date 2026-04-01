package semo.back.service.feature.dues.vo;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record IssueClubDuesInvoicesRequest(
        @NotNull @Min(2000) @Max(2100) Integer billingYear,
        @NotNull @Min(1) @Max(12) Integer billingMonth,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        String dueAt,
        @Size(max = 500) String note,
        List<Long> clubProfileIds
) {
}
