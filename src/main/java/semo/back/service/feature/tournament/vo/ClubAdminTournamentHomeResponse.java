package semo.back.service.feature.tournament.vo;

import java.util.List;

public record ClubAdminTournamentHomeResponse(
        Long clubId,
        String clubName,
        boolean admin,
        boolean canReview,
        boolean canDelete,
        int totalTournamentCount,
        int pendingTournamentCount,
        int approvedTournamentCount,
        int rejectedTournamentCount,
        List<TournamentSummaryResponse> tournaments
) {
}
