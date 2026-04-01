package semo.back.service.feature.bracket.vo;

import jakarta.validation.constraints.Size;

public record UpsertBracketParticipantRequest(
        Long clubProfileId,
        @Size(max = 100) String displayName,
        Integer seedNumber,
        Long sourceTournamentApplicationId
) {
}
