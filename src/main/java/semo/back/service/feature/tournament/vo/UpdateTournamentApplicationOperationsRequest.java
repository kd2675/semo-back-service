package semo.back.service.feature.tournament.vo;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpdateTournamentApplicationOperationsRequest(
        Boolean checkedIn,
        @Min(1) Integer placement,
        @Size(max = 1000) String resultNote
) {
}
