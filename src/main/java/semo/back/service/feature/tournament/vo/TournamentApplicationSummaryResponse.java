package semo.back.service.feature.tournament.vo;

public record TournamentApplicationSummaryResponse(
        Long tournamentApplicationId,
        Long clubProfileId,
        String applicantDisplayName,
        String applicantAvatarImageUrl,
        String applicantAvatarThumbnailUrl,
        String applicationStatus,
        String applicationNote,
        String teamName,
        Integer waitlistPosition,
        Long financePaymentId,
        String feePaymentStatusCode,
        String feePaymentStatusLabel,
        String checkedInAtLabel,
        Integer placement,
        String resultNote,
        java.util.List<TournamentRosterMemberResponse> rosterMembers,
        String appliedAtLabel,
        boolean mine,
        boolean canReview,
        boolean canCancel
) {
}
