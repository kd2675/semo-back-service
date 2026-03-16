package semo.back.service.feature.timeline.vo;

import semo.back.service.feature.notice.vo.NoticeCategoryOptionResponse;

import java.util.List;

public record ClubTimelineResponse(
        Long clubId,
        String clubName,
        boolean admin,
        String selectedCategoryKey,
        List<NoticeCategoryOptionResponse> categories,
        List<TimelineEntryResponse> entries,
        String nextCursorPublishedAt,
        Long nextCursorNoticeId,
        boolean hasNext
) {
}
