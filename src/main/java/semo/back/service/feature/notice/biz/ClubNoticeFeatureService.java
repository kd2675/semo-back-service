package semo.back.service.feature.notice.biz;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import semo.back.service.database.pub.entity.ClubNotice;
import semo.back.service.database.pub.entity.ClubScheduleEvent;
import semo.back.service.database.pub.entity.ClubScheduleVote;
import semo.back.service.database.pub.repository.ClubScheduleEventRepository;
import semo.back.service.database.pub.repository.ClubScheduleVoteRepository;
import semo.back.service.feature.club.biz.ClubAccessResolver;
import semo.back.service.feature.notice.vo.ClubNoticeHomeResponse;
import semo.back.service.feature.notice.vo.ClubNoticeSummaryResponse;
import semo.back.service.feature.schedule.biz.ClubScheduleService;
import semo.back.service.feature.schedule.vo.ScheduleEventSummaryResponse;
import semo.back.service.feature.schedule.vo.ScheduleVoteSummaryResponse;
import semo.back.service.feature.share.biz.ClubContentShareService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubNoticeFeatureService {
    private final ClubAccessResolver clubAccessResolver;
    private final ClubNoticeService clubNoticeService;
    private final ClubScheduleService clubScheduleService;
    private final ClubScheduleEventRepository clubScheduleEventRepository;
    private final ClubScheduleVoteRepository clubScheduleVoteRepository;
    private final ClubContentShareService clubContentShareService;

    public NoticeSharedContent getSharedContent(Long clubId, String userKey) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        List<ClubScheduleEvent> sharedEvents = loadBoardSharedEvents(clubId);
        List<ScheduleEventSummaryResponse> sharedEventSummaries = clubScheduleService
                .getEventSummariesForHome(access, sharedEvents)
                .stream()
                .limit(20)
                .toList();
        List<ClubScheduleVote> sharedVotes = loadBoardSharedVotes(clubId);
        List<ScheduleVoteSummaryResponse> sharedVoteSummaries = clubScheduleService
                .getVoteSummariesForHome(access, sharedVotes)
                .stream()
                .limit(20)
                .toList();
        return new NoticeSharedContent(sharedEventSummaries, sharedVoteSummaries);
    }

    public ClubNoticeHomeResponse getNoticeHome(Long clubId, String userKey, boolean pinnedOnly) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);

        List<ClubNotice> allNotices = access.isAdmin()
                ? clubNoticeService.getActiveNotices(clubId)
                : clubNoticeService.getDirectNoticesByAuthor(clubId, access.clubProfile().getClubProfileId());
        List<ClubNotice> listNotices = pinnedOnly
                ? (access.isAdmin()
                        ? clubNoticeService.getPinnedNotices(clubId)
                        : clubNoticeService.getDirectPinnedNoticesByAuthor(clubId, access.clubProfile().getClubProfileId()))
                : allNotices;
        List<ClubNotice> homeNotices = access.isAdmin()
                ? listNotices.stream()
                        .filter(notice -> clubNoticeService.canManageNotice(access, notice.getAuthorClubProfileId()))
                        .limit(20)
                        .toList()
                : listNotices.stream().limit(20).toList();
        List<ClubNoticeSummaryResponse> manageableNotices = clubNoticeService.toNoticeSummaries(access, homeNotices);
        List<ClubScheduleEvent> sharedEvents = loadBoardSharedEvents(clubId);
        List<ScheduleEventSummaryResponse> sharedEventSummaries = clubScheduleService
                .getEventSummariesForHome(access, sharedEvents)
                .stream()
                .limit(20)
                .toList();
        List<ClubScheduleVote> sharedVotes = loadBoardSharedVotes(clubId);
        List<ScheduleVoteSummaryResponse> sharedVoteSummaries = clubScheduleService
                .getVoteSummariesForHome(access, sharedVotes)
                .stream()
                .limit(20)
                .toList();

        LocalDate today = LocalDate.now();
        return new ClubNoticeHomeResponse(
                access.club().getClubId(),
                access.club().getName(),
                access.isAdmin(),
                access.isAdmin(),
                allNotices.size(),
                (int) allNotices.stream().filter(ClubNotice::isPinned).count(),
                (int) allNotices.stream().filter(ClubNotice::isSharedToCalendar).count(),
                (int) allNotices.stream()
                        .filter(notice -> notice.getPublishedAt() != null && notice.getPublishedAt().toLocalDate().isEqual(today))
                        .count(),
                manageableNotices.size(),
                manageableNotices,
                sharedEventSummaries,
                sharedVoteSummaries
        );
    }

    private List<ClubScheduleEvent> loadBoardSharedEvents(Long clubId) {
        List<Long> eventIds = clubContentShareService.getBoardContentIds(clubId, ClubContentShareService.CONTENT_SCHEDULE_EVENT);
        if (eventIds.isEmpty()) {
            return List.of();
        }
        Map<Long, ClubScheduleEvent> eventById = clubScheduleEventRepository.findAllByEventIdIn(eventIds).stream()
                .collect(Collectors.toMap(ClubScheduleEvent::getEventId, Function.identity()));
        return eventIds.stream()
                .map(eventById::get)
                .filter(event -> event != null && !"CANCELLED".equals(event.getEventStatus()))
                .toList();
    }

    private List<ClubScheduleVote> loadBoardSharedVotes(Long clubId) {
        List<Long> voteIds = clubContentShareService.getBoardContentIds(clubId, ClubContentShareService.CONTENT_SCHEDULE_VOTE);
        if (voteIds.isEmpty()) {
            return List.of();
        }
        Map<Long, ClubScheduleVote> voteById = clubScheduleVoteRepository.findAllByVoteIdIn(voteIds).stream()
                .collect(Collectors.toMap(ClubScheduleVote::getVoteId, Function.identity()));
        return voteIds.stream()
                .map(voteById::get)
                .filter(vote -> vote != null)
                .toList();
    }

    public record NoticeSharedContent(
            List<ScheduleEventSummaryResponse> sharedEvents,
            List<ScheduleVoteSummaryResponse> sharedVotes
    ) {
    }
}
