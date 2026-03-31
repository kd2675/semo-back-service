package semo.back.service.feature.tournament.vo;

public record UpdateTournamentBracketDraftSideRequest(
        Long tournamentMatchSideId,
        Long tournamentEntryId,
        String scoreSummary,
        String resultStatus
) {
}
