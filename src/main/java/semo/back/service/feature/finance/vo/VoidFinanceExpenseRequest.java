package semo.back.service.feature.finance.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VoidFinanceExpenseRequest(
        @NotBlank @Size(max = 1000) String reason
) {
}
