package semo.back.service.feature.clubfeature.biz;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import semo.back.service.database.pub.entity.FeaturePermissionCatalog;
import semo.back.service.database.pub.repository.ClubDashboardWidgetRepository;
import semo.back.service.database.pub.repository.ClubFeatureRepository;
import semo.back.service.database.pub.repository.ClubMemberPositionHistoryRepository;
import semo.back.service.database.pub.repository.ClubMemberPositionRepository;
import semo.back.service.database.pub.repository.ClubMemberRepository;
import semo.back.service.database.pub.repository.ClubNotificationRepository;
import semo.back.service.database.pub.repository.ClubPositionPermissionRepository;
import semo.back.service.database.pub.repository.ClubPositionRepository;
import semo.back.service.database.pub.repository.ClubProfileRepository;
import semo.back.service.database.pub.repository.ClubRepository;
import semo.back.service.database.pub.repository.DashboardWidgetCatalogRepository;
import semo.back.service.database.pub.repository.FeatureCatalogRepository;
import semo.back.service.database.pub.repository.FeaturePermissionCatalogRepository;
import semo.back.service.database.pub.repository.ProfileUserRepository;
import semo.back.service.database.pub.repository.TodoChecklistItemRepository;
import semo.back.service.database.pub.repository.TodoCommentRepository;
import semo.back.service.database.pub.repository.TodoItemApplicationRepository;
import semo.back.service.database.pub.repository.TodoItemAssigneeRepository;
import semo.back.service.database.pub.repository.TodoItemRepository;
import semo.back.service.feature.club.biz.ClubService;
import semo.back.service.feature.club.vo.CreateClubRequest;
import semo.back.service.feature.clubfeature.vo.ApplyClubOperationTemplateRequest;
import semo.back.service.feature.clubfeature.vo.ApplyClubPresetRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static semo.back.service.support.TestCatalogSeeder.seedDashboardWidgetCatalogs;
import static semo.back.service.support.TestCatalogSeeder.seedFeatureCatalogs;

@SpringBootTest
@ActiveProfiles("test")
class ClubOperationsCatalogServiceIntegrationTest {
    @Autowired
    private ClubOperationsCatalogService clubOperationsCatalogService;

    @Autowired
    private ClubService clubService;

    @Autowired
    private TodoCommentRepository todoCommentRepository;

    @Autowired
    private TodoChecklistItemRepository todoChecklistItemRepository;

    @Autowired
    private TodoItemApplicationRepository todoItemApplicationRepository;

    @Autowired
    private TodoItemAssigneeRepository todoItemAssigneeRepository;

    @Autowired
    private TodoItemRepository todoItemRepository;

    @Autowired
    private ClubMemberPositionHistoryRepository clubMemberPositionHistoryRepository;

    @Autowired
    private ClubMemberPositionRepository clubMemberPositionRepository;

    @Autowired
    private ClubPositionPermissionRepository clubPositionPermissionRepository;

    @Autowired
    private ClubPositionRepository clubPositionRepository;

    @Autowired
    private ClubDashboardWidgetRepository clubDashboardWidgetRepository;

    @Autowired
    private ClubNotificationRepository clubNotificationRepository;

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
    private FeaturePermissionCatalogRepository featurePermissionCatalogRepository;

    @Autowired
    private FeatureCatalogRepository featureCatalogRepository;

    @Autowired
    private DashboardWidgetCatalogRepository dashboardWidgetCatalogRepository;

    @BeforeEach
    void setUp() {
        todoCommentRepository.deleteAll();
        todoChecklistItemRepository.deleteAll();
        todoItemApplicationRepository.deleteAll();
        todoItemAssigneeRepository.deleteAll();
        todoItemRepository.deleteAll();
        clubMemberPositionHistoryRepository.deleteAll();
        clubMemberPositionRepository.deleteAll();
        clubPositionPermissionRepository.deleteAll();
        clubPositionRepository.deleteAll();
        clubDashboardWidgetRepository.deleteAll();
        clubNotificationRepository.deleteAll();
        clubFeatureRepository.deleteAll();
        clubProfileRepository.deleteAll();
        clubMemberRepository.deleteAll();
        clubRepository.deleteAll();
        profileUserRepository.deleteAll();
        seedFeatureCatalogs(featureCatalogRepository);
        seedDashboardWidgetCatalogs(dashboardWidgetCatalogRepository);
        seedPermissionCatalogs();
    }

    @Test
    void applySportsPreset_mergesFeaturesWidgetsAndDelegatedPositionsIdempotently() {
        Long clubId = createClub("preset-owner-001", "프리셋 관리자", "프리셋 테스트");

        var applied = clubOperationsCatalogService.applyPreset(
                clubId,
                "SPORTS",
                "preset-owner-001",
                new ApplyClubPresetRequest("MERGE")
        );

        assertThat(applied.appliedFeatureKeys())
                .contains("TOURNAMENT_RECORD", "BRACKET", "TODO", "FINANCE", "ROLE_MANAGEMENT");
        assertThat(applied.enabledWidgetKeys())
                .contains("BOARD_NOTICE", "SCHEDULE_OVERVIEW", "TOURNAMENT_RECORD_LATEST", "ATTENDANCE_STATUS", "FINANCE_STATUS");
        assertThat(applied.createdPositionNames()).containsExactly("경기 운영", "회계");
        assertThat(clubPositionRepository.findByClubIdOrderByDisplayNameAscClubPositionIdAsc(clubId)).hasSize(2);

        var reapplied = clubOperationsCatalogService.applyPreset(
                clubId,
                "SPORTS",
                "preset-owner-001",
                new ApplyClubPresetRequest("MERGE")
        );

        assertThat(reapplied.createdPositionNames()).isEmpty();
        assertThat(clubPositionRepository.findByClubIdOrderByDisplayNameAscClubPositionIdAsc(clubId)).hasSize(2);
        assertThat(clubOperationsCatalogService.getCatalog(clubId, "preset-owner-001").presets())
                .filteredOn(preset -> preset.presetKey().equals("SPORTS"))
                .singleElement()
                .returns(true, preset -> preset.includedInCurrentConfiguration());
    }

    @Test
    void applyTournamentTemplate_createsAssignedTodoWithCompleteChecklist() {
        Long clubId = createClub("preset-owner-002", "템플릿 관리자", "템플릿 테스트");
        clubOperationsCatalogService.applyPreset(
                clubId,
                "SPORTS",
                "preset-owner-002",
                new ApplyClubPresetRequest("MERGE")
        );

        var applied = clubOperationsCatalogService.applyTemplate(
                clubId,
                "TOURNAMENT_OPERATIONS",
                "preset-owner-002",
                new ApplyClubOperationTemplateRequest("가을 대회 준비", null)
        );

        assertThat(applied.checklistItemCount()).isEqualTo(6);
        assertThat(todoItemRepository.findById(applied.todoItemId()).orElseThrow())
                .returns("가을 대회 준비", item -> item.getTitle())
                .returns("HIGH", item -> item.getPriorityCode())
                .returns("DIRECT_ASSIGN", item -> item.getAssignmentMode());
        assertThat(todoChecklistItemRepository.findByTodoItemIdOrderBySortOrderAscTodoChecklistItemIdAsc(applied.todoItemId()))
                .hasSize(6)
                .extracting(item -> item.getContent())
                .containsExactly(
                        "대회 요강과 모집 기간 확정",
                        "참가 신청·대기열 검토",
                        "참가비 납부 확인",
                        "코트·시간표 등록",
                        "현장 체크인",
                        "결과·순위 확정 및 공유"
                );
    }

    private Long createClub(String userKey, String displayName, String clubName) {
        return clubService.createClub(
                userKey,
                displayName,
                new CreateClubRequest(clubName, "운영 프리셋 테스트", "OTHER", "PUBLIC", "APPROVAL", null)
        ).clubId();
    }

    private void seedPermissionCatalogs() {
        featurePermissionCatalogRepository.deleteAll();
        Map<String, String> featureByPermission = Map.ofEntries(
                Map.entry("NOTICE_CREATE", "NOTICE"),
                Map.entry("NOTICE_UPDATE_SELF", "NOTICE"),
                Map.entry("NOTICE_DELETE_SELF", "NOTICE"),
                Map.entry("SCHEDULE_CREATE", "SCHEDULE_MANAGE"),
                Map.entry("SCHEDULE_UPDATE_SELF", "SCHEDULE_MANAGE"),
                Map.entry("SCHEDULE_DELETE_SELF", "SCHEDULE_MANAGE"),
                Map.entry("ATTENDANCE_MANAGE", "ATTENDANCE"),
                Map.entry("TOURNAMENT_RECORD_CREATE", "TOURNAMENT_RECORD"),
                Map.entry("TOURNAMENT_RECORD_UPDATE_SELF", "TOURNAMENT_RECORD"),
                Map.entry("TOURNAMENT_RECORD_REVIEW", "TOURNAMENT_RECORD"),
                Map.entry("BRACKET_CREATE", "BRACKET"),
                Map.entry("BRACKET_UPDATE_SELF", "BRACKET"),
                Map.entry("BRACKET_REVIEW", "BRACKET"),
                Map.entry("TODO_VIEW", "TODO"),
                Map.entry("TODO_CREATE", "TODO"),
                Map.entry("TODO_ASSIGN", "TODO"),
                Map.entry("TODO_MANAGE_STATUS", "TODO"),
                Map.entry("FINANCE_VIEW", "FINANCE"),
                Map.entry("FINANCE_BILLING_ISSUE", "FINANCE"),
                Map.entry("FINANCE_REQUEST_REVIEW", "FINANCE"),
                Map.entry("FINANCE_EXPENSE_CREATE", "FINANCE"),
                Map.entry("FINANCE_PAYMENT_UPDATE", "FINANCE"),
                Map.entry("FINANCE_EXPORT", "FINANCE"),
                Map.entry("FINANCE_PERIOD_CLOSE", "FINANCE"),
                Map.entry("ROLE_MANAGEMENT_VIEW", "ROLE_MANAGEMENT"),
                Map.entry("ROLE_MANAGEMENT_CREATE", "ROLE_MANAGEMENT"),
                Map.entry("ROLE_MANAGEMENT_UPDATE", "ROLE_MANAGEMENT"),
                Map.entry("ROLE_MANAGEMENT_DELETE", "ROLE_MANAGEMENT"),
                Map.entry("ROLE_MANAGEMENT_ASSIGN", "ROLE_MANAGEMENT")
        );
        int sortOrder = 10;
        for (Map.Entry<String, String> entry : featureByPermission.entrySet()) {
            featurePermissionCatalogRepository.save(FeaturePermissionCatalog.builder()
                    .permissionKey(entry.getKey())
                    .featureKey(entry.getValue())
                    .displayName(entry.getKey())
                    .description("테스트 권한")
                    .ownershipScope("CLUB")
                    .active(true)
                    .sortOrder(sortOrder++)
                    .build());
        }
    }
}
