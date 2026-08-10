package semo.back.service.feature.notice.vo;

import java.util.List;

public record ClubNoticeFeedResponse(
        Long clubId,
        String clubName,
        boolean admin,
        boolean canCreateNotice,
        boolean canCreateSchedule,
        boolean canCreatePoll,
        boolean canCreateTournament,
        List<ClubBoardFeedItemResponse> items,
        Long nextCursorBoardItemId,
        boolean hasNext
) {
}
