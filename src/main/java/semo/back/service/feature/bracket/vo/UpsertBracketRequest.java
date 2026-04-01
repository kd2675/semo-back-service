package semo.back.service.feature.bracket.vo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpsertBracketRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 500) String summaryText,
        @NotBlank @Size(max = 30) String bracketType,
        @NotBlank @Size(max = 20) String participantType,
        @NotBlank @Size(max = 20) String sourceType,
        Long sourceTournamentRecordId,
        @NotNull @Valid List<UpsertBracketParticipantRequest> participants
) {
}
