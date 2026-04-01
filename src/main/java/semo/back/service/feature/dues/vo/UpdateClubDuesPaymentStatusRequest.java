package semo.back.service.feature.dues.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateClubDuesPaymentStatusRequest(
        @NotBlank @Size(max = 20) String paymentStatus,
        @Size(max = 500) String note
) {
}
