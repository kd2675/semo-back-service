package semo.back.service.feature.tournament.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReviewTournamentRecordRequest(
        @NotBlank @Size(max = 20) String approvalStatus,
        @Size(max = 500) String rejectionReason
) {
}
