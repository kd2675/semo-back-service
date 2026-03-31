package semo.back.service.feature.tournament.vo;

import jakarta.validation.Valid;

import java.util.List;

public record UpdateTournamentBracketDraftRequest(
        @Valid List<UpdateTournamentBracketDraftMatchRequest> matches
) {
}
