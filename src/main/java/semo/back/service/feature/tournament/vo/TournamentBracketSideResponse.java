package semo.back.service.feature.tournament.vo;

import java.util.List;

public record TournamentBracketSideResponse(
        Long tournamentMatchSideId,
        int sideNo,
        Long tournamentEntryId,
        String entryDisplayName,
        Integer seedNumber,
        String scoreSummary,
        String resultStatus,
        List<TournamentEntryMemberResponse> members
) {
}
