package semo.back.service.feature.tournament.vo;

import java.util.List;

public record ClubTournamentHomeResponse(
        Long clubId,
        String clubName,
        boolean admin,
        boolean canCreate,
        int totalTournamentCount,
        int recruitingCount,
        int ongoingCount,
        int participatingCount,
        TournamentSummaryResponse featuredTournament,
        List<TournamentSummaryResponse> tournaments,
        List<TournamentSummaryResponse> myTournaments,
        List<TournamentSummaryResponse> archivedTournaments
) {
}
