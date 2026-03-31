package semo.back.service.feature.tournament.vo;

public record TournamentUpsertResponse(
        Long tournamentRecordId,
        String title,
        String startDate,
        String endDate,
        String tournamentStatus,
        boolean bracketConfirmed
) {
}
