package semo.back.service.feature.poll.vo;

import java.util.List;

public record ClubPollHomeResponse(
        Long clubId,
        String clubName,
        boolean admin,
        boolean canCreate,
        int waitingCount,
        int ongoingCount,
        int closedCount,
        List<ClubPollSummaryResponse> polls
) {
}
