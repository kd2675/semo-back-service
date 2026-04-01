package semo.back.service.feature.bracket.vo;

public record BracketMatchResponse(
        int matchNumber,
        String homeParticipantName,
        String awayParticipantName
) {
}
