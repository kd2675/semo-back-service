package semo.back.service.feature.bracket.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReviewBracketRequest(
        @NotBlank String approvalStatus,
        @Size(max = 500) String rejectionReason
) {
}
