package semo.back.service.feature.bracket.vo;

public record BracketParticipantResponse(
        Long bracketParticipantId,
        int seedNumber,
        Long clubProfileId,
        String displayName,
        boolean guestEntry,
        String participantRole,
        String entrySourceType,
        Long sourceTournamentApplicationId
) {
}
