package semo.back.service.feature.schedule.biz;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import semo.back.service.common.exception.SemoException;
import semo.back.service.database.pub.entity.ClubMember;
import semo.back.service.database.pub.entity.ProfileUser;
import semo.back.service.database.pub.repository.ClubFeatureRepository;
import semo.back.service.database.pub.repository.ClubMemberRepository;
import semo.back.service.database.pub.repository.ClubProfileRepository;
import semo.back.service.database.pub.repository.ClubRepository;
import semo.back.service.database.pub.repository.ClubScheduleEventRepository;
import semo.back.service.database.pub.repository.FeatureCatalogRepository;
import semo.back.service.database.pub.repository.ProfileUserRepository;
import semo.back.service.database.pub.repository.SchedulePermissionPolicyRepository;
import semo.back.service.feature.club.biz.ClubService;
import semo.back.service.feature.club.vo.CreateClubRequest;
import semo.back.service.feature.clubfeature.biz.ClubFeatureService;
import semo.back.service.feature.clubfeature.vo.UpdateClubFeaturesRequest;
import semo.back.service.feature.schedule.vo.UpdateClubAdminScheduleSettingsRequest;
import semo.back.service.feature.schedule.vo.UpsertScheduleEventRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static semo.back.service.support.TestCatalogSeeder.seedFeatureCatalogs;

@SpringBootTest
@ActiveProfiles("test")
class ClubSchedulePermissionServiceTest {

    @Autowired
    private ClubService clubService;

    @Autowired
    private ClubFeatureService clubFeatureService;

    @Autowired
    private ClubScheduleService clubScheduleService;

    @Autowired
    private ClubSchedulePermissionService clubSchedulePermissionService;

    @Autowired
    private SchedulePermissionPolicyRepository schedulePermissionPolicyRepository;

    @Autowired
    private ClubScheduleEventRepository clubScheduleEventRepository;

    @Autowired
    private ClubFeatureRepository clubFeatureRepository;

    @Autowired
    private ClubProfileRepository clubProfileRepository;

    @Autowired
    private ClubMemberRepository clubMemberRepository;

    @Autowired
    private ClubRepository clubRepository;

    @Autowired
    private ProfileUserRepository profileUserRepository;

    @Autowired
    private FeatureCatalogRepository featureCatalogRepository;

    @BeforeEach
    void setUp() {
        clubScheduleEventRepository.deleteAll();
        schedulePermissionPolicyRepository.deleteAll();
        clubFeatureRepository.deleteAll();
        clubProfileRepository.deleteAll();
        clubMemberRepository.deleteAll();
        clubRepository.deleteAll();
        profileUserRepository.deleteAll();
        seedFeatureCatalogs(featureCatalogRepository);
    }

    @AfterEach
    void tearDown() {
        clubScheduleEventRepository.deleteAll();
        schedulePermissionPolicyRepository.deleteAll();
        clubFeatureRepository.deleteAll();
        clubProfileRepository.deleteAll();
        clubMemberRepository.deleteAll();
        clubRepository.deleteAll();
        profileUserRepository.deleteAll();
    }

    @Test
    void adminScheduleSettingsExposeDefaultPolicy() {
        String ownerUserKey = "schedule-policy-owner-001";
        Long clubId = createScheduleClub(ownerUserKey, "Schedule Policy Club");

        var response = clubSchedulePermissionService.getAdminSettings(clubId, ownerUserKey);

        assertThat(response.allowMemberCreate()).isFalse();
        assertThat(response.allowMemberUpdate()).isTrue();
        assertThat(response.allowMemberDelete()).isTrue();
    }

    @Test
    void memberScheduleCreateUpdateDeleteFollowConfiguredPolicyWhileAdminStillCanManage() {
        String ownerUserKey = "schedule-policy-owner-002";
        String memberUserKey = "schedule-policy-member-001";
        Long clubId = createScheduleClub(ownerUserKey, "Schedule Permission Lab");
        addActiveMember(clubId, memberUserKey, "Schedule Member");

        assertThatThrownBy(() -> clubScheduleService.createScheduleEvent(
                clubId,
                memberUserKey,
                eventRequest("멤버 일정", "기본 설정에서는 작성 불가")
        ))
                .isInstanceOf(SemoException.ForbiddenException.class)
                .hasMessageContaining("일정 작성 권한");

        clubSchedulePermissionService.updateAdminSettings(
                clubId,
                ownerUserKey,
                new UpdateClubAdminScheduleSettingsRequest(true, false, false)
        );

        var created = clubScheduleService.createScheduleEvent(
                clubId,
                memberUserKey,
                eventRequest("멤버 일정", "생성 허용 후 작성")
        );

        assertThat(created.eventId()).isNotNull();

        assertThatThrownBy(() -> clubScheduleService.updateScheduleEvent(
                clubId,
                created.eventId(),
                memberUserKey,
                eventRequest("멤버 일정 수정", "수정은 아직 불가")
        ))
                .isInstanceOf(SemoException.ForbiddenException.class)
                .hasMessageContaining("일정 수정 권한");

        assertThatThrownBy(() -> clubScheduleService.deleteScheduleEvent(
                clubId,
                created.eventId(),
                memberUserKey
        ))
                .isInstanceOf(SemoException.ForbiddenException.class)
                .hasMessageContaining("일정 삭제 권한");

        var adminUpdated = clubScheduleService.updateScheduleEvent(
                clubId,
                created.eventId(),
                ownerUserKey,
                eventRequest("관리자가 수정한 일정", "관리자는 계속 수정 가능")
        );

        assertThat(adminUpdated.title()).isEqualTo("관리자가 수정한 일정");

        clubSchedulePermissionService.updateAdminSettings(
                clubId,
                ownerUserKey,
                new UpdateClubAdminScheduleSettingsRequest(true, true, true)
        );

        var memberUpdated = clubScheduleService.updateScheduleEvent(
                clubId,
                created.eventId(),
                memberUserKey,
                eventRequest("멤버가 수정한 일정", "권한 허용 후 수정")
        );

        assertThat(memberUpdated.title()).isEqualTo("멤버가 수정한 일정");

        clubScheduleService.deleteScheduleEvent(clubId, created.eventId(), memberUserKey);

        assertThat(clubScheduleEventRepository.findByEventIdAndClubId(created.eventId(), clubId)).isEmpty();
    }

    private Long createScheduleClub(String ownerUserKey, String clubName) {
        Long clubId = clubService.createClub(
                ownerUserKey,
                "Schedule Owner",
                new CreateClubRequest(
                        clubName,
                        "일정 권한 테스트",
                        "OTHER",
                        "PUBLIC",
                        "APPROVAL",
                        null
                )
        ).clubId();

        clubFeatureService.updateClubFeatures(
                clubId,
                ownerUserKey,
                new UpdateClubFeaturesRequest(List.of("SCHEDULE_MANAGE"))
        );
        return clubId;
    }

    private void addActiveMember(Long clubId, String userKey, String displayName) {
        ProfileUser profileUser = profileUserRepository.save(ProfileUser.builder()
                .userKey(userKey)
                .displayName(displayName)
                .tagline(displayName + " 소개")
                .profileColor("#135bec")
                .build());

        clubMemberRepository.save(ClubMember.builder()
                .clubId(clubId)
                .profileId(profileUser.getProfileId())
                .roleCode("MEMBER")
                .membershipStatus("ACTIVE")
                .joinedAt(LocalDateTime.now())
                .lastActivityAt(LocalDateTime.now())
                .build());
    }

    private UpsertScheduleEventRequest eventRequest(String title, String locationLabel) {
        return new UpsertScheduleEventRequest(
                title,
                "2030-05-01",
                null,
                null,
                null,
                null,
                locationLabel,
                null,
                false,
                false,
                null,
                false,
                false,
                false,
                true
        );
    }
}
