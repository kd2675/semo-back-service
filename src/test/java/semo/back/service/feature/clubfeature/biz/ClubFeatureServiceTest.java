package semo.back.service.feature.clubfeature.biz;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import semo.back.service.database.pub.repository.ClubAttendanceCheckInRepository;
import semo.back.service.database.pub.repository.ClubAttendanceSessionRepository;
import semo.back.service.database.pub.repository.ClubEventParticipantRepository;
import semo.back.service.database.pub.repository.ClubFeatureRepository;
import semo.back.service.database.pub.repository.ClubMemberRepository;
import semo.back.service.database.pub.repository.ClubProfileRepository;
import semo.back.service.database.pub.repository.ClubRepository;
import semo.back.service.database.pub.repository.ClubScheduleEventRepository;
import semo.back.service.database.pub.repository.ClubScheduleVoteOptionRepository;
import semo.back.service.database.pub.repository.ClubScheduleVoteRepository;
import semo.back.service.database.pub.repository.ClubScheduleVoteSelectionRepository;
import semo.back.service.database.pub.repository.ProfileUserRepository;
import semo.back.service.feature.club.biz.ClubService;
import semo.back.service.feature.club.vo.CreateClubRequest;
import semo.back.service.feature.clubfeature.vo.UpdateClubFeaturesRequest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ClubFeatureServiceTest {

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

    @BeforeEach
    void setUp() {
        clubScheduleVoteSelectionRepository.deleteAll();
        clubScheduleVoteOptionRepository.deleteAll();
        clubScheduleVoteRepository.deleteAll();
        clubEventParticipantRepository.deleteAll();
        clubScheduleEventRepository.deleteAll();
        clubAttendanceCheckInRepository.deleteAll();
        clubAttendanceSessionRepository.deleteAll();
        clubFeatureRepository.deleteAll();
        clubProfileRepository.deleteAll();
        clubMemberRepository.deleteAll();
        clubRepository.deleteAll();
        profileUserRepository.deleteAll();
    }

    @Test
    void updateClubFeaturesEnablesAttendanceFeature() {
        Long clubId = clubService.createClub(
                "feature-user-001",
                "Feature Admin",
                new CreateClubRequest(
                        "Feature Club",
                        "기능 테스트",
                        "OTHER",
                        "PUBLIC",
                        "APPROVAL",
                        null
                )
        ).clubId();

        var responses = clubFeatureService.updateClubFeatures(
                clubId,
                "feature-user-001",
                new UpdateClubFeaturesRequest(java.util.List.of("ATTENDANCE"))
        );

        assertThat(responses).hasSize(5);
        assertThat(responses)
                .extracting(response -> response.featureKey() + ":" + response.enabled())
                .containsExactly("ATTENDANCE:true", "TIMELINE:false", "NOTICE:false", "POLL:false", "SCHEDULE_MANAGE:false");
        assertThat(clubFeatureService.isFeatureEnabled(clubId, "ATTENDANCE")).isTrue();
        assertThat(clubFeatureService.isFeatureEnabled(clubId, "TIMELINE")).isFalse();
        assertThat(clubFeatureService.isFeatureEnabled(clubId, "NOTICE")).isFalse();
        assertThat(clubFeatureService.isFeatureEnabled(clubId, "POLL")).isFalse();
        assertThat(clubFeatureService.isFeatureEnabled(clubId, "SCHEDULE_MANAGE")).isFalse();
    }

    @Test
    void updateClubFeaturesEnablesTimelineFeature() {
        Long clubId = clubService.createClub(
                "feature-user-002",
                "Feature Admin",
                new CreateClubRequest(
                        "Timeline Feature Club",
                        "타임라인 테스트",
                        "OTHER",
                        "PUBLIC",
                        "APPROVAL",
                        null
                )
        ).clubId();

        var responses = clubFeatureService.updateClubFeatures(
                clubId,
                "feature-user-002",
                new UpdateClubFeaturesRequest(java.util.List.of("TIMELINE"))
        );

        assertThat(responses).hasSize(5);
        assertThat(responses)
                .extracting(response -> response.featureKey() + ":" + response.enabled())
                .containsExactly("ATTENDANCE:false", "TIMELINE:true", "NOTICE:false", "POLL:false", "SCHEDULE_MANAGE:false");
        assertThat(clubFeatureService.isFeatureEnabled(clubId, "TIMELINE")).isTrue();
    }

    @Test
    void updateClubFeaturesEnablesNoticeFeature() {
        Long clubId = clubService.createClub(
                "feature-user-003",
                "Feature Admin",
                new CreateClubRequest(
                        "Notice Feature Club",
                        "공지 기능 테스트",
                        "OTHER",
                        "PUBLIC",
                        "APPROVAL",
                        null
                )
        ).clubId();

        var responses = clubFeatureService.updateClubFeatures(
                clubId,
                "feature-user-003",
                new UpdateClubFeaturesRequest(java.util.List.of("NOTICE"))
        );

        assertThat(responses).hasSize(5);
        assertThat(responses)
                .extracting(response -> response.featureKey() + ":" + response.enabled())
                .containsExactly("ATTENDANCE:false", "TIMELINE:false", "NOTICE:true", "POLL:false", "SCHEDULE_MANAGE:false");
        assertThat(clubFeatureService.isFeatureEnabled(clubId, "NOTICE")).isTrue();
    }

    @Test
    void updateClubFeaturesEnablesPollFeature() {
        Long clubId = clubService.createClub(
                "feature-user-004",
                "Feature Admin",
                new CreateClubRequest(
                        "Poll Feature Club",
                        "투표 기능 테스트",
                        "OTHER",
                        "PUBLIC",
                        "APPROVAL",
                        null
                )
        ).clubId();

        var responses = clubFeatureService.updateClubFeatures(
                clubId,
                "feature-user-004",
                new UpdateClubFeaturesRequest(java.util.List.of("POLL"))
        );

        assertThat(responses).hasSize(5);
        assertThat(responses)
                .extracting(response -> response.featureKey() + ":" + response.enabled())
                .containsExactly("ATTENDANCE:false", "TIMELINE:false", "NOTICE:false", "POLL:true", "SCHEDULE_MANAGE:false");
        assertThat(clubFeatureService.isFeatureEnabled(clubId, "POLL")).isTrue();
    }

    @Test
    void updateClubFeaturesEnablesScheduleManageFeature() {
        Long clubId = clubService.createClub(
                "feature-user-005",
                "Feature Admin",
                new CreateClubRequest(
                        "Schedule Manage Feature Club",
                        "일정 관리 기능 테스트",
                        "OTHER",
                        "PUBLIC",
                        "APPROVAL",
                        null
                )
        ).clubId();

        var responses = clubFeatureService.updateClubFeatures(
                clubId,
                "feature-user-005",
                new UpdateClubFeaturesRequest(java.util.List.of("SCHEDULE_MANAGE"))
        );

        assertThat(responses).hasSize(5);
        assertThat(responses)
                .extracting(response -> response.featureKey() + ":" + response.enabled())
                .containsExactly("ATTENDANCE:false", "TIMELINE:false", "NOTICE:false", "POLL:false", "SCHEDULE_MANAGE:true");
        assertThat(clubFeatureService.isFeatureEnabled(clubId, "SCHEDULE_MANAGE")).isTrue();
    }
}
