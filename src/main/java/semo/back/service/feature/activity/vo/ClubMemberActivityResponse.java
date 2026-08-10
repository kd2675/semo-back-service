package semo.back.service.feature.activity.vo;

import java.util.List;

public record ClubMemberActivityResponse(
        Long clubId,
        String clubName,
        boolean admin,
        List<ClubMemberActivityEntryResponse> entries,
        String nextCursorCreatedAt,
        Long nextCursorActivityId,
        boolean hasNext
) {
}
