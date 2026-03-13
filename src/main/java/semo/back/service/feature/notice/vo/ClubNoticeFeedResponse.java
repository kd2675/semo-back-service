package semo.back.service.feature.notice.vo;

import java.util.List;

public record ClubNoticeFeedResponse(
        Long clubId,
        String clubName,
        boolean admin,
        List<ClubNoticeSummaryResponse> notices,
        String nextCursorPublishedAt,
        Long nextCursorNoticeId,
        boolean hasNext
) {
}
