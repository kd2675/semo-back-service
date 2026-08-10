package semo.back.service.feature.decision.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record DecisionResourceReferenceRequest(
        @NotBlank @Size(max = 40) String resourceType,
        @NotNull @Positive Long resourceId
) {
}
