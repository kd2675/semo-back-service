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
import semo.back.service.feature.schedule.vo.ClubScheduleHomeResponse;
import semo.back.service.feature.schedule.vo.ScheduleEventSummaryResponse;
import semo.back.service.feature.schedule.vo.ScheduleVoteSummaryResponse;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

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

    public ClubScheduleHomeResponse getScheduleHome(Long clubId, String userKey) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);

        List<ClubScheduleEvent> events = loadHomeEvents(access, clubId);
        List<ScheduleEventSummaryResponse> eventSummaries = clubScheduleService.getEventSummariesForHome(access, events);
        List<ClubNotice> sharedNotices = clubNoticeRepository
                .findAllByClubIdAndSharedToScheduleTrueAndDeletedFalseOrderByPublishedAtDescNoticeIdDesc(clubId);
        List<ClubNoticeSummaryResponse> sharedNoticeSummaries = clubNoticeService
                .toNoticeSummaries(access, sharedNotices)
                .stream()
                .limit(20)
                .toList();
        List<ClubScheduleVote> sharedVotes = clubScheduleVoteRepository.findAllByClubIdAndSharedToScheduleTrue(clubId);
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
}
