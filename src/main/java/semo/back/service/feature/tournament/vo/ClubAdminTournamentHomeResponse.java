package semo.back.service.feature.tournament.vo;

import java.util.List;

public record ClubAdminTournamentHomeResponse(
        Long clubId,
        String clubName,
        boolean admin,
        boolean canCreate,
        int totalTournamentCount,
        int activeTournamentCount,
        int completedTournamentCount,
        int recruitingTournamentCount,
        int bracketConfirmedCount,
        List<TournamentSummaryResponse> tournaments
) {
}
