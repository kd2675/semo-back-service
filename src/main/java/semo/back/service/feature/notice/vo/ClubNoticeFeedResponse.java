package semo.back.service.feature.notice.vo;

import java.util.List;

public record ClubNoticeFeedResponse(
        Long clubId,
        String clubName,
        boolean admin,
        List<ClubBoardFeedItemResponse> items,
        Long nextCursorBoardItemId,
        boolean hasNext
) {
}
