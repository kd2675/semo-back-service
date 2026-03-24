package semo.back.service.feature.notice.biz;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import semo.back.service.common.exception.SemoException;
import semo.back.service.database.pub.repository.ClubBoardItemRepository;
import semo.back.service.database.pub.repository.ClubCalendarItemRepository;
import semo.back.service.database.pub.repository.ClubEventParticipantRepository;
import semo.back.service.database.pub.repository.ClubFeatureRepository;
import semo.back.service.database.pub.repository.ClubMemberRepository;
import semo.back.service.database.pub.repository.ClubNoticeRepository;
import semo.back.service.database.pub.repository.ClubProfileRepository;
import semo.back.service.database.pub.repository.ClubRepository;
import semo.back.service.database.pub.repository.ClubScheduleEventRepository;
import semo.back.service.database.pub.repository.ClubScheduleVoteOptionRepository;
import semo.back.service.database.pub.repository.ClubScheduleVoteRepository;
import semo.back.service.database.pub.repository.ClubScheduleVoteSelectionRepository;
import semo.back.service.database.pub.repository.FeatureCatalogRepository;
import semo.back.service.database.pub.repository.ProfileUserRepository;
import semo.back.service.feature.club.biz.ClubService;
import semo.back.service.feature.club.vo.CreateClubRequest;
import semo.back.service.feature.clubfeature.biz.ClubFeatureService;
import semo.back.service.feature.clubfeature.vo.UpdateClubFeaturesRequest;
import semo.back.service.feature.notice.vo.UpsertClubNoticeRequest;
import semo.back.service.feature.schedule.biz.ClubScheduleService;
import semo.back.service.feature.schedule.vo.UpsertScheduleEventRequest;
import semo.back.service.feature.schedule.vo.UpsertScheduleVoteRequest;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.locks.LockSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static semo.back.service.support.TestCatalogSeeder.seedFeatureCatalogs;

@SpringBootTest
@ActiveProfiles("test")
class ClubBoardFeedServiceTest {

    @Autowired
    private ClubBoardFeedService clubBoardFeedService;

    @Autowired
    private ClubNoticeService clubNoticeService;

    @Autowired
    private ClubScheduleService clubScheduleService;

    @Autowired
    private ClubService clubService;

    @Autowired
    private ClubFeatureService clubFeatureService;

    @Autowired
    private ClubBoardItemRepository clubBoardItemRepository;

    @Autowired
    private ClubCalendarItemRepository clubCalendarItemRepository;

    @Autowired
    private ClubEventParticipantRepository clubEventParticipantRepository;

    @Autowired
    private ClubFeatureRepository clubFeatureRepository;

    @Autowired
    private ClubNoticeRepository clubNoticeRepository;

    @Autowired
    private ClubProfileRepository clubProfileRepository;

    @Autowired
    private ClubMemberRepository clubMemberRepository;

    @Autowired
    private ClubRepository clubRepository;

    @Autowired
    private ClubScheduleEventRepository clubScheduleEventRepository;

    @Autowired
    private ClubScheduleVoteOptionRepository clubScheduleVoteOptionRepository;

    @Autowired
    private ClubScheduleVoteRepository clubScheduleVoteRepository;

    @Autowired
    private ClubScheduleVoteSelectionRepository clubScheduleVoteSelectionRepository;

    @Autowired
    private ProfileUserRepository profileUserRepository;

    @Autowired
    private FeatureCatalogRepository featureCatalogRepository;

    @BeforeEach
    void setUp() {
        clubScheduleVoteSelectionRepository.deleteAll();
        clubScheduleVoteOptionRepository.deleteAll();
        clubScheduleVoteRepository.deleteAll();
        clubEventParticipantRepository.deleteAll();
        clubScheduleEventRepository.deleteAll();
        clubBoardItemRepository.deleteAll();
        clubCalendarItemRepository.deleteAll();
        clubNoticeRepository.deleteAll();
        clubFeatureRepository.deleteAll();
        clubProfileRepository.deleteAll();
        clubMemberRepository.deleteAll();
        clubRepository.deleteAll();
        profileUserRepository.deleteAll();
        seedFeatureCatalogs(featureCatalogRepository);
    }

    @Test
    void boardFeedUsesBoardItemCursorOrderAcrossMixedContent() {
        String userKey = "board-owner-001";
        Long clubId = clubService.createClub(
                userKey,
                "Board Owner",
                new CreateClubRequest(
                        "Board Feed Lab",
                        "통합 게시판 피드 테스트",
                        "OTHER",
                        "PUBLIC",
                        "APPROVAL",
                        null
                )
        ).clubId();
        enableNoticeFeature(clubId, userKey);

        var hiddenNotice = clubNoticeService.createNotice(
                clubId,
                userKey,
                new UpsertClubNoticeRequest(
                        "나중에 공유할 공지",
                        "처음에는 보드에 올리지 않습니다.",
                        null,
                        null,
                        null,
                        null,
                        false,
                        false,
                        false,
                        false
                )
        );

        pauseForOrdering();

        var event = clubScheduleService.createScheduleEvent(
                clubId,
                userKey,
                new UpsertScheduleEventRequest(
                        "보드 일정",
                        "2030-06-10",
                        null,
                        "19:00",
                        "21:00",
                        null,
                        "한강공원",
                        null,
                        true,
                        false,
                        null,
                        false,
                        false,
                        true,
                        false,
                        false
                )
        );

        pauseForOrdering();

        var vote = clubScheduleService.createScheduleVote(
                clubId,
                userKey,
                new UpsertScheduleVoteRequest(
                        "보드 투표",
                        "2030-06-10",
                        "2030-06-12",
                        "18:00",
                        "22:00",
                        List.of("A안", "B안"),
                        true,
                        false,
                        false,
                        false
                )
        );

        pauseForOrdering();

        clubNoticeService.updateNotice(
                clubId,
                hiddenNotice.noticeId(),
                userKey,
                new UpsertClubNoticeRequest(
                        "나중에 공유할 공지",
                        "가장 마지막에 게시판에 공유합니다.",
                        null,
                        null,
                        null,
                        null,
                        true,
                        false,
                        false,
                        false
                )
        );

        var firstPage = clubBoardFeedService.getBoardFeed(clubId, userKey, null, false, null, 2);
        var secondPage = clubBoardFeedService.getBoardFeed(
                clubId,
                userKey,
                null,
                false,
                firstPage.nextCursorBoardItemId(),
                2
        );

        assertThat(firstPage.items()).hasSize(2);
        assertThat(firstPage.items().get(0).contentType()).isEqualTo("NOTICE");
        assertThat(firstPage.items().get(0).notice()).isNotNull();
        assertThat(firstPage.items().get(0).notice().noticeId()).isEqualTo(hiddenNotice.noticeId());
        assertThat(firstPage.items().get(1).contentType()).isEqualTo("SCHEDULE_VOTE");
        assertThat(firstPage.items().get(1).vote()).isNotNull();
        assertThat(firstPage.items().get(1).vote().voteId()).isEqualTo(vote.voteId());
        assertThat(firstPage.hasNext()).isTrue();

        assertThat(secondPage.items()).hasSize(1);
        assertThat(secondPage.items().get(0).contentType()).isEqualTo("SCHEDULE_EVENT");
        assertThat(secondPage.items().get(0).event()).isNotNull();
        assertThat(secondPage.items().get(0).event().eventId()).isEqualTo(event.eventId());
        assertThat(secondPage.hasNext()).isFalse();
    }

    @Test
    void boardFeedPinnedOnlyIncludesPinnedNoticeEventAndVote() {
        String userKey = "board-owner-002";
        Long clubId = clubService.createClub(
                userKey,
                "Board Owner",
                new CreateClubRequest(
                        "Pinned Feed Lab",
                        "핀 게시물 테스트",
                        "OTHER",
                        "PUBLIC",
                        "APPROVAL",
                        null
                )
        ).clubId();
        enableNoticeFeature(clubId, userKey);

        var pinnedNotice = clubNoticeService.createNotice(
                clubId,
                userKey,
                new UpsertClubNoticeRequest(
                        "핀 공지",
                        "공지 핀 테스트",
                        null,
                        null,
                        null,
                        null,
                        true,
                        false,
                        false,
                        true
                )
        );

        pauseForOrdering();

        var pinnedEvent = clubScheduleService.createScheduleEvent(
                clubId,
                userKey,
                new UpsertScheduleEventRequest(
                        "핀 일정",
                        "2030-09-10",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        false,
                        false,
                        null,
                        false,
                        false,
                        true,
                        false,
                        true
                )
        );

        pauseForOrdering();

        var pinnedVote = clubScheduleService.createScheduleVote(
                clubId,
                userKey,
                new UpsertScheduleVoteRequest(
                        "핀 투표",
                        "2030-09-10",
                        "2030-09-11",
                        null,
                        null,
                        List.of("찬성", "보류"),
                        true,
                        false,
                        false,
                        true
                )
        );

        pauseForOrdering();

        clubScheduleService.createScheduleEvent(
                clubId,
                userKey,
                new UpsertScheduleEventRequest(
                        "일반 일정",
                        "2030-09-12",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        false,
                        false,
                        null,
                        false,
                        false,
                        true,
                        false,
                        false
                )
        );

        var pinnedFeed = clubBoardFeedService.getBoardFeed(clubId, userKey, null, true, null, 10);

        assertThat(pinnedFeed.items()).hasSize(3);
        assertThat(pinnedFeed.items().stream().map(item -> item.contentType())).containsExactlyInAnyOrder(
                "NOTICE",
                "SCHEDULE_EVENT",
                "SCHEDULE_VOTE"
        );
        assertThat(pinnedFeed.items().stream().map(item -> item.notice() != null ? item.notice().noticeId() : null))
                .contains(pinnedNotice.noticeId());
        assertThat(pinnedFeed.items().stream().map(item -> item.event() != null ? item.event().eventId() : null))
                .contains(pinnedEvent.eventId());
        assertThat(pinnedFeed.items().stream().map(item -> item.vote() != null ? item.vote().voteId() : null))
                .contains(pinnedVote.voteId());
    }

    @Test
    void boardFeedRejectsInvalidBoardItemCursor() {
        String userKey = "board-owner-002";
        Long clubId = clubService.createClub(
                userKey,
                "Board Owner",
                new CreateClubRequest(
                        "Board Feed Validation Lab",
                        "보드 커서 검증 테스트",
                        "OTHER",
                        "PUBLIC",
                        "APPROVAL",
                        null
                )
        ).clubId();

        assertThatThrownBy(() -> clubBoardFeedService.getBoardFeed(clubId, userKey, null, false, 0L, 10))
                .isInstanceOf(SemoException.ValidationException.class)
                .hasMessageContaining("잘못된 커서 값");
    }

    private void enableNoticeFeature(Long clubId, String userKey) {
        clubFeatureService.updateClubFeatures(
                clubId,
                userKey,
                new UpdateClubFeaturesRequest(List.of("NOTICE"))
        );
    }

    private void pauseForOrdering() {
        LockSupport.parkNanos(Duration.ofMillis(5).toNanos());
    }
}
