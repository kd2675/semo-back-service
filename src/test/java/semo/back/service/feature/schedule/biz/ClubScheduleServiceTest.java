package semo.back.service.feature.schedule.biz;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import semo.back.service.database.pub.entity.ClubMember;
import semo.back.service.database.pub.repository.ClubAttendanceCheckInRepository;
import semo.back.service.database.pub.repository.ClubAttendanceSessionRepository;
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
import semo.back.service.feature.notice.biz.ClubNoticeService;
import semo.back.service.feature.notice.vo.UpsertClubNoticeRequest;
import semo.back.service.feature.schedule.vo.UpsertScheduleEventRequest;
import semo.back.service.feature.schedule.vo.UpdateScheduleEventParticipationRequest;
import semo.back.service.feature.schedule.vo.SubmitScheduleVoteSelectionRequest;
import semo.back.service.feature.schedule.vo.UpsertScheduleVoteRequest;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static semo.back.service.support.TestCatalogSeeder.seedFeatureCatalogs;

@SpringBootTest
@ActiveProfiles("test")
class ClubScheduleServiceTest {

    @Autowired
    private ClubScheduleService clubScheduleService;

    @Autowired
    private ClubNoticeService clubNoticeService;

    @Autowired
    private ClubService clubService;

    @Autowired
    private ClubFeatureService clubFeatureService;

    @Autowired
    private ClubAttendanceCheckInRepository clubAttendanceCheckInRepository;

    @Autowired
    private ClubAttendanceSessionRepository clubAttendanceSessionRepository;

    @Autowired
    private ClubEventParticipantRepository clubEventParticipantRepository;

    @Autowired
    private ClubFeatureRepository clubFeatureRepository;

    @Autowired
    private ClubMemberRepository clubMemberRepository;

    @Autowired
    private ClubNoticeRepository clubNoticeRepository;

    @Autowired
    private ClubProfileRepository clubProfileRepository;

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
        clubNoticeRepository.deleteAll();
        clubAttendanceCheckInRepository.deleteAll();
        clubAttendanceSessionRepository.deleteAll();
        clubFeatureRepository.deleteAll();
        clubProfileRepository.deleteAll();
        clubMemberRepository.deleteAll();
        clubRepository.deleteAll();
        profileUserRepository.deleteAll();
        seedFeatureCatalogs(featureCatalogRepository);
    }

    @Test
    void scheduleManagementReturnsEventAndVoteSummariesWithOptionalFields() {
        Long clubId = clubService.createClub(
                "schedule-owner-001",
                "Schedule Owner",
                new CreateClubRequest(
                        "Schedule Lab",
                        "일정 재구성 테스트",
                        "OTHER",
                        "PUBLIC",
                        "APPROVAL",
                        null
                )
        ).clubId();

        var createdEvent = clubScheduleService.createScheduleEvent(
                clubId,
                "schedule-owner-001",
                new UpsertScheduleEventRequest(
                        "봄 정모",
                        "2030-04-20",
                        null,
                        null,
                        null,
                        12,
                        "성수역 3번 출구",
                        "신규 멤버도 참석 가능",
                        true,
                        true,
                        15000,
                        false,
                        true,
                        true,
                        true
                )
        );

        var createdVote = clubScheduleService.createScheduleVote(
                clubId,
                "schedule-owner-001",
                new UpsertScheduleVoteRequest(
                        "뒤풀이 장소 투표",
                        "2030-04-20",
                        "2030-04-22",
                        "18:30",
                        "22:00",
                        List.of("성수", "건대", "잠실"),
                        true,
                        true,
                        true
                )
        );

        enableNoticeFeature(clubId, "schedule-owner-001");

        clubNoticeService.createNotice(
                clubId,
                "schedule-owner-001",
                new UpsertClubNoticeRequest(
                        "달력 공지",
                        "달력에서 함께 보여야 하는 공지",
                        null,
                        "성수동",
                        "2030-04-05T09:00",
                        "2030-04-05T11:00",
                        false,
                        true,
                        true,
                        false
                )
        );
        clubNoticeService.createNotice(
                clubId,
                "schedule-owner-001",
                new UpsertClubNoticeRequest(
                        "비공유 공지",
                        "calendar item 에 없어야 하는 공지",
                        null,
                        null,
                        "2030-04-06T09:00",
                        null,
                        false,
                        false,
                        false,
                        false
                )
        );

        var schedule = clubScheduleService.getClubSchedule(clubId, "schedule-owner-001", 2030, 4);
        var eventDetail = clubScheduleService.getScheduleEventDetail(clubId, createdEvent.eventId(), "schedule-owner-001");
        var voteDetail = clubScheduleService.getScheduleVoteDetail(clubId, createdVote.voteId(), "schedule-owner-001");

        assertThat(schedule.items()).hasSize(3);
        assertThat(schedule.items().get(0).contentType()).isEqualTo("NOTICE");
        assertThat(schedule.items().get(0).notice()).isNotNull();
        assertThat(schedule.items().get(0).notice().title()).isEqualTo("달력 공지");
        assertThat(schedule.items().get(1).contentType()).isEqualTo("SCHEDULE_VOTE");
        assertThat(schedule.items().get(2).contentType()).isEqualTo("SCHEDULE_EVENT");
        assertThat(schedule.calendarYear()).isEqualTo(2030);
        assertThat(schedule.calendarMonth()).isEqualTo(4);
        assertThat(schedule.overview().pendingAttendanceCount()).isEqualTo(1);
        assertThat(schedule.overview().pendingVoteCount()).isZero();
        assertThat(schedule.overview().voteCount()).isEqualTo(1);

        assertThat(eventDetail.startDate()).isEqualTo("2030-04-20");
        assertThat(eventDetail.endDate()).isNull();
        assertThat(eventDetail.timeLabel()).isNull();
        assertThat(eventDetail.attendeeLimit()).isEqualTo(12);
        assertThat(eventDetail.locationLabel()).isEqualTo("성수역 3번 출구");
        assertThat(eventDetail.participationConditionText()).isEqualTo("신규 멤버도 참석 가능");
        assertThat(eventDetail.participationEnabled()).isTrue();
        assertThat(eventDetail.feeRequired()).isTrue();
        assertThat(eventDetail.feeAmount()).isEqualTo(15000);
        assertThat(eventDetail.feeAmountUndecided()).isFalse();
        assertThat(eventDetail.feeNWaySplit()).isTrue();
        assertThat(eventDetail.postedToBoard()).isTrue();
        assertThat(eventDetail.linkedNoticeId()).isNull();

        assertThat(voteDetail.options()).hasSize(3);
        assertThat(voteDetail.voteStartDate()).isEqualTo("2030-04-20");
        assertThat(voteDetail.voteEndDate()).isEqualTo("2030-04-22");
        assertThat(voteDetail.voteStartTime()).isEqualTo("18:30");
        assertThat(voteDetail.voteEndTime()).isEqualTo("22:00");
        assertThat(voteDetail.voteTimeLabel()).isEqualTo("18:30 - 22:00");
        assertThat(voteDetail.votingOpen()).isFalse();
        assertThat(voteDetail.postedToBoard()).isTrue();
        assertThat(voteDetail.linkedNoticeId()).isNull();
        assertThat(clubNoticeRepository.count()).isEqualTo(2);
    }

    @Test
    void memberCanRespondInsideScheduleDetailAndVoteDetail() {
        Long clubId = clubService.createClub(
                "schedule-owner-003",
                "Schedule Owner",
                new CreateClubRequest(
                        "Schedule Interaction Lab",
                        "상세 참여 테스트",
                        "OTHER",
                        "PUBLIC",
                        "APPROVAL",
                        null
                )
        ).clubId();

        var createdEvent = clubScheduleService.createScheduleEvent(
                clubId,
                "schedule-owner-003",
                new UpsertScheduleEventRequest(
                        "주말 번개",
                        "2030-05-12",
                        null,
                        "19:00",
                        "21:00",
                        null,
                        "서울숲",
                        null,
                        true,
                        false,
                        null,
                        false,
                        false,
                        false,
                        true
                )
        );
        var createdVote = clubScheduleService.createScheduleVote(
                clubId,
                "schedule-owner-003",
                new UpsertScheduleVoteRequest(
                        "2차 장소 투표",
                        LocalDate.now().minusDays(1).toString(),
                        LocalDate.now().plusDays(1).toString(),
                        "18:00",
                        "22:00",
                        List.of("성수", "건대"),
                        false,
                        true,
                        true
                )
        );

        var participated = clubScheduleService.updateScheduleEventParticipation(
                clubId,
                createdEvent.eventId(),
                "schedule-owner-003",
                new UpdateScheduleEventParticipationRequest("GOING")
        );
        var voted = clubScheduleService.submitScheduleVoteSelection(
                clubId,
                createdVote.voteId(),
                "schedule-owner-003",
                new SubmitScheduleVoteSelectionRequest(
                        clubScheduleVoteOptionRepository.findByVoteIdOrderBySortOrderAscVoteOptionIdAsc(createdVote.voteId())
                                .getFirst()
                                .getVoteOptionId()
                )
        );

        assertThat(participated.myParticipationStatus()).isEqualTo("GOING");
        assertThat(participated.goingCount()).isEqualTo(1);
        assertThat(clubEventParticipantRepository.count()).isOne();

        assertThat(voted.mySelectedOptionId()).isNotNull();
        assertThat(voted.totalResponses()).isEqualTo(1);
        assertThat(clubScheduleVoteSelectionRepository.count()).isOne();
    }

    @Test
    void scheduleReturnsOnlyVotesSharedToScheduleWhenPollFeatureEnabled() {
        Long clubId = clubService.createClub(
                "schedule-owner-006",
                "Schedule Share Owner",
                new CreateClubRequest(
                        "Schedule Share Lab",
                        "일정 공유 투표 테스트",
                        "OTHER",
                        "PUBLIC",
                        "APPROVAL",
                        null
                )
        ).clubId();

        clubScheduleService.createScheduleVote(
                clubId,
                "schedule-owner-006",
                new UpsertScheduleVoteRequest(
                        "일정 공유 투표",
                        "2030-08-10",
                        "2030-08-12",
                        null,
                        null,
                        List.of("찬성", "보류"),
                        false,
                        true,
                        true
                )
        );

        clubScheduleService.createScheduleVote(
                clubId,
                "schedule-owner-006",
                new UpsertScheduleVoteRequest(
                        "투표 홈 전용 투표",
                        "2030-08-10",
                        "2030-08-12",
                        null,
                        null,
                        List.of("A", "B"),
                        false,
                        false,
                        false
                )
        );

        enablePollFeature(clubId, "schedule-owner-006");

        var schedule = clubScheduleService.getClubSchedule(clubId, "schedule-owner-006", 2030, 8);

        assertThat(schedule.items())
                .filteredOn(item -> item.vote() != null)
                .hasSize(1);
        assertThat(schedule.items().stream()
                .map(item -> item.vote() == null ? null : item.vote().title())
                .filter(title -> title != null)
                .toList()).containsExactly("일정 공유 투표");
    }

    @Test
    void managerCanCloseVoteAndFurtherSelectionIsBlocked() {
        Long clubId = clubService.createClub(
                "schedule-owner-005",
                "Schedule Closer",
                new CreateClubRequest(
                        "Schedule Close Lab",
                        "투표 종료 테스트",
                        "OTHER",
                        "PUBLIC",
                        "APPROVAL",
                        null
                )
        ).clubId();

        var createdVote = clubScheduleService.createScheduleVote(
                clubId,
                "schedule-owner-005",
                new UpsertScheduleVoteRequest(
                        "정산 방식 투표",
                        LocalDate.now().minusDays(1).toString(),
                        LocalDate.now().plusDays(1).toString(),
                        "10:00",
                        "23:00",
                        List.of("1/N", "회비"),
                        true,
                        true,
                        true
                )
        );

        var closedVote = clubScheduleService.closeScheduleVote(clubId, createdVote.voteId(), "schedule-owner-005");

        assertThat(closedVote.votingOpen()).isFalse();

        Long optionId = clubScheduleVoteOptionRepository.findByVoteIdOrderBySortOrderAscVoteOptionIdAsc(createdVote.voteId())
                .getFirst()
                .getVoteOptionId();

        assertThatThrownBy(() -> clubScheduleService.submitScheduleVoteSelection(
                clubId,
                createdVote.voteId(),
                "schedule-owner-005",
                new SubmitScheduleVoteSelectionRequest(optionId)
        )).hasMessageContaining("현재 투표 기간이 아닙니다.");
    }

    @Test
    void updateAndDeleteVoteReplacesOptionsAndCleansUpRows() {
        Long clubId = clubService.createClub(
                "schedule-owner-002",
                "Schedule Admin",
                new CreateClubRequest(
                        "Schedule Delete Lab",
                        "투표 정리 테스트",
                        "OTHER",
                        "PUBLIC",
                        "APPROVAL",
                        null
                )
        ).clubId();

        var createdVote = clubScheduleService.createScheduleVote(
                clubId,
                "schedule-owner-002",
                new UpsertScheduleVoteRequest(
                        "1차 장소 투표",
                        "2030-06-01",
                        "2030-06-03",
                        null,
                        null,
                        List.of("A", "B"),
                        false,
                        false,
                        false
                )
        );

        var updatedVote = clubScheduleService.updateScheduleVote(
                clubId,
                createdVote.voteId(),
                "schedule-owner-002",
                new UpsertScheduleVoteRequest(
                        "최종 장소 투표",
                        "2030-06-02",
                        "2030-06-05",
                        "19:00",
                        "21:30",
                        List.of("서울숲", "왕십리", "건대"),
                        true,
                        true,
                        true
                )
        );

        var detail = clubScheduleService.getScheduleVoteDetail(clubId, updatedVote.voteId(), "schedule-owner-002");
        assertThat(detail.title()).isEqualTo("최종 장소 투표");
        assertThat(detail.voteStartDate()).isEqualTo("2030-06-02");
        assertThat(detail.voteEndDate()).isEqualTo("2030-06-05");
        assertThat(detail.voteTimeLabel()).isEqualTo("19:00 - 21:30");
        assertThat(detail.options()).hasSize(3);
        assertThat(updatedVote.postedToBoard()).isTrue();

        clubScheduleService.deleteScheduleVote(clubId, updatedVote.voteId(), "schedule-owner-002");

        assertThat(clubScheduleVoteRepository.count()).isZero();
        assertThat(clubScheduleVoteOptionRepository.count()).isZero();
        assertThat(clubScheduleVoteSelectionRepository.count()).isZero();
    }

    @Test
    void authorCanStillManageOwnEventAndVoteAfterLosingAdminRole() {
        Long clubId = clubService.createClub(
                "schedule-owner-004",
                "Schedule Author",
                new CreateClubRequest(
                        "Schedule Author Lab",
                        "작성자 권한 테스트",
                        "OTHER",
                        "PUBLIC",
                        "APPROVAL",
                        null
                )
        ).clubId();

        var createdEvent = clubScheduleService.createScheduleEvent(
                clubId,
                "schedule-owner-004",
                new UpsertScheduleEventRequest(
                        "작성자 일정",
                        "2030-07-10",
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
                        false,
                        true
                )
        );
        var createdVote = clubScheduleService.createScheduleVote(
                clubId,
                "schedule-owner-004",
                new UpsertScheduleVoteRequest(
                        "작성자 투표",
                        "2030-07-10",
                        "2030-07-11",
                        null,
                        null,
                        List.of("찬성", "반대"),
                        false,
                        false,
                        false
                )
        );

        downgradeMembershipToMember(clubId, "schedule-owner-004");

        var eventDetail = clubScheduleService.getScheduleEventDetail(clubId, createdEvent.eventId(), "schedule-owner-004");
        var voteDetail = clubScheduleService.getScheduleVoteDetail(clubId, createdVote.voteId(), "schedule-owner-004");
        assertThat(eventDetail.canEdit()).isTrue();
        assertThat(eventDetail.canDelete()).isTrue();
        assertThat(voteDetail.canEdit()).isTrue();
        assertThat(voteDetail.canDelete()).isTrue();

        var updatedEvent = clubScheduleService.updateScheduleEvent(
                clubId,
                createdEvent.eventId(),
                "schedule-owner-004",
                new UpsertScheduleEventRequest(
                        "작성자 일정 수정",
                        "2030-07-12",
                        null,
                        null,
                        null,
                        20,
                        "잠실",
                        "작성자 수정 가능",
                        true,
                        false,
                        null,
                        false,
                        false,
                        false,
                        true
                )
        );
        var updatedVote = clubScheduleService.updateScheduleVote(
                clubId,
                createdVote.voteId(),
                "schedule-owner-004",
                new UpsertScheduleVoteRequest(
                        "작성자 투표 수정",
                        "2030-07-12",
                        "2030-07-15",
                        "20:00",
                        "22:00",
                        List.of("점심", "저녁", "야식"),
                        false,
                        false,
                        false
                )
        );

        assertThat(updatedEvent.title()).isEqualTo("작성자 일정 수정");
        assertThat(updatedVote.title()).isEqualTo("작성자 투표 수정");

        clubScheduleService.deleteScheduleEvent(clubId, createdEvent.eventId(), "schedule-owner-004");
        clubScheduleService.deleteScheduleVote(clubId, createdVote.voteId(), "schedule-owner-004");

        assertThat(clubScheduleEventRepository.count()).isZero();
        assertThat(clubScheduleVoteRepository.count()).isZero();
    }

    private void downgradeMembershipToMember(Long clubId, String userKey) {
        Long profileId = profileUserRepository.findByUserKey(userKey)
                .orElseThrow()
                .getProfileId();
        ClubMember membership = clubMemberRepository.findByClubIdAndProfileId(clubId, profileId)
                .orElseThrow();

        clubMemberRepository.save(ClubMember.builder()
                .clubMemberId(membership.getClubMemberId())
                .clubId(membership.getClubId())
                .profileId(membership.getProfileId())
                .roleCode("MEMBER")
                .membershipStatus(membership.getMembershipStatus())
                .joinMessage(membership.getJoinMessage())
                .invitedByProfileId(membership.getInvitedByProfileId())
                .joinedAt(membership.getJoinedAt())
                .lastActivityAt(membership.getLastActivityAt())
                .build());
    }

    private void enablePollFeature(Long clubId, String userKey) {
        clubFeatureService.updateClubFeatures(
                clubId,
                userKey,
                new UpdateClubFeaturesRequest(List.of("POLL"))
        );
    }

    private void enableNoticeFeature(Long clubId, String userKey) {
        clubFeatureService.updateClubFeatures(
                clubId,
                userKey,
                new UpdateClubFeaturesRequest(List.of("NOTICE", "POLL"))
        );
    }
}
