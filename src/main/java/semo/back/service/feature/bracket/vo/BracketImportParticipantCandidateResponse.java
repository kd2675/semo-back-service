package semo.back.service.feature.bracket.vo;

public record BracketImportParticipantCandidateResponse(
        Long tournamentApplicationId,
        Long clubProfileId,
        String displayName
) {
}
