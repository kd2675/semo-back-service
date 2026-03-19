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

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubNoticeFeatureService {
    private final ClubAccessResolver clubAccessResolver;
    private final ClubNoticeService clubNoticeService;
    private final ClubScheduleService clubScheduleService;
    private final ClubScheduleEventRepository clubScheduleEventRepository;
    private final ClubScheduleVoteRepository clubScheduleVoteRepository;

    public ClubNoticeHomeResponse getNoticeHome(Long clubId, String userKey) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);

        List<ClubNotice> notices = access.isAdmin()
                ? clubNoticeService.getActiveNotices(clubId)
                : clubNoticeService.getDirectNoticesByAuthor(clubId, access.clubProfile().getClubProfileId());
        List<ClubNotice> homeNotices = access.isAdmin()
                ? notices.stream()
                        .filter(notice -> clubNoticeService.canManageNotice(access, notice.getAuthorClubProfileId()))
                        .limit(20)
                        .toList()
                : notices.stream().limit(20).toList();
        List<ClubNoticeSummaryResponse> manageableNotices = clubNoticeService.toNoticeSummaries(access, homeNotices);
        List<ClubScheduleEvent> sharedEvents = clubScheduleEventRepository.findAllByClubIdAndSharedToNoticeTrue(clubId);
        List<ScheduleEventSummaryResponse> sharedEventSummaries = clubScheduleService
                .getEventSummariesForHome(access, sharedEvents)
                .stream()
                .limit(20)
                .toList();
        List<ClubScheduleVote> sharedVotes = clubScheduleVoteRepository.findAllByClubIdAndSharedToNoticeTrue(clubId);
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
                notices.size(),
                (int) notices.stream().filter(ClubNotice::isPinned).count(),
                (int) notices.stream().filter(notice -> notice.getScheduleAt() != null).count(),
                (int) notices.stream()
                        .filter(notice -> notice.getPublishedAt() != null && notice.getPublishedAt().toLocalDate().isEqual(today))
                        .count(),
                notices.size(),
                manageableNotices,
                sharedEventSummaries,
                sharedVoteSummaries
        );
    }
}
