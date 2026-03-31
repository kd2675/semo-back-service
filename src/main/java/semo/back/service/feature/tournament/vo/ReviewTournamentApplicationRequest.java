package semo.back.service.feature.tournament.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReviewTournamentApplicationRequest(
        @NotBlank @Size(max = 20) String applicationStatus,
        @Size(max = 500) String reviewNote
) {
}
