package semo.back.service.feature.tournament.vo;

public record TournamentSummaryResponse(
        Long tournamentRecordId,
        String title,
        String summaryText,
        String approvalStatus,
        String tournamentStatus,
        String authorDisplayName,
        String authorAvatarImageUrl,
        String authorAvatarThumbnailUrl,
        String applicationWindowLabel,
        String tournamentPeriodLabel,
        String startDate,
        String endDate,
        String locationLabel,
        String matchFormat,
        Integer teamMemberLimit,
        Integer participantLimit,
        int approvedApplicationCount,
        int participantCount,
        boolean feeRequired,
        Integer feeAmount,
        String feeCurrencyCode,
        boolean postedToBoard,
        boolean postedToCalendar,
        boolean pinned,
        boolean mine,
        boolean participating,
        boolean canEdit,
        boolean canCancel,
        boolean canDelete
) {
}
