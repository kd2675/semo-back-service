package semo.back.service.feature.tournament.vo;

public record TournamentParticipantSummaryResponse(
        Long clubProfileId,
        String displayName,
        String avatarImageUrl,
        String avatarThumbnailUrl,
        String approvedAtLabel,
        String teamName,
        Long financePaymentId,
        String feePaymentStatusCode,
        String feePaymentStatusLabel,
        String checkedInAtLabel,
        Integer placement,
        String resultNote,
        java.util.List<TournamentRosterMemberResponse> rosterMembers
) {
}
