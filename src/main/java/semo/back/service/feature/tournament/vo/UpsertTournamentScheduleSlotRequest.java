package semo.back.service.feature.tournament.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpsertTournamentScheduleSlotRequest(
        @NotBlank @Size(max = 150) String title,
        @Size(max = 100) String courtLabel,
        @NotBlank String startAt,
        @NotBlank String endAt,
        @Size(max = 500) String note
) {
}
