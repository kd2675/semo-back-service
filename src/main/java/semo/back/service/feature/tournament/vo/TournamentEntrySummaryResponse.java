package semo.back.service.feature.tournament.vo;

import java.util.List;

public record TournamentEntrySummaryResponse(
        Long tournamentEntryId,
        String entryType,
        String displayName,
        String entryStatus,
        Integer seedNumber,
        int sortOrder,
        List<TournamentEntryMemberResponse> members
) {
}
