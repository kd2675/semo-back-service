package semo.back.service.feature.bracket.vo;

import java.util.List;

public record ClubBracketHomeResponse(
        Long clubId,
        String clubName,
        boolean admin,
        boolean canCreate,
        int approvedBracketCount,
        int pendingBracketCount,
        BracketSummaryResponse featuredBracket,
        List<BracketSummaryResponse> publishedBrackets,
        List<BracketSummaryResponse> myBrackets,
        List<BracketImportTournamentResponse> importableTournaments
) {
}
