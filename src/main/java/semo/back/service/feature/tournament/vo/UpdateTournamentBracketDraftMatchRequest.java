package semo.back.service.feature.tournament.vo;

import jakarta.validation.Valid;

import java.util.List;

public record UpdateTournamentBracketDraftMatchRequest(
        Long tournamentMatchId,
        String title,
        String scheduledAt,
        String locationLabel,
        @Valid List<UpdateTournamentBracketDraftSideRequest> sides
) {
}
