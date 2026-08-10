package semo.back.service.feature.finance.vo;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateFinanceRequestRequest(
        @NotBlank @Size(max = 30) String requestTypeCode,
        @NotBlank @Size(max = 200) String title,
        @DecimalMin(value = "0.01", message = "요청 금액은 0보다 커야 합니다.") BigDecimal amount,
        @Size(max = 120) String relatedEventName,
        @Size(max = 1000) String note,
        Long linkedScheduleEventId
) {
    public CreateFinanceRequestRequest(
            String requestTypeCode,
            String title,
            BigDecimal amount,
            String relatedEventName,
            String note
    ) {
        this(requestTypeCode, title, amount, relatedEventName, note, null);
    }
}
