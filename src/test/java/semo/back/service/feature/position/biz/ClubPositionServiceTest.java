package semo.back.service.feature.position.biz;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import semo.back.service.common.exception.SemoException;
import semo.back.service.database.pub.entity.FeaturePermissionCatalog;
import semo.back.service.database.pub.entity.ClubPositionFeatureGrant;
import semo.back.service.database.pub.repository.ClubActivityLogRepository;
import semo.back.service.database.pub.repository.ClubBoardItemRepository;
import semo.back.service.database.pub.repository.ClubEventParticipantRepository;
import semo.back.service.database.pub.repository.ClubFeatureRepository;
import semo.back.service.database.pub.repository.ClubMemberPositionHistoryRepository;
import semo.back.service.database.pub.repository.ClubMemberPositionRepository;
import semo.back.service.database.pub.repository.ClubMemberRepository;
import semo.back.service.database.pub.repository.ClubNoticeRepository;
import semo.back.service.database.pub.repository.ClubPositionPermissionRepository;
import semo.back.service.database.pub.repository.ClubPositionFeatureGrantRepository;
import semo.back.service.database.pub.repository.ClubPositionRepository;
import semo.back.service.database.pub.repository.ClubPositionSensitiveGrantRepository;
import semo.back.service.database.pub.repository.ClubProfileRepository;
import semo.back.service.database.pub.repository.ClubRepository;
import semo.back.service.database.pub.repository.ClubScheduleEventRepository;
import semo.back.service.database.pub.repository.ClubScheduleVoteOptionRepository;
import semo.back.service.database.pub.repository.ClubScheduleVoteRepository;
import semo.back.service.database.pub.repository.ClubScheduleVoteSelectionRepository;
import semo.back.service.database.pub.repository.FeatureCatalogRepository;
import semo.back.service.database.pub.repository.FeaturePermissionCatalogRepository;
import semo.back.service.database.pub.repository.ProfileUserRepository;
import semo.back.service.feature.club.biz.ClubService;
import semo.back.service.feature.club.biz.policy.ClubAccessResolver;
import semo.back.service.feature.club.vo.CreateClubRequest;
import semo.back.service.feature.clubfeature.biz.ClubFeatureService;
import semo.back.service.feature.clubfeature.vo.UpdateClubFeaturesRequest;
import semo.back.service.feature.activity.biz.ClubActivityRecorder;
import semo.back.service.feature.activity.biz.ClubActivityService;
import semo.back.service.feature.position.vo.CreateClubPositionRequest;
import semo.back.service.feature.position.vo.ClubPositionFeatureGrantRequest;
import semo.back.service.feature.position.vo.UpdateClubPositionRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static semo.back.service.support.TestCatalogSeeder.seedFeatureCatalogs;

@SpringBootTest
@ActiveProfiles("test")
class ClubPositionServiceTest {

    @Autowired
    private ClubService clubService;

    @Autowired
    private ClubAccessResolver clubAccessResolver;

    @Autowired
    private ClubFeatureService clubFeatureService;

    @Autowired
    private ClubActivityRecorder clubActivityRecorder;

    @Autowired
    private ClubActivityService clubActivityService;

    @Autowired
    private ClubPositionService clubPositionService;

    @Autowired
    private ClubPositionPermissionEvaluator clubPositionPermissionEvaluator;

    @Autowired
    private ClubActivityLogRepository clubActivityLogRepository;

    @Autowired
    private ClubBoardItemRepository clubBoardItemRepository;

    @Autowired
    private ClubEventParticipantRepository clubEventParticipantRepository;

    @Autowired
    private ClubFeatureRepository clubFeatureRepository;

    @Autowired
    private ClubMemberPositionRepository clubMemberPositionRepository;

    @Autowired
    private ClubMemberPositionHistoryRepository clubMemberPositionHistoryRepository;

    @Autowired
    private ClubMemberRepository clubMemberRepository;

    @Autowired
    private ClubNoticeRepository clubNoticeRepository;

    @Autowired
    private ClubPositionPermissionRepository clubPositionPermissionRepository;

    @Autowired
    private ClubPositionFeatureGrantRepository clubPositionFeatureGrantRepository;

    @Autowired
    private ClubPositionSensitiveGrantRepository clubPositionSensitiveGrantRepository;

    @Autowired
    private ClubPositionRepository clubPositionRepository;

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
    private FeatureCatalogRepository featureCatalogRepository;

    @Autowired
    private FeaturePermissionCatalogRepository featurePermissionCatalogRepository;

    @Autowired
    private ProfileUserRepository profileUserRepository;

    @BeforeEach
    void setUp() {
        clubActivityLogRepository.deleteAll();
        clubScheduleVoteSelectionRepository.deleteAll();
        clubScheduleVoteOptionRepository.deleteAll();
        clubScheduleVoteRepository.deleteAll();
        clubEventParticipantRepository.deleteAll();
        clubScheduleEventRepository.deleteAll();
        clubBoardItemRepository.deleteAll();
        clubNoticeRepository.deleteAll();
        clubMemberPositionHistoryRepository.deleteAll();
        clubMemberPositionRepository.deleteAll();
        clubPositionPermissionRepository.deleteAll();
        clubPositionSensitiveGrantRepository.deleteAll();
        clubPositionFeatureGrantRepository.deleteAll();
        clubPositionRepository.deleteAll();
        clubFeatureRepository.deleteAll();
        clubProfileRepository.deleteAll();
        clubMemberRepository.deleteAll();
        clubRepository.deleteAll();
        featurePermissionCatalogRepository.deleteAll();
        featureCatalogRepository.deleteAll();
        profileUserRepository.deleteAll();

        seedFeatureCatalogs(featureCatalogRepository);
        seedPermissionCatalogs();
    }

    @Test
    void getRoleManagementShowsOnlyEnabledFeatureGroups() {
        Long clubId = createClub("role-owner-001", "직책 테스트 클럽");
        clubFeatureService.updateClubFeatures(
                clubId,
                "role-owner-001",
                new UpdateClubFeaturesRequest(List.of("NOTICE", "ROLE_MANAGEMENT"))
        );

        clubPositionService.createPosition(
                clubId,
                "role-owner-001",
                new CreateClubPositionRequest(
                        "공지 담당",
                        "NOTICE_EDITOR",
                        null,
                        "campaign",
                        "#c76117",
                        noticeOperatorGrants()
                )
        );

        var response = clubPositionService.getRoleManagement(clubId, "role-owner-001");

        assertThat(response.permissionGroups())
                .extracting(item -> item.featureKey() + ":" + item.displayName())
                .containsExactly("NOTICE:게시판 공지");
        assertThat(response.permissionGroups().getFirst().accessLevels())
                .extracting(item -> item.accessLevel())
                .containsExactly("NONE", "OPERATOR");
        assertThat(response.permissionGroups().getFirst().accessLevels().getLast().permissionKeys())
                .containsExactlyInAnyOrderElementsOf(noticeOperatorPermissions());
        assertThat(response.positions()).singleElement().satisfies(position -> {
            assertThat(position.permissionKeys()).containsExactlyInAnyOrderElementsOf(noticeOperatorPermissions());
            assertThat(position.permissionCount()).isEqualTo(3);
            assertThat(position.featureGrants()).singleElement().satisfies(grant -> {
                assertThat(grant.featureKey()).isEqualTo("NOTICE");
                assertThat(grant.accessLevel()).isEqualTo("OPERATOR");
                assertThat(grant.policyVersion()).isEqualTo(1);
                assertThat(grant.status()).isEqualTo(ClubPositionGrantService.STATUS_CURRENT);
            });
        });
        assertThat(clubPositionFeatureGrantRepository.findAll()).singleElement().satisfies(grant -> {
            assertThat(grant.getFeatureKey()).isEqualTo("NOTICE");
            assertThat(grant.getAccessLevel()).isEqualTo("OPERATOR");
        });
        assertThat(response.positionTemplates())
                .filteredOn(template -> template.templateKey().equals("CONTENT_COORDINATOR"))
                .singleElement()
                .satisfies(template -> {
                    assertThat(template.featureCount()).isEqualTo(1);
                    assertThat(template.permissionKeys()).containsExactlyInAnyOrderElementsOf(noticeOperatorPermissions());
                });
    }

    @Test
    void createPositionRejectsDisabledFeaturePermission() {
        Long clubId = createClub("role-owner-002", "직책 생성 검증 클럽");
        clubFeatureService.updateClubFeatures(
                clubId,
                "role-owner-002",
                new UpdateClubFeaturesRequest(List.of("NOTICE", "ROLE_MANAGEMENT"))
        );

        assertThatThrownBy(() -> clubPositionService.createPosition(
                clubId,
                "role-owner-002",
                new CreateClubPositionRequest(
                        "잘못된 직책",
                        "INVALID_ROLE",
                        null,
                        "shield",
                        "#0053dd",
                        List.of(grant("POLL", "OPERATOR"))
                )
        )).isInstanceOf(SemoException.ValidationException.class)
                .hasMessageContaining("활성화되지 않은 기능");
    }

    @Test
    void createPositionRejectsNonStandardPermissionCombination() {
        Long clubId = createClub("role-owner-standard", "직책 표준 수준 검증 클럽");
        clubFeatureService.updateClubFeatures(
                clubId,
                "role-owner-standard",
                new UpdateClubFeaturesRequest(List.of("NOTICE", "ROLE_MANAGEMENT"))
        );

        assertThatThrownBy(() -> clubPositionService.createPosition(
                clubId,
                "role-owner-standard",
                new CreateClubPositionRequest(
                        "불완전한 공지 담당",
                        "PARTIAL_NOTICE",
                        null,
                        "campaign",
                        "#0053dd",
                        List.of(grant("NOTICE", "PARTIAL"))
                )
        )).isInstanceOf(SemoException.ValidationException.class)
                .hasMessageContaining("지원하지 않는 운영 수준");
    }

    @Test
    void createPositionRejectsInvalidColorValue() {
        Long clubId = createClub("role-owner-color", "직책 색상 검증 클럽");
        clubFeatureService.updateClubFeatures(
                clubId,
                "role-owner-color",
                new UpdateClubFeaturesRequest(List.of("ROLE_MANAGEMENT"))
        );

        assertThatThrownBy(() -> clubPositionService.createPosition(
                clubId,
                "role-owner-color",
                new CreateClubPositionRequest(
                        "색상 오류",
                        "INVALID_COLOR",
                        null,
                        "shield",
                        "orange",
                        List.of()
                )
        )).isInstanceOf(SemoException.ValidationException.class)
                .hasMessageContaining("HEX 형식");
    }

    @Test
    void updatePositionPreservesHiddenPermissionsFromDisabledFeature() {
        Long clubId = createClub("role-owner-003", "직책 수정 검증 클럽");
        clubFeatureService.updateClubFeatures(
                clubId,
                "role-owner-003",
                new UpdateClubFeaturesRequest(List.of("NOTICE", "POLL", "ROLE_MANAGEMENT"))
        );

        var created = clubPositionService.createPosition(
                clubId,
                "role-owner-003",
                new CreateClubPositionRequest(
                        "운영 직책",
                        "OPS_EDITOR",
                        null,
                        "shield",
                        "#0053dd",
                        List.of(
                                grant("NOTICE", "OPERATOR"),
                                grant("POLL", "OPERATOR")
                        )
                )
        );

        clubFeatureService.updateClubFeatures(
                clubId,
                "role-owner-003",
                new UpdateClubFeaturesRequest(List.of("NOTICE", "ROLE_MANAGEMENT"))
        );

        var hiddenBeforeUpdate = clubPositionService.getPositionDetail(
                clubId,
                created.position().clubPositionId(),
                "role-owner-003"
        );
        assertThat(hiddenBeforeUpdate.permissionGroups())
                .extracting(item -> item.featureKey())
                .containsExactly("NOTICE");
        assertThat(hiddenBeforeUpdate.position().permissionKeys()).containsExactlyInAnyOrder(
                "NOTICE_CREATE",
                "NOTICE_UPDATE_SELF",
                "NOTICE_DELETE_SELF",
                "POLL_CREATE",
                "POLL_UPDATE_SELF",
                "POLL_DELETE_SELF"
        );

        var updated = clubPositionService.updatePosition(
                clubId,
                created.position().clubPositionId(),
                "role-owner-003",
                new UpdateClubPositionRequest(
                        "운영 직책",
                        "OPS_EDITOR",
                        "공지 운영 담당",
                        "shield",
                        "#0053dd",
                        created.position().version(),
                        true,
                        noticeOperatorGrants()
                )
        );

        assertThat(updated.position().permissionKeys())
                .containsExactlyInAnyOrder(
                        "NOTICE_CREATE",
                        "NOTICE_UPDATE_SELF",
                        "NOTICE_DELETE_SELF",
                        "POLL_CREATE",
                        "POLL_UPDATE_SELF",
                        "POLL_DELETE_SELF"
                );
        assertThat(updated.position().version()).isEqualTo(created.position().version() + 1);
        assertThat(updated.position().permissionCount()).isEqualTo(6);
        assertThat(clubPositionPermissionRepository.findByClubPositionId(created.position().clubPositionId()))
                .extracting(item -> item.getPermissionKey())
                .containsExactlyInAnyOrder(
                        "NOTICE_CREATE",
                        "NOTICE_UPDATE_SELF",
                        "NOTICE_DELETE_SELF",
                        "POLL_CREATE",
                        "POLL_UPDATE_SELF",
                        "POLL_DELETE_SELF"
                );
    }

    @Test
    void updatePosition_rejectsChangingStablePositionCode() {
        Long clubId = createClub("role-owner-stable-code", "직책 코드 불변 테스트 클럽");
        clubFeatureService.updateClubFeatures(
                clubId,
                "role-owner-stable-code",
                new UpdateClubFeaturesRequest(List.of("ROLE_MANAGEMENT"))
        );
        var created = clubPositionService.createPosition(
                clubId,
                "role-owner-stable-code",
                new CreateClubPositionRequest("총무", "TREASURER", null, "payments", "#0053dd", List.of())
        );

        assertThatThrownBy(() -> clubPositionService.updatePosition(
                clubId,
                created.position().clubPositionId(),
                "role-owner-stable-code",
                new UpdateClubPositionRequest(
                        "재정 담당",
                        "FINANCE_MANAGER",
                        null,
                        "payments",
                        "#0053dd",
                        created.position().version(),
                        true,
                        List.of()
                )
        )).isInstanceOf(SemoException.ValidationException.class)
                .hasMessageContaining("생성 후 변경할 수 없습니다");
    }

    @Test
    void updatePosition_staleVersion_throwsConflictWithoutChangingGrants() {
        Long clubId = createClub("role-owner-stale-version", "직책 동시 수정 테스트 클럽");
        clubFeatureService.updateClubFeatures(
                clubId,
                "role-owner-stale-version",
                new UpdateClubFeaturesRequest(List.of("NOTICE"))
        );
        var created = clubPositionService.createPosition(
                clubId,
                "role-owner-stale-version",
                new CreateClubPositionRequest(
                        "공지 담당",
                        "NOTICE_OPERATOR",
                        null,
                        "campaign",
                        "#0053dd",
                        noticeOperatorGrants()
                )
        );

        assertThatThrownBy(() -> clubPositionService.updatePosition(
                clubId,
                created.position().clubPositionId(),
                "role-owner-stale-version",
                new UpdateClubPositionRequest(
                        "오래된 수정",
                        "NOTICE_OPERATOR",
                        null,
                        "campaign",
                        "#0053dd",
                        created.position().version() + 1,
                        true,
                        List.of()
                )
        )).isInstanceOf(SemoException.ConflictException.class)
                .hasMessageContaining("최신 내용을 다시 불러온 뒤");

        assertThat(clubPositionPermissionRepository.findByClubPositionId(created.position().clubPositionId()))
                .extracting(item -> item.getPermissionKey())
                .containsExactlyInAnyOrderElementsOf(noticeOperatorPermissions());
    }

    @Test
    void updatePosition_previousPolicyVersion_preservesExistingProjectionUntilExplicitUpgrade() {
        Long clubId = createClub("role-owner-policy-version", "직책 정책 버전 테스트 클럽");
        clubFeatureService.updateClubFeatures(
                clubId,
                "role-owner-policy-version",
                new UpdateClubFeaturesRequest(List.of("NOTICE"))
        );
        var created = clubPositionService.createPosition(
                clubId,
                "role-owner-policy-version",
                new CreateClubPositionRequest(
                        "공지 담당",
                        "NOTICE_OPERATOR",
                        null,
                        "campaign",
                        "#0053dd",
                        noticeOperatorGrants()
                )
        );
        Long positionId = created.position().clubPositionId();
        clubPositionFeatureGrantRepository.deleteAll();
        clubPositionFeatureGrantRepository.saveAndFlush(ClubPositionFeatureGrant.builder()
                .clubPositionId(positionId)
                .featureKey("NOTICE")
                .accessLevel("OPERATOR")
                .policyVersion(0)
                .build());

        var updated = clubPositionService.updatePosition(
                clubId,
                positionId,
                "role-owner-policy-version",
                new UpdateClubPositionRequest(
                        "공지 담당",
                        "NOTICE_OPERATOR",
                        "이전 정책을 유지합니다.",
                        "campaign",
                        "#0053dd",
                        created.position().version(),
                        true,
                        List.of(new ClubPositionFeatureGrantRequest("NOTICE", "OPERATOR", 0, List.of()))
                )
        );

        assertThat(updated.position().featureGrants()).singleElement().satisfies(grant -> {
            assertThat(grant.policyVersion()).isZero();
            assertThat(grant.currentPolicyVersion()).isEqualTo(1);
            assertThat(grant.status()).isEqualTo(ClubPositionGrantService.STATUS_POLICY_UPDATE_AVAILABLE);
        });
        assertThat(clubPositionPermissionRepository.findByClubPositionId(positionId))
                .extracting(item -> item.getPermissionKey())
                .containsExactlyInAnyOrderElementsOf(noticeOperatorPermissions());
    }

    @Test
    void replaceMemberPositionsIgnoresAlreadyAssignedPositions() {
        Long clubId = createClub("role-owner-004", "직책 재할당 검증 클럽");
        clubFeatureService.updateClubFeatures(
                clubId,
                "role-owner-004",
                new UpdateClubFeaturesRequest(List.of("NOTICE", "ROLE_MANAGEMENT"))
        );

        var leader = clubPositionService.createPosition(
                clubId,
                "role-owner-004",
                new CreateClubPositionRequest(
                        "리더",
                        "LEADER",
                        null,
                        "shield",
                        "#0053dd",
                        noticeOperatorGrants()
                )
        );
        var manager = clubPositionService.createPosition(
                clubId,
                "role-owner-004",
                new CreateClubPositionRequest(
                        "매니저",
                        "MANAGER",
                        null,
                        "badge",
                        "#c76117",
                        List.of()
                )
        );

        var access = clubAccessResolver.requireAdmin(clubId, "role-owner-004");
        var ownerMember = clubMemberRepository.findByClubIdOrderByClubMemberIdAsc(clubId).getFirst();

        clubPositionService.replaceMemberPositions(
                access,
                ownerMember,
                List.of(leader.position().clubPositionId(), manager.position().clubPositionId())
        );

        assertThatCode(() -> clubPositionService.replaceMemberPositions(
                access,
                ownerMember,
                List.of(manager.position().clubPositionId(), leader.position().clubPositionId(), leader.position().clubPositionId())
        )).doesNotThrowAnyException();

        assertThat(clubMemberPositionRepository.findByClubMemberId(ownerMember.getClubMemberId()))
                .extracting(item -> item.getClubPositionId())
                .containsExactlyInAnyOrder(leader.position().clubPositionId(), manager.position().clubPositionId());
        assertThat(clubPositionService.getRoleManagement(clubId, "role-owner-004").assignedMemberCount())
                .isEqualTo(1);
    }

    @Test
    void replaceMemberPositions_recordsPositionTenureHistory() {
        Long clubId = createClub("role-owner-005", "직책 이력 검증 클럽");
        clubFeatureService.updateClubFeatures(
                clubId,
                "role-owner-005",
                new UpdateClubFeaturesRequest(List.of("NOTICE", "ROLE_MANAGEMENT"))
        );
        var leader = clubPositionService.createPosition(
                clubId,
                "role-owner-005",
                new CreateClubPositionRequest(
                        "리더",
                        "LEADER",
                        null,
                        "shield",
                        "#0053dd",
                        noticeOperatorGrants()
                )
        );
        var access = clubAccessResolver.requireAdmin(clubId, "role-owner-005");
        var ownerMember = clubMemberRepository.findByClubIdOrderByClubMemberIdAsc(clubId).getFirst();

        clubPositionService.replaceMemberPositions(
                access,
                ownerMember,
                List.of(leader.position().clubPositionId())
        );

        var openHistory = clubMemberPositionHistoryRepository.findOpenHistories(
                ownerMember.getClubMemberId(),
                leader.position().clubPositionId()
        );
        assertThat(openHistory).singleElement().satisfies(history -> {
            assertThat(history.getClubId()).isEqualTo(clubId);
            assertThat(history.getPositionCodeSnapshot()).isEqualTo("LEADER");
            assertThat(history.getPositionDisplayNameSnapshot()).isEqualTo("리더");
            assertThat(history.getStartedAt()).isNotNull();
            assertThat(history.getEndedAt()).isNull();
        });

        clubPositionService.replaceMemberPositions(access, ownerMember, List.of());

        assertThat(clubMemberPositionHistoryRepository.findOpenHistories(
                ownerMember.getClubMemberId(),
                leader.position().clubPositionId()
        )).isEmpty();
        assertThat(clubMemberPositionHistoryRepository.findByClubIdAndDeletedFalseOrderByStartedAtDescClubMemberPositionHistoryIdDesc(clubId))
                .singleElement()
                .satisfies(history -> assertThat(history.getEndedAt()).isNotNull());
    }

    @Test
    void getRoleManagement_memberWithLegacyGovernancePermissionIsRejected() {
        Long clubId = createClub("role-owner-view", "직책 위임 테스트 클럽");
        clubFeatureService.updateClubFeatures(
                clubId,
                "role-owner-view",
                new UpdateClubFeaturesRequest(List.of("ROLE_MANAGEMENT"))
        );
        var viewerPosition = clubPositionService.createPosition(
                clubId,
                "role-owner-view",
                new CreateClubPositionRequest(
                        "직책 조회 담당",
                        "ROLE_VIEWER",
                        null,
                        "visibility",
                        "#0053dd",
                        List.of()
                )
        );
        clubPositionPermissionRepository.save(semo.back.service.database.pub.entity.ClubPositionPermission.builder()
                .clubPositionId(viewerPosition.position().clubPositionId())
                .permissionKey("ROLE_MANAGEMENT_VIEW")
                .build());
        featurePermissionCatalogRepository.save(FeaturePermissionCatalog.builder()
                .permissionKey("ROLE_MANAGEMENT_FUTURE")
                .featureKey("ROLE_MANAGEMENT")
                .displayName("미래 거버넌스 권한")
                .description("향후 추가되는 거버넌스 권한도 위임되지 않아야 합니다.")
                .ownershipScope("CLUB")
                .active(true)
                .sortOrder(999)
                .build());
        clubPositionPermissionRepository.save(semo.back.service.database.pub.entity.ClubPositionPermission.builder()
                .clubPositionId(viewerPosition.position().clubPositionId())
                .permissionKey("ROLE_MANAGEMENT_FUTURE")
                .build());
        var profileUser = profileUserRepository.save(semo.back.service.database.pub.entity.ProfileUser.builder()
                .userKey("delegated-role-viewer")
                .displayName("위임 조회자")
                .build());
        var member = clubMemberRepository.save(semo.back.service.database.pub.entity.ClubMember.builder()
                .clubId(clubId)
                .profileId(profileUser.getProfileId())
                .roleCode("MEMBER")
                .membershipStatus("ACTIVE")
                .joinedAt(java.time.LocalDateTime.now())
                .build());
        clubProfileRepository.save(semo.back.service.database.pub.entity.ClubProfile.builder()
                .clubMemberId(member.getClubMemberId())
                .displayName("위임 조회자")
                .build());
        var ownerAccess = clubAccessResolver.requireAdmin(clubId, "role-owner-view");
        clubPositionService.replaceMemberPositions(
                ownerAccess,
                member,
                List.of(viewerPosition.position().clubPositionId())
        );

        assertThat(clubPositionPermissionEvaluator.getPermissionKeysForMember(clubId, member.getClubMemberId()))
                .doesNotContain("ROLE_MANAGEMENT_VIEW", "ROLE_MANAGEMENT_FUTURE");
        assertThatThrownBy(() -> clubPositionService.getRoleManagement(clubId, "delegated-role-viewer"))
                .isInstanceOf(SemoException.ForbiddenException.class);
    }

    @Test
    void deletePosition_retiresAssignmentsAndPreservesHistory() {
        Long clubId = createClub("role-owner-retire", "직책 사용 종료 테스트 클럽");
        clubFeatureService.updateClubFeatures(
                clubId,
                "role-owner-retire",
                new UpdateClubFeaturesRequest(List.of("NOTICE", "ROLE_MANAGEMENT"))
        );
        var position = clubPositionService.createPosition(
                clubId,
                "role-owner-retire",
                new CreateClubPositionRequest(
                        "운영 담당",
                        "OPERATIONS",
                        null,
                        "shield",
                        "#0053dd",
                        noticeOperatorGrants()
                )
        );
        var access = clubAccessResolver.requireAdmin(clubId, "role-owner-retire");
        var ownerMember = clubMemberRepository.findByClubIdOrderByClubMemberIdAsc(clubId).getFirst();
        Long positionId = position.position().clubPositionId();
        clubPositionService.replaceMemberPositions(access, ownerMember, List.of(positionId));

        clubPositionService.deletePosition(clubId, positionId, position.position().version(), "role-owner-retire");

        assertThat(clubPositionRepository.findById(positionId)).hasValueSatisfying(retired ->
                assertThat(retired.isActive()).isFalse()
        );
        assertThat(clubMemberPositionRepository.findByClubMemberId(ownerMember.getClubMemberId())).isEmpty();
        assertThat(clubMemberPositionHistoryRepository.findOpenHistories(ownerMember.getClubMemberId(), positionId)).isEmpty();
        assertThat(clubPositionPermissionRepository.findByClubPositionId(positionId))
                .extracting(item -> item.getPermissionKey())
                .containsExactlyInAnyOrderElementsOf(noticeOperatorPermissions());
    }

    @Test
    void deletePosition_staleVersion_throwsConflictWithoutDeactivatingPosition() {
        Long clubId = createClub("role-owner-stale-delete", "직책 종료 충돌 테스트 클럽");
        var position = clubPositionService.createPosition(
                clubId,
                "role-owner-stale-delete",
                new CreateClubPositionRequest(
                        "운영 담당",
                        "OPERATIONS",
                        null,
                        "shield",
                        "#0053dd",
                        List.of()
                )
        );
        Long positionId = position.position().clubPositionId();

        assertThatThrownBy(() -> clubPositionService.deletePosition(
                clubId,
                positionId,
                position.position().version() + 1,
                "role-owner-stale-delete"
        )).isInstanceOf(SemoException.ConflictException.class)
                .hasMessageContaining("최신 내용을 다시 불러온 뒤");
        assertThat(clubPositionRepository.findById(positionId)).hasValueSatisfying(current ->
                assertThat(current.isActive()).isTrue()
        );
    }

    @Test
    void updatePosition_deactivationRetiresAssignmentsAndPreservesHistory() {
        Long clubId = createClub("role-owner-deactivate", "직책 비활성화 테스트 클럽");
        clubFeatureService.updateClubFeatures(
                clubId,
                "role-owner-deactivate",
                new UpdateClubFeaturesRequest(List.of("NOTICE", "ROLE_MANAGEMENT"))
        );
        var position = clubPositionService.createPosition(
                clubId,
                "role-owner-deactivate",
                new CreateClubPositionRequest(
                        "공지 담당",
                        "NOTICE_OPERATOR",
                        "공지를 운영합니다.",
                        "campaign",
                        "#0053dd",
                        noticeOperatorGrants()
                )
        );
        var access = clubAccessResolver.requireAdmin(clubId, "role-owner-deactivate");
        var ownerMember = clubMemberRepository.findByClubIdOrderByClubMemberIdAsc(clubId).getFirst();
        Long positionId = position.position().clubPositionId();
        clubPositionService.replaceMemberPositions(access, ownerMember, List.of(positionId));

        var updated = clubPositionService.updatePosition(
                clubId,
                positionId,
                "role-owner-deactivate",
                new UpdateClubPositionRequest(
                        "공지 담당",
                        "NOTICE_OPERATOR",
                        "공지를 운영합니다.",
                        "campaign",
                        "#0053dd",
                        position.position().version(),
                        false,
                        noticeOperatorGrants()
                )
        );

        assertThat(updated.position().active()).isFalse();
        assertThat(clubMemberPositionRepository.findByClubMemberId(ownerMember.getClubMemberId())).isEmpty();
        assertThat(clubMemberPositionHistoryRepository.findOpenHistories(ownerMember.getClubMemberId(), positionId)).isEmpty();
        assertThat(clubPositionPermissionRepository.findByClubPositionId(positionId))
                .extracting(item -> item.getPermissionKey())
                .containsExactlyInAnyOrderElementsOf(noticeOperatorPermissions());
    }

    @Test
    void replaceMemberPositions_rejectsRetiredPosition() {
        Long clubId = createClub("role-owner-inactive", "종료 직책 배정 테스트 클럽");
        clubFeatureService.updateClubFeatures(
                clubId,
                "role-owner-inactive",
                new UpdateClubFeaturesRequest(List.of("NOTICE", "ROLE_MANAGEMENT"))
        );
        var position = clubPositionService.createPosition(
                clubId,
                "role-owner-inactive",
                new CreateClubPositionRequest("종료 직책", "RETIRED", null, "shield", "#0053dd", List.of())
        );
        Long positionId = position.position().clubPositionId();
        clubPositionService.deletePosition(clubId, positionId, position.position().version(), "role-owner-inactive");
        var access = clubAccessResolver.requireAdmin(clubId, "role-owner-inactive");
        var ownerMember = clubMemberRepository.findByClubIdOrderByClubMemberIdAsc(clubId).getFirst();

        assertThatThrownBy(() -> clubPositionService.replaceMemberPositions(access, ownerMember, List.of(positionId)))
                .isInstanceOf(SemoException.ValidationException.class)
                .hasMessageContaining("사용 종료된 직책");
    }

    @Test
    void replaceMemberPositions_rejectsMemberFromAnotherClub() {
        Long actorClubId = createClub("role-owner-cross-actor", "직책 배정 주체 클럽");
        Long targetClubId = createClub("role-owner-cross-target", "직책 배정 대상 클럽");
        clubFeatureService.updateClubFeatures(
                actorClubId,
                "role-owner-cross-actor",
                new UpdateClubFeaturesRequest(List.of("ROLE_MANAGEMENT"))
        );
        var actorAccess = clubAccessResolver.requireAdmin(actorClubId, "role-owner-cross-actor");
        var foreignTarget = clubMemberRepository.findByClubIdOrderByClubMemberIdAsc(targetClubId).getFirst();

        assertThatThrownBy(() -> clubPositionService.replaceMemberPositions(actorAccess, foreignTarget, List.of()))
                .isInstanceOf(SemoException.ValidationException.class)
                .hasMessageContaining("다른 모임의 멤버");
    }

    @Test
    void replaceMemberPositions_rejectsDormantMember() {
        Long clubId = createClub("role-owner-dormant", "휴면 멤버 직책 테스트 클럽");
        clubFeatureService.updateClubFeatures(
                clubId,
                "role-owner-dormant",
                new UpdateClubFeaturesRequest(List.of("ROLE_MANAGEMENT"))
        );
        var actorAccess = clubAccessResolver.requireAdmin(clubId, "role-owner-dormant");
        var target = clubMemberRepository.findByClubIdOrderByClubMemberIdAsc(clubId).getFirst();
        target.updateMembershipStatus("DORMANT");
        clubMemberRepository.saveAndFlush(target);

        assertThatThrownBy(() -> clubPositionService.replaceMemberPositions(actorAccess, target, List.of()))
                .isInstanceOf(SemoException.ValidationException.class)
                .hasMessageContaining("활동 중인 멤버");
    }

    @Test
    void getRecentAdminActivities_positionFilterUsesTenureAtActivityTime() {
        Long clubId = createClub("role-owner-006", "직책 로그 필터 클럽");
        clubFeatureService.updateClubFeatures(
                clubId,
                "role-owner-006",
                new UpdateClubFeaturesRequest(List.of("NOTICE", "ROLE_MANAGEMENT"))
        );
        var leader = clubPositionService.createPosition(
                clubId,
                "role-owner-006",
                new CreateClubPositionRequest(
                        "리더",
                        "LEADER",
                        null,
                        "shield",
                        "#0053dd",
                        noticeOperatorGrants()
                )
        );
        var access = clubAccessResolver.requireAdmin(clubId, "role-owner-006");
        var ownerMember = clubMemberRepository.findByClubIdOrderByClubMemberIdAsc(clubId).getFirst();
        clubPositionService.replaceMemberPositions(
                access,
                ownerMember,
                List.of(leader.position().clubPositionId())
        );

        clubActivityRecorder.recordSuccessSafely(clubId, "role-owner-006", "공지관리", "공지 테스트 로그를 남겼습니다.");

        var response = clubActivityService.getRecentAdminActivities(
                clubId,
                "role-owner-006",
                null,
                null,
                20,
                leader.position().clubPositionId()
        );

        assertThat(response.selectedPositionId()).isEqualTo(leader.position().clubPositionId());
        assertThat(response.positionFilters())
                .extracting(item -> item.displayName())
                .contains("리더");
        assertThat(response.activities()).singleElement().satisfies(activity -> {
            assertThat(activity.detail()).isEqualTo("공지 테스트 로그를 남겼습니다.");
            assertThat(activity.actorPositions())
                    .extracting(item -> item.displayName())
                    .containsExactly("리더");
        });
    }

    private Long createClub(String userKey, String clubName) {
        return clubService.createClub(
                userKey,
                "Role Owner",
                new CreateClubRequest(
                        clubName,
                        "직책 테스트",
                        "OTHER",
                        "PUBLIC",
                        "APPROVAL",
                        null
                )
        ).clubId();
    }

    private void seedPermissionCatalogs() {
        featurePermissionCatalogRepository.saveAll(List.of(
                FeaturePermissionCatalog.builder()
                        .permissionKey("NOTICE_CREATE")
                        .featureKey("NOTICE")
                        .displayName("공지 작성")
                        .description("공지 작성 권한")
                        .ownershipScope("CLUB")
                        .active(true)
                        .sortOrder(10)
                        .build(),
                FeaturePermissionCatalog.builder()
                        .permissionKey("NOTICE_UPDATE_SELF")
                        .featureKey("NOTICE")
                        .displayName("공지 수정")
                        .description("본인 공지 수정 권한")
                        .ownershipScope("SELF")
                        .active(true)
                        .sortOrder(20)
                        .build(),
                FeaturePermissionCatalog.builder()
                        .permissionKey("NOTICE_DELETE_SELF")
                        .featureKey("NOTICE")
                        .displayName("공지 삭제")
                        .description("본인 공지 삭제 권한")
                        .ownershipScope("SELF")
                        .active(true)
                        .sortOrder(30)
                        .build(),
                FeaturePermissionCatalog.builder()
                        .permissionKey("POLL_CREATE")
                        .featureKey("POLL")
                        .displayName("투표 작성")
                        .description("투표 작성 권한")
                        .ownershipScope("CLUB")
                        .active(true)
                        .sortOrder(10)
                        .build(),
                FeaturePermissionCatalog.builder()
                        .permissionKey("POLL_UPDATE_SELF")
                        .featureKey("POLL")
                        .displayName("투표 수정")
                        .description("본인 투표 수정 권한")
                        .ownershipScope("SELF")
                        .active(true)
                        .sortOrder(20)
                        .build(),
                FeaturePermissionCatalog.builder()
                        .permissionKey("POLL_DELETE_SELF")
                        .featureKey("POLL")
                        .displayName("투표 삭제")
                        .description("본인 투표 삭제 권한")
                        .ownershipScope("SELF")
                        .active(true)
                        .sortOrder(30)
                        .build(),
                FeaturePermissionCatalog.builder()
                        .permissionKey("ROLE_MANAGEMENT_VIEW")
                        .featureKey("ROLE_MANAGEMENT")
                        .displayName("직책 조회")
                        .description("직책 조회 권한")
                        .ownershipScope("CLUB")
                        .active(true)
                        .sortOrder(10)
                        .build()
        ));
    }

    private List<String> noticeOperatorPermissions() {
        return List.of("NOTICE_CREATE", "NOTICE_UPDATE_SELF", "NOTICE_DELETE_SELF");
    }

    private List<ClubPositionFeatureGrantRequest> noticeOperatorGrants() {
        return List.of(grant("NOTICE", "OPERATOR"));
    }

    private ClubPositionFeatureGrantRequest grant(String featureKey, String accessLevel) {
        return new ClubPositionFeatureGrantRequest(featureKey, accessLevel, null, List.of());
    }
}
