package semo.back.service.feature.activity.vo;

import java.util.List;

public record ClubAdminActivityFeedResponse(
        Long clubId,
        String clubName,
        List<ClubAdminActivityItemResponse> activities,
        String nextCursorCreatedAt,
        Long nextCursorActivityId,
        boolean hasNext
) {
}
