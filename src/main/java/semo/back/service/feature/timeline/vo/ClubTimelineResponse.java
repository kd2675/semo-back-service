package semo.back.service.feature.timeline.vo;

import java.util.List;

public record ClubTimelineResponse(
        Long clubId,
        String clubName,
        boolean admin,
        List<TimelineEntryResponse> entries,
        String nextCursorPublishedAt,
        Long nextCursorNoticeId,
        boolean hasNext
) {
}
