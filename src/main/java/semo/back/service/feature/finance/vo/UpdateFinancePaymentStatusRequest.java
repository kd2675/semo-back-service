package semo.back.service.feature.finance.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateFinancePaymentStatusRequest(
        @NotBlank @Size(max = 20) String paymentStatusCode,
        @Size(max = 500) String note
) {
}
