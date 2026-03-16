package semo.back.service.feature.timeline.vo;

import semo.back.service.feature.notice.vo.NoticeCategorySettingResponse;

import java.util.List;

public record ClubAdminTimelineResponse(
        Long clubId,
        String clubName,
        List<NoticeCategorySettingResponse> categories
) {
}
