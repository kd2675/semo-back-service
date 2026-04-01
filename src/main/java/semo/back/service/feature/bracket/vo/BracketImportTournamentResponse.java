package semo.back.service.feature.bracket.vo;

import java.util.List;

public record BracketImportTournamentResponse(
        Long tournamentRecordId,
        String title,
        String summaryText,
        String tournamentPeriodLabel,
        int participantCount,
        List<BracketImportParticipantCandidateResponse> participants
) {
}
