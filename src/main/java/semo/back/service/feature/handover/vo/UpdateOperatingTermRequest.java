package semo.back.service.feature.handover.vo;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateOperatingTermRequest(
        @NotBlank @Size(max = 100) String termName,
        @NotBlank @Size(max = 20) String termType,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        @Size(max = 1000) String description
) {
}
