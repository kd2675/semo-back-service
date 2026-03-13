package semo.back.service.feature.club.vo;

import java.util.List;

public record ClubBoardResponse(
        Long clubId,
        String clubName,
        boolean admin,
        List<ClubBoardNoticeResponse> notices
) {
}
