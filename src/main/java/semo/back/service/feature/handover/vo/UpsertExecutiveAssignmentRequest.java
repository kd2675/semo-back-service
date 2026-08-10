package semo.back.service.feature.handover.vo;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpsertExecutiveAssignmentRequest(
        @NotNull Long clubMemberId,
        @NotNull Long clubPositionId,
        @Size(max = 1000) String responsibility,
        @Min(0) @Max(10000) Integer sortOrder
) {
}
