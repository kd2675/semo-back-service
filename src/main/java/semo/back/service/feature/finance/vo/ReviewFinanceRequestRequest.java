package semo.back.service.feature.finance.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReviewFinanceRequestRequest(
        @NotBlank @Size(max = 20) String statusCode,
        @Size(max = 1000) String reviewNote
) {
}
