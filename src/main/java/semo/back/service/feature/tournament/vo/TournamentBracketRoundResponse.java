package semo.back.service.feature.tournament.vo;

import java.util.List;

public record TournamentBracketRoundResponse(
        Long tournamentRoundId,
        String roundKey,
        String displayName,
        String roundType,
        int sortOrder,
        List<TournamentBracketMatchResponse> matches
) {
}
