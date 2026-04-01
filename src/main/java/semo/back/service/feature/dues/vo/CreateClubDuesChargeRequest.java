package semo.back.service.feature.dues.vo;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record CreateClubDuesChargeRequest(
        @NotBlank(message = "회비 항목 이름은 필수입니다.")
        @Size(max = 150, message = "회비 항목 이름은 150자 이하여야 합니다.")
        String title,
        @DecimalMin(value = "0.01", message = "회비 금액은 0보다 커야 합니다.")
        BigDecimal amount,
        String dueAt,
        @Size(max = 500, message = "메모는 500자 이하여야 합니다.")
        String note,
        String targetScope,
        List<Long> clubProfileIds
) {
}
