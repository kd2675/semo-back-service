package semo.back.service.feature.finance.vo;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateFinancePeriodRequest(
        @NotBlank @Size(max = 100) String title,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        Long clubOperatingTermId,
        BigDecimal openingBalance,
        @Size(max = 1000) String note
) {
}
