package semo.back.service.feature.notice.vo;

import semo.back.service.feature.schedule.vo.ScheduleEventSummaryResponse;
import semo.back.service.feature.schedule.vo.ScheduleVoteSummaryResponse;

import java.util.List;

public record ClubNoticeHomeResponse(
        Long clubId,
        String clubName,
        boolean admin,
        boolean canCreate,
        int totalNoticeCount,
        int pinnedNoticeCount,
        int scheduledNoticeCount,
        int publishedTodayCount,
        int manageableNoticeCount,
        List<ClubNoticeSummaryResponse> notices,
        List<ScheduleEventSummaryResponse> sharedEvents,
        List<ScheduleVoteSummaryResponse> sharedVotes
) {
}
