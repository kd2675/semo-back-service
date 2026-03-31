package semo.back.service.feature.tournament.vo;

import java.util.List;

public record TournamentBracketMatchResponse(
        Long tournamentMatchId,
        Long tournamentRoundId,
        String title,
        String matchStatus,
        String scheduledAt,
        String scheduledAtLabel,
        String locationLabel,
        Long winnerEntryId,
        int sortOrder,
        List<TournamentBracketSideResponse> sides
) {
}
