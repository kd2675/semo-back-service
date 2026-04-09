package semo.back.service.feature.club.vo;

import java.util.List;

public record ClubJoinRequestInboxResponse(
        Long clubId,
        String clubName,
        boolean admin,
        int pendingRequestCount,
        int requestedTodayCount,
        int messageAttachedCount,
        String latestRequestedAtLabel,
        List<ClubJoinRequestInboxItemResponse> requests
) {
}
