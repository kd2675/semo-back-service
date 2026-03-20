package semo.back.service.feature.schedule.biz;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import semo.back.service.database.pub.entity.ClubNotice;
import semo.back.service.database.pub.entity.ClubScheduleEvent;
import semo.back.service.database.pub.entity.ClubScheduleVote;
import semo.back.service.database.pub.repository.ClubNoticeRepository;
import semo.back.service.database.pub.repository.ClubScheduleEventRepository;
import semo.back.service.database.pub.repository.ClubScheduleVoteRepository;
import semo.back.service.feature.club.biz.ClubAccessResolver;
import semo.back.service.feature.notice.biz.ClubNoticeService;
import semo.back.service.feature.notice.vo.ClubNoticeSummaryResponse;
import semo.back.service.feature.share.biz.ClubContentShareService;
import semo.back.service.feature.schedule.vo.ClubScheduleHomeResponse;
import semo.back.service.feature.schedule.vo.ScheduleEventSummaryResponse;
import semo.back.service.feature.schedule.vo.ScheduleVoteSummaryResponse;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubScheduleHomeService {
    private final ClubAccessResolver clubAccessResolver;
    private final ClubScheduleService clubScheduleService;
    private final ClubNoticeService clubNoticeService;
    private final ClubNoticeRepository clubNoticeRepository;
    private final ClubScheduleEventRepository clubScheduleEventRepository;
    private final ClubScheduleVoteRepository clubScheduleVoteRepository;
    private final ClubContentShareService clubContentShareService;

    public ClubScheduleHomeResponse getScheduleHome(Long clubId, String userKey) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);

        List<ClubScheduleEvent> events = loadHomeEvents(access, clubId);
        List<ScheduleEventSummaryResponse> eventSummaries = clubScheduleService.getEventSummariesForHome(access, events);
        List<ClubNotice> sharedNotices = loadCalendarSharedNotices(clubId);
        List<ClubNoticeSummaryResponse> sharedNoticeSummaries = clubNoticeService
                .toNoticeSummaries(access, sharedNotices)
                .stream()
                .limit(20)
                .toList();
        List<ClubScheduleVote> sharedVotes = loadCalendarSharedVotes(clubId);
        List<ScheduleVoteSummaryResponse> sharedVoteSummaries = clubScheduleService
                .getVoteSummariesForHome(access, sharedVotes)
                .stream()
                .limit(20)
                .toList();
        LocalDate today = LocalDate.now();

        return new ClubScheduleHomeResponse(
                access.club().getClubId(),
                access.club().getName(),
                access.isAdmin(),
                access.isAdmin(),
                events.size(),
                (int) events.stream()
                        .filter(event -> {
                            LocalDate endDate = event.getEndAt() == null
                                    ? event.getStartAt().toLocalDate()
                                    : event.getEndAt().toLocalDate();
                            return !endDate.isBefore(today);
                        })
                        .count(),
                events.size(),
                eventSummaries.stream().limit(20).toList(),
                sharedNoticeSummaries,
                sharedVoteSummaries
        );
    }

    private List<ClubScheduleEvent> loadHomeEvents(ClubAccessResolver.ClubAccess access, Long clubId) {
        return clubScheduleEventRepository.findAllActiveEvents(clubId).stream()
                .filter(event -> access.isAdmin() || event.getAuthorClubProfileId().equals(access.clubProfile().getClubProfileId()))
                .sorted(Comparator.comparing(ClubScheduleEvent::getStartAt).reversed().thenComparing(ClubScheduleEvent::getEventId).reversed())
                .toList();
    }

    private List<ClubNotice> loadCalendarSharedNotices(Long clubId) {
        List<Long> noticeIds = clubContentShareService.getCalendarContentIds(clubId, ClubContentShareService.CONTENT_NOTICE);
        if (noticeIds.isEmpty()) {
            return List.of();
        }
        Map<Long, ClubNotice> noticeById = clubNoticeRepository.findAllByNoticeIdIn(noticeIds).stream()
                .collect(Collectors.toMap(ClubNotice::getNoticeId, Function.identity()));
        return noticeIds.stream()
                .map(noticeById::get)
                .filter(notice -> notice != null && !notice.isDeleted())
                .toList();
    }

    private List<ClubScheduleVote> loadCalendarSharedVotes(Long clubId) {
        List<Long> voteIds = clubContentShareService.getCalendarContentIds(clubId, ClubContentShareService.CONTENT_SCHEDULE_VOTE);
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
}
