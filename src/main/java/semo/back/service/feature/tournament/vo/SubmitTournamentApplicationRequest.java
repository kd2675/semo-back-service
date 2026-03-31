package semo.back.service.feature.tournament.vo;

import jakarta.validation.constraints.Size;

public record SubmitTournamentApplicationRequest(
        @Size(max = 500) String applicationNote
) {
}
