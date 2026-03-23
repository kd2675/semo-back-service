package semo.back.service.feature.position.biz;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import semo.back.service.common.exception.SemoException;
import semo.back.service.database.pub.entity.FeaturePermissionCatalog;
import semo.back.service.database.pub.repository.ClubAttendanceCheckInRepository;
import semo.back.service.database.pub.repository.ClubAttendanceSessionRepository;
import semo.back.service.database.pub.repository.ClubBoardItemRepository;
import semo.back.service.database.pub.repository.ClubEventParticipantRepository;
import semo.back.service.database.pub.repository.ClubFeatureRepository;
import semo.back.service.database.pub.repository.ClubMemberPositionRepository;
import semo.back.service.database.pub.repository.ClubMemberRepository;
import semo.back.service.database.pub.repository.ClubNoticeRepository;
import semo.back.service.database.pub.repository.ClubPositionPermissionRepository;
import semo.back.service.database.pub.repository.ClubPositionRepository;
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
import semo.back.service.feature.club.vo.CreateClubRequest;
import semo.back.service.feature.clubfeature.biz.ClubFeatureService;
import semo.back.service.feature.clubfeature.vo.UpdateClubFeaturesRequest;
import semo.back.service.feature.position.vo.CreateClubPositionRequest;
import semo.back.service.feature.position.vo.UpdateClubPositionRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static semo.back.service.support.TestCatalogSeeder.seedFeatureCatalogs;

@SpringBootTest
@ActiveProfiles("test")
class ClubPositionServiceTest {

    @Autowired
    private ClubService clubService;

    @Autowired
    private ClubFeatureService clubFeatureService;

    @Autowired
    private ClubPositionService clubPositionService;

    @Autowired
    private ClubAttendanceCheckInRepository clubAttendanceCheckInRepository;

    @Autowired
    private ClubAttendanceSessionRepository clubAttendanceSessionRepository;

    @Autowired
    private ClubBoardItemRepository clubBoardItemRepository;

    @Autowired
    private ClubEventParticipantRepository clubEventParticipantRepository;

    @Autowired
    private ClubFeatureRepository clubFeatureRepository;

    @Autowired
    private ClubMemberPositionRepository clubMemberPositionRepository;

    @Autowired
    private ClubMemberRepository clubMemberRepository;

    @Autowired
    private ClubNoticeRepository clubNoticeRepository;

    @Autowired
    private ClubPositionPermissionRepository clubPositionPermissionRepository;

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
        clubScheduleVoteSelectionRepository.deleteAll();
        clubScheduleVoteOptionRepository.deleteAll();
        clubScheduleVoteRepository.deleteAll();
        clubEventParticipantRepository.deleteAll();
        clubScheduleEventRepository.deleteAll();
        clubBoardItemRepository.deleteAll();
        clubNoticeRepository.deleteAll();
        clubAttendanceCheckInRepository.deleteAll();
        clubAttendanceSessionRepository.deleteAll();
        clubMemberPositionRepository.deleteAll();
        clubPositionPermissionRepository.deleteAll();
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
                        List.of("NOTICE_CREATE")
                )
        );

        var response = clubPositionService.getRoleManagement(clubId, "role-owner-001");

        assertThat(response.permissionGroups())
                .extracting(item -> item.featureKey() + ":" + item.displayName())
                .containsExactly(
                        "NOTICE:공지관리",
                        "ROLE_MANAGEMENT:직책관리"
                );
        assertThat(response.positions()).singleElement().satisfies(position -> {
            assertThat(position.permissionKeys()).containsExactly("NOTICE_CREATE");
            assertThat(position.permissionCount()).isEqualTo(1);
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
                        List.of("POLL_CREATE")
                )
        )).isInstanceOf(SemoException.ValidationException.class)
                .hasMessageContaining("지원하지 않는 하위 권한");
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
                        List.of("NOTICE_CREATE", "POLL_CREATE")
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
                .containsExactly("NOTICE", "ROLE_MANAGEMENT");
        assertThat(hiddenBeforeUpdate.position().permissionKeys()).containsExactly("NOTICE_CREATE");

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
                        true,
                        List.of("NOTICE_CREATE", "NOTICE_UPDATE_SELF")
                )
        );

        assertThat(updated.position().permissionKeys())
                .containsExactlyInAnyOrder("NOTICE_CREATE", "NOTICE_UPDATE_SELF");
        assertThat(updated.position().permissionCount()).isEqualTo(2);
        assertThat(clubPositionPermissionRepository.findByClubPositionId(created.position().clubPositionId()))
                .extracting(item -> item.getPermissionKey())
                .containsExactlyInAnyOrder("NOTICE_CREATE", "NOTICE_UPDATE_SELF", "POLL_CREATE");
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
                        .permissionKey("POLL_CREATE")
                        .featureKey("POLL")
                        .displayName("투표 작성")
                        .description("투표 작성 권한")
                        .ownershipScope("CLUB")
                        .active(true)
                        .sortOrder(10)
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
}
