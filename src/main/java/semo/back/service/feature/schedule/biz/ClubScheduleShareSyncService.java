package semo.back.service.feature.schedule.biz;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import semo.back.service.database.pub.entity.ClubScheduleEvent;
import semo.back.service.database.pub.entity.ClubScheduleVote;
import semo.back.service.feature.share.biz.ClubContentShareService;

@Component
@RequiredArgsConstructor
public class ClubScheduleShareSyncService {
    private final ClubContentShareService clubContentShareService;

    public void syncEventShares(ClubScheduleEvent event) {
        boolean activeEvent = !"CANCELLED".equals(event.getEventStatus());
        clubContentShareService.syncBoardShare(
                event.getClubId(),
                ClubContentShareService.CONTENT_SCHEDULE_EVENT,
                event.getEventId(),
                event.isSharedToBoard() && activeEvent
        );
        clubContentShareService.syncCalendarShare(
                event.getClubId(),
                ClubContentShareService.CONTENT_SCHEDULE_EVENT,
                event.getEventId(),
                event.isSharedToCalendar() && activeEvent
        );
    }

    public void syncVoteShares(ClubScheduleVote vote) {
        clubContentShareService.syncBoardShare(
                vote.getClubId(),
                ClubContentShareService.CONTENT_SCHEDULE_VOTE,
                vote.getVoteId(),
                vote.isSharedToBoard()
        );
        clubContentShareService.syncCalendarShare(
                vote.getClubId(),
                ClubContentShareService.CONTENT_SCHEDULE_VOTE,
                vote.getVoteId(),
                vote.isSharedToCalendar()
        );
    }

    public void removeEventShares(Long clubId, Long eventId) {
        clubContentShareService.removeAllShares(clubId, ClubContentShareService.CONTENT_SCHEDULE_EVENT, eventId);
    }

    public void removeVoteShares(Long clubId, Long voteId) {
        clubContentShareService.removeAllShares(clubId, ClubContentShareService.CONTENT_SCHEDULE_VOTE, voteId);
    }
}
