package semo.back.service.feature.decision.vo;

public record DecisionParticipantResponse(
        Long clubProfileId,
        String displayName,
        String participantRole
) {
}
