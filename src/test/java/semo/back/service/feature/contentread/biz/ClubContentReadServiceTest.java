package semo.back.service.feature.contentread.biz;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import semo.back.service.database.pub.entity.ClubBoardItem;
import semo.back.service.database.pub.entity.ClubCalendarItem;
import semo.back.service.database.pub.repository.ClubBoardItemReadRepository;
import semo.back.service.database.pub.repository.ClubBoardItemRepository;
import semo.back.service.database.pub.repository.ClubCalendarItemReadRepository;
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
import semo.back.service.feature.notice.biz.ClubBoardFeedService;
import semo.back.service.feature.notice.biz.ClubNoticeService;
import semo.back.service.feature.notice.vo.UpsertClubNoticeRequest;
import semo.back.service.feature.schedule.biz.ClubScheduleService;

import static org.assertj.core.api.Assertions.assertThat;
import static semo.back.service.support.TestCatalogSeeder.seedFeatureCatalogs;

@SpringBootTest
@ActiveProfiles("test")
class ClubContentReadServiceTest {

    @Autowired
    private ClubContentReadService clubContentReadService;

    @Autowired
    private ClubBoardFeedService clubBoardFeedService;

    @Autowired
    private ClubScheduleService clubScheduleService;

    @Autowired
    private ClubNoticeService clubNoticeService;

    @Autowired
    private ClubService clubService;

    @Autowired
    private ClubFeatureService clubFeatureService;

    @Autowired
    private ClubBoardItemReadRepository clubBoardItemReadRepository;

    @Autowired
    private ClubCalendarItemReadRepository clubCalendarItemReadRepository;

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
        clubBoardItemReadRepository.deleteAll();
        clubCalendarItemReadRepository.deleteAll();
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
    void boardAndCalendarReadsStayIdempotentAndExposeReaderStatus() {
        String userKey = "read-owner-001";
        Long clubId = clubService.createClub(
                userKey,
                "읽음 회장",
                new CreateClubRequest(
                        "Read Club",
                        "읽음 기능 테스트",
                        "OTHER",
                        "PUBLIC",
                        "APPROVAL",
                        null
                )
        ).clubId();
        enableNoticeFeature(clubId, userKey);

        var notice = clubNoticeService.createNotice(
                clubId,
                userKey,
                new UpsertClubNoticeRequest(
                        "읽음 추적 공지",
                        "게시판과 캘린더에서 모두 확인합니다.",
                        null,
                        "서울숲",
                        "2030-07-10T19:00",
                        "2030-07-10T21:00",
                        true,
                        true,
                        true,
                        false
                )
        );

        ClubBoardItem boardItem = clubBoardItemRepository
                .findByClubIdAndContentTypeAndContentId(clubId, "NOTICE", notice.noticeId())
                .orElseThrow();
        ClubCalendarItem calendarItem = clubCalendarItemRepository
                .findByClubIdAndContentTypeAndContentId(clubId, "NOTICE", notice.noticeId())
                .orElseThrow();

        var firstBoardRead = clubContentReadService.recordBoardItemRead(clubId, boardItem.getBoardItemId(), userKey);
        var secondBoardRead = clubContentReadService.recordBoardItemRead(clubId, boardItem.getBoardItemId(), userKey);
        var firstCalendarRead = clubContentReadService.recordCalendarItemRead(clubId, calendarItem.getCalendarItemId(), userKey);
        var secondCalendarRead = clubContentReadService.recordCalendarItemRead(clubId, calendarItem.getCalendarItemId(), userKey);

        assertThat(firstBoardRead.readCount()).isEqualTo(1);
        assertThat(secondBoardRead.readCount()).isEqualTo(1);
        assertThat(firstCalendarRead.readCount()).isEqualTo(1);
        assertThat(secondCalendarRead.readCount()).isEqualTo(1);

        var boardStatus = clubContentReadService.getBoardItemReadStatus(clubId, boardItem.getBoardItemId(), userKey);
        var calendarStatus = clubContentReadService.getCalendarItemReadStatus(clubId, calendarItem.getCalendarItemId(), userKey);
        var boardFeed = clubBoardFeedService.getBoardFeed(clubId, userKey, null, false, null, 10);
        var schedule = clubScheduleService.getClubSchedule(clubId, userKey, 2030, 7);

        assertThat(boardStatus.readCount()).isEqualTo(1);
        assertThat(boardStatus.activeMemberCount()).isEqualTo(1);
        assertThat(boardStatus.unreadCount()).isZero();
        assertThat(boardStatus.readers()).hasSize(1);
        assertThat(boardStatus.readers().getFirst().displayName()).isEqualTo("읽음 회장");

        assertThat(calendarStatus.readCount()).isEqualTo(1);
        assertThat(calendarStatus.activeMemberCount()).isEqualTo(1);
        assertThat(calendarStatus.unreadCount()).isZero();
        assertThat(calendarStatus.readers()).hasSize(1);
        assertThat(calendarStatus.readers().getFirst().displayName()).isEqualTo("읽음 회장");

        assertThat(boardFeed.items())
                .extracting(item -> item.readCount())
                .contains(1);
        assertThat(schedule.items())
                .extracting(item -> item.readCount())
                .contains(1);
    }

    @Test
    void deletingSharedNoticeAlsoRemovesBoardAndCalendarReadHistory() {
        String userKey = "read-owner-delete-001";
        Long clubId = clubService.createClub(
                userKey,
                "삭제 회장",
                new CreateClubRequest(
                        "Delete Read Club",
                        "공유 삭제 테스트",
                        "OTHER",
                        "PUBLIC",
                        "APPROVAL",
                        null
                )
        ).clubId();
        enableNoticeFeature(clubId, userKey);

        var notice = clubNoticeService.createNotice(
                clubId,
                userKey,
                new UpsertClubNoticeRequest(
                        "삭제 테스트 공지",
                        "읽음 기록이 있어도 안전하게 삭제되어야 합니다.",
                        null,
                        "잠실",
                        "2030-08-15T18:00",
                        "2030-08-15T20:00",
                        true,
                        true,
                        false,
                        false
                )
        );

        ClubBoardItem boardItem = clubBoardItemRepository
                .findByClubIdAndContentTypeAndContentId(clubId, "NOTICE", notice.noticeId())
                .orElseThrow();
        ClubCalendarItem calendarItem = clubCalendarItemRepository
                .findByClubIdAndContentTypeAndContentId(clubId, "NOTICE", notice.noticeId())
                .orElseThrow();

        clubContentReadService.recordBoardItemRead(clubId, boardItem.getBoardItemId(), userKey);
        clubContentReadService.recordCalendarItemRead(clubId, calendarItem.getCalendarItemId(), userKey);

        clubNoticeService.deleteNotice(clubId, notice.noticeId(), userKey);

        assertThat(clubBoardItemRepository.findByClubIdAndContentTypeAndContentId(clubId, "NOTICE", notice.noticeId())).isEmpty();
        assertThat(clubCalendarItemRepository.findByClubIdAndContentTypeAndContentId(clubId, "NOTICE", notice.noticeId())).isEmpty();
        assertThat(clubBoardItemReadRepository.countByBoardItemId(boardItem.getBoardItemId())).isZero();
        assertThat(clubCalendarItemReadRepository.countByCalendarItemId(calendarItem.getCalendarItemId())).isZero();
    }

    private void enableNoticeFeature(Long clubId, String userKey) {
        clubFeatureService.updateClubFeatures(
                clubId,
                userKey,
                new UpdateClubFeaturesRequest(java.util.List.of("NOTICE"))
        );
    }
}
