package semo.back.service.feature.tournament.vo;

import jakarta.validation.constraints.Size;

public record CancelTournamentRequest(
        @Size(max = 500) String cancelReason
) {
}
