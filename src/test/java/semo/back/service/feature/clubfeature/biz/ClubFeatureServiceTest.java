package semo.back.service.feature.clubfeature.biz;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import semo.back.service.common.exception.SemoException;
import semo.back.service.database.pub.entity.ClubFeature;
import semo.back.service.database.pub.repository.ClubEventParticipantRepository;
import semo.back.service.database.pub.repository.ClubFeatureRepository;
import semo.back.service.database.pub.repository.ClubMemberRepository;
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
import semo.back.service.feature.clubfeature.vo.UpdateClubFeaturesRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static semo.back.service.support.TestCatalogSeeder.seedFeatureCatalogs;

@SpringBootTest
@ActiveProfiles("test")
class ClubFeatureServiceTest {

    @Autowired
    private ClubService clubService;

    @Autowired
    private ClubFeatureService clubFeatureService;

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

    @Autowired
    private FeatureCatalogRepository featureCatalogRepository;

    @BeforeEach
    void setUp() {
        clubScheduleVoteSelectionRepository.deleteAll();
        clubScheduleVoteOptionRepository.deleteAll();
        clubScheduleVoteRepository.deleteAll();
        clubEventParticipantRepository.deleteAll();
        clubScheduleEventRepository.deleteAll();
        clubFeatureRepository.deleteAll();
        clubProfileRepository.deleteAll();
        clubMemberRepository.deleteAll();
        clubRepository.deleteAll();
        profileUserRepository.deleteAll();
        seedFeatureCatalogs(featureCatalogRepository);
    }

    @Test
    void updateClubFeatures_attendanceAutomaticallyIncludesRequiredScheduleAndApprovalQueue() {
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

        assertThat(responses).hasSize(14);
        assertThat(responses)
                .extracting(response -> response.featureKey() + ":" + response.enabled())
                .containsExactlyInAnyOrder(
                        "JOIN_REQUEST:true",
                        "ATTENDANCE:true",
                        "NOTICE:false",
                        "POLL:false",
                        "SCHEDULE_MANAGE:true",
                        "TOURNAMENT_RECORD:false",
                        "BRACKET:false",
                        "FINANCE:false",
                        "FEEDBACK:false",
                        "MEMBER_DIRECTORY:false",
                        "TODO:false",
                        "ROLE_MANAGEMENT:true",
                        "HANDOVER:false",
                        "DECISION_LOG:false"
                );
        assertThat(clubFeatureService.isFeatureEnabled(clubId, "ATTENDANCE")).isTrue();
        assertThat(clubFeatureService.isFeatureEnabled(clubId, "NOTICE")).isFalse();
        assertThat(clubFeatureService.isFeatureEnabled(clubId, "POLL")).isFalse();
        assertThat(clubFeatureService.isFeatureEnabled(clubId, "SCHEDULE_MANAGE")).isTrue();
        assertThat(clubFeatureService.isFeatureEnabled(clubId, "ROLE_MANAGEMENT")).isTrue();
        assertThat(responses)
                .filteredOn(response -> "JOIN_REQUEST".equals(response.featureKey()))
                .singleElement()
                .satisfies(response -> {
                    assertThat(response.navigationScope()).isEqualTo("ADMIN_ONLY");
                    assertThat(response.mandatory()).isTrue();
                    assertThat(response.mandatoryReason()).contains("가입 승인제");
                    assertThat(response.userPath()).isEqualTo("/clubs/" + clubId);
                    assertThat(response.adminPath()).isEqualTo("/clubs/" + clubId + "/admin/more/join-requests");
                });
        assertThat(responses)
                .filteredOn(response -> "ATTENDANCE".equals(response.featureKey()))
                .singleElement()
                .satisfies(response -> {
                    assertThat(response.requiredFeatureKeys()).containsExactly("SCHEDULE_MANAGE");
                    assertThat(response.userPath()).isEqualTo("/clubs/" + clubId + "/schedule");
                    assertThat(response.adminPath()).isEqualTo("/clubs/" + clubId + "/schedule");
                });
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "NOTICE",
            "SCHEDULE_MANAGE",
            "POLL",
            "TOURNAMENT_RECORD",
            "BRACKET",
            "FINANCE",
            "MEMBER_DIRECTORY",
            "FEEDBACK",
            "TODO",
            "ROLE_MANAGEMENT",
            "DECISION_LOG"
    })
    void updateClubFeatures_standaloneFeatureEnablesWithoutAnotherFeature(String featureKey) {
        String userKey = "standalone-" + featureKey.toLowerCase();
        Long clubId = clubService.createClub(
                userKey,
                "Standalone Admin",
                new CreateClubRequest(
                        "Standalone " + featureKey,
                        "단독 기능 계약 테스트",
                        "OTHER",
                        "PUBLIC",
                        "OPEN",
                        null
                )
        ).clubId();

        var responses = clubFeatureService.updateClubFeatures(
                clubId,
                userKey,
                new UpdateClubFeaturesRequest(List.of(featureKey))
        );

        var expectedEnabledKeys = "ROLE_MANAGEMENT".equals(featureKey)
                ? List.of("ROLE_MANAGEMENT")
                : List.of(featureKey, "ROLE_MANAGEMENT");
        assertThat(responses).filteredOn(response -> response.enabled())
                .extracting(response -> response.featureKey())
                .containsExactlyInAnyOrderElementsOf(expectedEnabledKeys);
    }

    @Test
    void updateClubFeatures_preservesRequestedOrderInResponse() {
        Long clubId = clubService.createClub(
                "feature-user-order",
                "Feature Admin",
                new CreateClubRequest(
                        "Ordered Feature Club",
                        "기능 순서 테스트",
                        "OTHER",
                        "PUBLIC",
                        "APPROVAL",
                        null
                )
        ).clubId();

        var responses = clubFeatureService.updateClubFeatures(
                clubId,
                "feature-user-order",
                new UpdateClubFeaturesRequest(java.util.List.of("FINANCE", "TODO", "MEMBER_DIRECTORY"))
        );

        assertThat(responses.stream().filter(response -> response.enabled()
                        && !"JOIN_REQUEST".equals(response.featureKey())
                        && !"ROLE_MANAGEMENT".equals(response.featureKey())))
                .extracting(response -> response.featureKey() + ":" + response.sortOrder())
                .containsExactly("FINANCE:10", "TODO:20", "MEMBER_DIRECTORY:30");
    }

    @Test
    void getClubFeatures_handoverRemainsEnabledBecauseRoleManagementIsCore() {
        Long clubId = clubService.createClub(
                "feature-user-inconsistent",
                "Feature Admin",
                new CreateClubRequest(
                        "Inconsistent Feature Club",
                        "의존성 방어 테스트",
                        "OTHER",
                        "PUBLIC",
                        "OPEN",
                        null
                )
        ).clubId();
        clubFeatureRepository.save(ClubFeature.builder()
                .clubId(clubId)
                .featureKey("HANDOVER")
                .enabled(true)
                .sortOrder(10)
                .build());

        var features = clubFeatureService.getClubFeatures(clubId, "feature-user-inconsistent");

        assertThat(clubFeatureService.isFeatureEnabled(clubId, "HANDOVER")).isTrue();
        assertThat(features)
                .filteredOn(feature -> "HANDOVER".equals(feature.featureKey()))
                .singleElement()
                .returns(true, feature -> feature.enabled());
    }

    @Test
    void updateClubFeatures_removedTimelineKey_throwsValidation() {
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

        assertThatThrownBy(() -> clubFeatureService.updateClubFeatures(
                clubId,
                "feature-user-002",
                new UpdateClubFeaturesRequest(java.util.List.of("TIMELINE"))
        ))
                .isInstanceOf(SemoException.ValidationException.class)
                .hasMessage("지원하지 않는 기능 키가 포함되어 있습니다.");
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

        assertThat(responses).hasSize(14);
        assertThat(responses)
                .extracting(response -> response.featureKey() + ":" + response.enabled())
                .containsExactlyInAnyOrder(
                        "JOIN_REQUEST:true",
                        "ATTENDANCE:false",
                        "NOTICE:true",
                        "POLL:false",
                        "SCHEDULE_MANAGE:false",
                        "TOURNAMENT_RECORD:false",
                        "BRACKET:false",
                        "FINANCE:false",
                        "FEEDBACK:false",
                        "MEMBER_DIRECTORY:false",
                        "TODO:false",
                        "ROLE_MANAGEMENT:true",
                        "HANDOVER:false",
                        "DECISION_LOG:false"
                );
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

        assertThat(responses).hasSize(14);
        assertThat(responses)
                .extracting(response -> response.featureKey() + ":" + response.enabled())
                .containsExactlyInAnyOrder(
                        "JOIN_REQUEST:true",
                        "ATTENDANCE:false",
                        "NOTICE:false",
                        "POLL:true",
                        "SCHEDULE_MANAGE:false",
                        "TOURNAMENT_RECORD:false",
                        "BRACKET:false",
                        "FINANCE:false",
                        "FEEDBACK:false",
                        "MEMBER_DIRECTORY:false",
                        "TODO:false",
                        "ROLE_MANAGEMENT:true",
                        "HANDOVER:false",
                        "DECISION_LOG:false"
                );
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

        assertThat(responses).hasSize(14);
        assertThat(responses)
                .extracting(response -> response.featureKey() + ":" + response.enabled())
                .containsExactlyInAnyOrder(
                        "JOIN_REQUEST:true",
                        "ATTENDANCE:false",
                        "NOTICE:false",
                        "POLL:false",
                        "SCHEDULE_MANAGE:true",
                        "TOURNAMENT_RECORD:false",
                        "BRACKET:false",
                        "FINANCE:false",
                        "FEEDBACK:false",
                        "MEMBER_DIRECTORY:false",
                        "TODO:false",
                        "ROLE_MANAGEMENT:true",
                        "HANDOVER:false",
                        "DECISION_LOG:false"
                );
        assertThat(clubFeatureService.isFeatureEnabled(clubId, "SCHEDULE_MANAGE")).isTrue();
    }

    @Test
    void updateClubFeaturesEnablesMemberDirectoryFeature() {
        Long clubId = clubService.createClub(
                "feature-user-006",
                "Feature Admin",
                new CreateClubRequest(
                        "Member Directory Feature Club",
                        "회원 디렉터리 기능 테스트",
                        "OTHER",
                        "PUBLIC",
                        "APPROVAL",
                        null
                )
        ).clubId();

        var responses = clubFeatureService.updateClubFeatures(
                clubId,
                "feature-user-006",
                new UpdateClubFeaturesRequest(java.util.List.of("MEMBER_DIRECTORY"))
        );

        assertThat(responses).hasSize(14);
        assertThat(responses)
                .extracting(response -> response.featureKey() + ":" + response.enabled())
                .containsExactlyInAnyOrder(
                        "JOIN_REQUEST:true",
                        "ATTENDANCE:false",
                        "NOTICE:false",
                        "POLL:false",
                        "SCHEDULE_MANAGE:false",
                        "TOURNAMENT_RECORD:false",
                        "BRACKET:false",
                        "FINANCE:false",
                        "FEEDBACK:false",
                        "MEMBER_DIRECTORY:true",
                        "TODO:false",
                        "ROLE_MANAGEMENT:true",
                        "HANDOVER:false",
                        "DECISION_LOG:false"
                );
        assertThat(clubFeatureService.isFeatureEnabled(clubId, "MEMBER_DIRECTORY")).isTrue();
    }

    @Test
    void getClubFeatures_newApprovalClubEnablesOnlyPolicyMandatoryJoinRequest() {
        Long clubId = clubService.createClub(
                "feature-user-approval-default",
                "Feature Admin",
                new CreateClubRequest(
                        "Approval Feature Club",
                        "승인제 기본 기능 테스트",
                        "OTHER",
                        "PUBLIC",
                        "APPROVAL",
                        null
                )
        ).clubId();

        var responses = clubFeatureService.getClubFeatures(clubId, "feature-user-approval-default");

        assertThat(responses).filteredOn(response -> response.enabled())
                .extracting(response -> response.featureKey())
                .containsExactlyInAnyOrder("JOIN_REQUEST", "ROLE_MANAGEMENT");
        assertThat(responses).filteredOn(response -> "HANDOVER".equals(response.featureKey()))
                .singleElement()
                .satisfies(response -> assertThat(response.enabled()).isFalse());
    }

    @Test
    void updateClubFeatures_openClubCannotEnableUnusedJoinRequestQueue() {
        Long clubId = clubService.createClub(
                "feature-user-open",
                "Feature Admin",
                new CreateClubRequest(
                        "Open Feature Club",
                        "자유 가입 기능 테스트",
                        "OTHER",
                        "PUBLIC",
                        "OPEN",
                        null
                )
        ).clubId();

        var responses = clubFeatureService.updateClubFeatures(
                clubId,
                "feature-user-open",
                new UpdateClubFeaturesRequest(List.of("JOIN_REQUEST"))
        );

        assertThat(responses).filteredOn(response -> "JOIN_REQUEST".equals(response.featureKey()))
                .singleElement()
                .satisfies(response -> {
                    assertThat(response.enabled()).isFalse();
                    assertThat(response.mandatory()).isFalse();
                    assertThat(response.available()).isFalse();
                    assertThat(response.unavailableReason()).contains("가입 승인제");
                });
    }

    @Test
    void updateClubFeatures_handoverAutomaticallyIncludesRoleManagement() {
        Long clubId = clubService.createClub(
                "feature-user-handover",
                "Feature Admin",
                new CreateClubRequest(
                        "Handover Feature Club",
                        "인수인계 필수 조합 테스트",
                        "OTHER",
                        "PUBLIC",
                        "OPEN",
                        null
                )
        ).clubId();

        var responses = clubFeatureService.updateClubFeatures(
                clubId,
                "feature-user-handover",
                new UpdateClubFeaturesRequest(List.of("HANDOVER"))
        );

        assertThat(responses).filteredOn(response -> response.enabled())
                .extracting(response -> response.featureKey())
                .containsExactlyInAnyOrder("ROLE_MANAGEMENT", "HANDOVER");
        assertThat(responses).filteredOn(response -> "HANDOVER".equals(response.featureKey()))
                .singleElement()
                .satisfies(response -> assertThat(response.requiredFeatureKeys()).containsExactly("ROLE_MANAGEMENT"));
    }
}
