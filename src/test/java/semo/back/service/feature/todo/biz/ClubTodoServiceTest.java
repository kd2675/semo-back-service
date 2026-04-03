package semo.back.service.feature.todo.biz;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import semo.back.service.database.pub.entity.ClubMember;
import semo.back.service.database.pub.entity.ClubMemberPosition;
import semo.back.service.database.pub.entity.ClubPosition;
import semo.back.service.database.pub.entity.ClubPositionPermission;
import semo.back.service.database.pub.entity.ClubProfile;
import semo.back.service.database.pub.entity.TodoItem;
import semo.back.service.database.pub.repository.ClubMemberPositionRepository;
import semo.back.service.database.pub.repository.ClubFeatureRepository;
import semo.back.service.database.pub.repository.ClubMemberRepository;
import semo.back.service.database.pub.repository.ClubPositionPermissionRepository;
import semo.back.service.database.pub.repository.ClubPositionRepository;
import semo.back.service.database.pub.repository.ClubProfileRepository;
import semo.back.service.database.pub.repository.ClubRepository;
import semo.back.service.database.pub.repository.FeatureCatalogRepository;
import semo.back.service.database.pub.repository.ProfileUserRepository;
import semo.back.service.database.pub.repository.TodoItemRepository;
import semo.back.service.feature.club.biz.ClubService;
import semo.back.service.feature.club.vo.CreateClubRequest;
import semo.back.service.feature.clubfeature.biz.ClubFeatureService;
import semo.back.service.feature.clubfeature.vo.UpdateClubFeaturesRequest;
import semo.back.service.feature.profile.biz.ProfileUserService;
import semo.back.service.feature.todo.vo.CreateClubTodoRequest;
import semo.back.service.feature.todo.vo.UpdateClubTodoRequest;
import semo.back.service.feature.todo.vo.UpdateTodoStatusRequest;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static semo.back.service.support.TestCatalogSeeder.seedFeatureCatalogs;

@SpringBootTest
@ActiveProfiles("test")
class ClubTodoServiceTest {

    @Autowired
    private ClubTodoService clubTodoService;

    @Autowired
    private ClubService clubService;

    @Autowired
    private ClubFeatureService clubFeatureService;

    @Autowired
    private ProfileUserService profileUserService;

    @Autowired
    private TodoItemRepository todoItemRepository;

    @Autowired
    private ClubFeatureRepository clubFeatureRepository;

    @Autowired
    private ClubProfileRepository clubProfileRepository;

    @Autowired
    private ClubMemberPositionRepository clubMemberPositionRepository;

    @Autowired
    private ClubPositionPermissionRepository clubPositionPermissionRepository;

    @Autowired
    private ClubPositionRepository clubPositionRepository;

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
        todoItemRepository.deleteAll();
        clubMemberPositionRepository.deleteAll();
        clubPositionPermissionRepository.deleteAll();
        clubPositionRepository.deleteAll();
        clubFeatureRepository.deleteAll();
        clubProfileRepository.deleteAll();
        clubMemberRepository.deleteAll();
        clubRepository.deleteAll();
        profileUserRepository.deleteAll();
        seedFeatureCatalogs(featureCatalogRepository);
    }

    @Test
    void createTodo_directAssignWithoutAssignee_throwsValidation() {
        Long clubId = createEnabledClub("todo-owner-001", "Todo Owner", "Todo Club 1");

        assertThatThrownBy(() -> clubTodoService.createTodo(
                clubId,
                "todo-owner-001",
                new CreateClubTodoRequest(
                        "운영 공지 정리",
                        "이번 주 공지 정리",
                        "OPERATIONS",
                        "DIRECT_ASSIGN",
                        null,
                        null
                )
        )).hasMessageContaining("담당자를 선택");
    }

    @Test
    void createTodo_openSupportWithAssignee_throwsValidation() {
        Long clubId = createEnabledClub("todo-owner-002", "Todo Owner 2", "Todo Club 2");
        Long memberClubProfileId = addActiveMember(clubId, "todo-member-002", "Todo Member 2");

        assertThatThrownBy(() -> clubTodoService.createTodo(
                clubId,
                "todo-owner-002",
                new CreateClubTodoRequest(
                        "장비 운반",
                        "행사 장비를 같이 옮깁니다.",
                        "VOLUNTEER",
                        "OPEN_SUPPORT",
                        memberClubProfileId,
                        null
                )
        )).hasMessageContaining("담당자를 비워");
    }

    @Test
    void claimTodo_openSupport_unassigned_success() {
        Long clubId = createEnabledClub("todo-owner-003", "Todo Owner 3", "Todo Club 3");
        addActiveMember(clubId, "todo-member-003", "Todo Member 3");

        Long todoItemId = clubTodoService.createTodo(
                clubId,
                "todo-owner-003",
                new CreateClubTodoRequest(
                        "현장 안내",
                        "입구 안내를 맡아주세요.",
                        "VOLUNTEER",
                        "OPEN_SUPPORT",
                        null,
                        null
                )
        ).todoItemId();

        var claimed = clubTodoService.claimTodo(clubId, todoItemId, "todo-member-003");

        assertThat(claimed.statusCode()).isEqualTo("IN_PROGRESS");
        assertThat(claimed.assignedDisplayName()).isEqualTo("Todo Member 3");
    }

    @Test
    void claimTodo_alreadyClaimed_throwsConflict() {
        Long clubId = createEnabledClub("todo-owner-004", "Todo Owner 4", "Todo Club 4");
        addActiveMember(clubId, "todo-member-004a", "Todo Member 4A");
        addActiveMember(clubId, "todo-member-004b", "Todo Member 4B");

        Long todoItemId = clubTodoService.createTodo(
                clubId,
                "todo-owner-004",
                new CreateClubTodoRequest(
                        "현수막 설치",
                        null,
                        "VOLUNTEER",
                        "OPEN_SUPPORT",
                        null,
                        null
                )
        ).todoItemId();

        clubTodoService.claimTodo(clubId, todoItemId, "todo-member-004a");

        assertThatThrownBy(() -> clubTodoService.claimTodo(clubId, todoItemId, "todo-member-004b"))
                .hasMessageContaining("이미 다른 멤버가 맡은 업무");
    }

    @Test
    void completeTodo_openSupport_withoutClaim_throwsForbidden() {
        Long clubId = createEnabledClub("todo-owner-005", "Todo Owner 5", "Todo Club 5");
        addActiveMember(clubId, "todo-member-005", "Todo Member 5");

        Long todoItemId = clubTodoService.createTodo(
                clubId,
                "todo-owner-005",
                new CreateClubTodoRequest(
                        "테이블 세팅",
                        null,
                        "VOLUNTEER",
                        "OPEN_SUPPORT",
                        null,
                        null
                )
        ).todoItemId();

        assertThatThrownBy(() -> clubTodoService.completeTodo(clubId, todoItemId, "todo-member-005"))
                .hasMessageContaining("배정된 업무");
    }

    @Test
    void completeTodo_notAssignee_throwsForbidden() {
        Long clubId = createEnabledClub("todo-owner-006", "Todo Owner 6", "Todo Club 6");
        Long assignedClubProfileId = addActiveMember(clubId, "todo-member-006a", "Todo Member 6A");
        addActiveMember(clubId, "todo-member-006b", "Todo Member 6B");

        Long todoItemId = clubTodoService.createTodo(
                clubId,
                "todo-owner-006",
                new CreateClubTodoRequest(
                        "정산표 작성",
                        null,
                        "OPERATIONS",
                        "DIRECT_ASSIGN",
                        assignedClubProfileId,
                        null
                )
        ).todoItemId();

        assertThatThrownBy(() -> clubTodoService.completeTodo(clubId, todoItemId, "todo-member-006b"))
                .hasMessageContaining("배정된 업무");
    }

    @Test
    void updateStatus_terminalToTerminal_invalidTransition_throwsValidation() {
        Long clubId = createEnabledClub("todo-owner-007", "Todo Owner 7", "Todo Club 7");
        Long assignedClubProfileId = addActiveMember(clubId, "todo-member-007", "Todo Member 7");

        Long todoItemId = clubTodoService.createTodo(
                clubId,
                "todo-owner-007",
                new CreateClubTodoRequest(
                        "대진표 업로드",
                        null,
                        "OPERATIONS",
                        "DIRECT_ASSIGN",
                        assignedClubProfileId,
                        null
                )
        ).todoItemId();

        clubTodoService.updateTodoStatus(
                clubId,
                todoItemId,
                "todo-owner-007",
                new UpdateTodoStatusRequest("COMPLETED")
        );

        assertThatThrownBy(() -> clubTodoService.updateTodoStatus(
                clubId,
                todoItemId,
                "todo-owner-007",
                new UpdateTodoStatusRequest("CANCELED")
        )).hasMessageContaining("먼저 다시 열어야");
    }

    @Test
    void updateTodo_metadataOnly_keepsInactiveDirectAssignee() {
        Long clubId = createEnabledClub("todo-owner-007b", "Todo Owner 7B", "Todo Club 7B");
        Long assignedClubProfileId = addActiveMember(clubId, "todo-member-007b", "Todo Member 7B");

        Long todoItemId = clubTodoService.createTodo(
                clubId,
                "todo-owner-007b",
                new CreateClubTodoRequest(
                        "자료 취합",
                        "원본 설명",
                        "OPERATIONS",
                        "DIRECT_ASSIGN",
                        assignedClubProfileId,
                        null
                )
        ).todoItemId();

        markMemberInactive(assignedClubProfileId);

        var updated = clubTodoService.updateTodo(
                clubId,
                todoItemId,
                "todo-owner-007b",
                new UpdateClubTodoRequest(
                        "자료 취합 수정",
                        "수정 설명",
                        "OPERATIONS",
                        "DIRECT_ASSIGN",
                        assignedClubProfileId,
                        null
                )
        );

        assertThat(updated.assignedClubProfileId()).isEqualTo(assignedClubProfileId);
        assertThat(updated.title()).isEqualTo("자료 취합 수정");
    }

    @Test
    void updateTodo_reassignToInactiveMember_throwsValidation() {
        Long clubId = createEnabledClub("todo-owner-007c", "Todo Owner 7C", "Todo Club 7C");
        Long activeClubProfileId = addActiveMember(clubId, "todo-member-007c-a", "Todo Member 7C A");
        Long inactiveClubProfileId = addActiveMember(clubId, "todo-member-007c-b", "Todo Member 7C B");

        Long todoItemId = clubTodoService.createTodo(
                clubId,
                "todo-owner-007c",
                new CreateClubTodoRequest(
                        "현장 브리핑",
                        "원본 설명",
                        "OPERATIONS",
                        "DIRECT_ASSIGN",
                        activeClubProfileId,
                        null
                )
        ).todoItemId();

        markMemberInactive(inactiveClubProfileId);

        assertThatThrownBy(() -> clubTodoService.updateTodo(
                clubId,
                todoItemId,
                "todo-owner-007c",
                new UpdateClubTodoRequest(
                        "현장 브리핑",
                        "원본 설명",
                        "OPERATIONS",
                        "DIRECT_ASSIGN",
                        inactiveClubProfileId,
                        null
                )
        )).hasMessageContaining("현재 활성 멤버에게만 업무를 배정");
    }

    @Test
    void getTodos_overdueComputedNotPersisted() {
        Long clubId = createEnabledClub("todo-owner-008", "Todo Owner 8", "Todo Club 8");
        Long memberClubProfileId = addActiveMember(clubId, "todo-member-008", "Todo Member 8");
        String overdueDueAt = LocalDateTime.now().minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        clubTodoService.createTodo(
                clubId,
                "todo-owner-008",
                new CreateClubTodoRequest(
                        "마감 지난 업무",
                        null,
                        "OPERATIONS",
                        "DIRECT_ASSIGN",
                        memberClubProfileId,
                        overdueDueAt
                )
        );

        var todos = clubTodoService.getTodos(clubId, "todo-member-008");

        assertThat(todos.overdueCount()).isEqualTo(1);
        assertThat(todos.myTodos()).singleElement().satisfies(item -> {
            assertThat(item.overdue()).isTrue();
            assertThat(item.statusCode()).isEqualTo("OPEN");
        });
    }

    @Test
    void featureDisabled_allTodoApis_forbidden() {
        Long clubId = createClub("todo-owner-009", "Todo Owner 9", "Todo Club 9");

        assertThatThrownBy(() -> clubTodoService.getTodos(clubId, "todo-owner-009"))
                .hasMessageContaining("할 일 기능이 활성화되지 않았습니다");
    }

    @Test
    void getAdminTodos_viewOnlyMember_canAccessWithReadOnlyFlags() {
        Long clubId = createEnabledClubWithRoleManagement("todo-owner-009b", "Todo Owner 9B", "Todo Club 9B");
        Long viewerClubProfileId = addActiveMember(clubId, "todo-viewer-009b", "Todo Viewer 9B");
        grantPermission(clubId, viewerClubProfileId, "TODO_VIEW", "todo-owner-009b");

        clubTodoService.createTodo(
                clubId,
                "todo-owner-009b",
                new CreateClubTodoRequest(
                        "운영 확인",
                        "조회 권한 테스트",
                        "OPERATIONS",
                        "OPEN_SUPPORT",
                        null,
                        null
                )
        );

        var response = clubTodoService.getAdminTodos(clubId, "todo-viewer-009b", null, null, null, null);

        assertThat(response.items()).hasSize(1);
        assertThat(response.canCreate()).isFalse();
        assertThat(response.canAssign()).isFalse();
        assertThat(response.canManageStatus()).isFalse();
    }

    @Test
    void createTodo_createOnly_openSupport_success() {
        Long clubId = createEnabledClubWithRoleManagement("todo-owner-009c", "Todo Owner 9C", "Todo Club 9C");
        Long memberClubProfileId = addActiveMember(clubId, "todo-member-009c", "Todo Member 9C");
        grantPermission(clubId, memberClubProfileId, "TODO_CREATE", "todo-owner-009c");

        var created = clubTodoService.createTodo(
                clubId,
                "todo-member-009c",
                new CreateClubTodoRequest(
                        "현장 정리",
                        "생성 권한 테스트",
                        "VOLUNTEER",
                        "OPEN_SUPPORT",
                        null,
                        null
                )
        );

        assertThat(created.assignmentMode()).isEqualTo("OPEN_SUPPORT");
        assertThat(created.assignedClubProfileId()).isNull();
    }

    @Test
    void createTodo_createOnly_directAssign_forbidden() {
        Long clubId = createEnabledClubWithRoleManagement("todo-owner-009d", "Todo Owner 9D", "Todo Club 9D");
        Long memberClubProfileId = addActiveMember(clubId, "todo-member-009d", "Todo Member 9D");
        grantPermission(clubId, memberClubProfileId, "TODO_CREATE", "todo-owner-009d");

        assertThatThrownBy(() -> clubTodoService.createTodo(
                clubId,
                "todo-member-009d",
                new CreateClubTodoRequest(
                        "직접 배정 생성",
                        "배정 권한 없음",
                        "OPERATIONS",
                        "DIRECT_ASSIGN",
                        memberClubProfileId,
                        null
                )
        )).hasMessageContaining("담당자를 배정할 권한");
    }

    @Test
    void createTodo_assignOnly_forbidden() {
        Long clubId = createEnabledClubWithRoleManagement("todo-owner-009e", "Todo Owner 9E", "Todo Club 9E");
        Long memberClubProfileId = addActiveMember(clubId, "todo-member-009e", "Todo Member 9E");
        grantPermission(clubId, memberClubProfileId, "TODO_ASSIGN", "todo-owner-009e");

        assertThatThrownBy(() -> clubTodoService.createTodo(
                clubId,
                "todo-member-009e",
                new CreateClubTodoRequest(
                        "생성 불가",
                        "배정 전용 권한",
                        "OPERATIONS",
                        "OPEN_SUPPORT",
                        null,
                        null
                )
        )).hasMessageContaining("등록할 권한");
    }

    @Test
    void updateTodo_assignOnly_cannotChangeMetadata() {
        Long clubId = createEnabledClubWithRoleManagement("todo-owner-010", "Todo Owner 10", "Todo Club 10");
        Long memberClubProfileId = addActiveMember(clubId, "todo-member-010", "Todo Member 10");
        grantPermission(clubId, memberClubProfileId, "TODO_ASSIGN", "todo-owner-010");

        var created = clubTodoService.createTodo(
                clubId,
                "todo-owner-010",
                new CreateClubTodoRequest(
                        "운영 체크리스트",
                        "원본 설명",
                        "OPERATIONS",
                        "OPEN_SUPPORT",
                        null,
                        null
                )
        );

        assertThatThrownBy(() -> clubTodoService.updateTodo(
                clubId,
                created.todoItemId(),
                "todo-member-010",
                new UpdateClubTodoRequest(
                        "바뀐 제목",
                        "원본 설명",
                        "OPERATIONS",
                        "OPEN_SUPPORT",
                        null,
                        null
                )
        )).hasMessageContaining("기본 정보를 수정할 권한");
    }

    @Test
    void updateTodo_assignOnly_canChangeAssignment() {
        Long clubId = createEnabledClubWithRoleManagement("todo-owner-011", "Todo Owner 11", "Todo Club 11");
        Long memberClubProfileId = addActiveMember(clubId, "todo-member-011", "Todo Member 11");
        grantPermission(clubId, memberClubProfileId, "TODO_ASSIGN", "todo-owner-011");

        var created = clubTodoService.createTodo(
                clubId,
                "todo-owner-011",
                new CreateClubTodoRequest(
                        "현장 운영",
                        "원본 설명",
                        "OPERATIONS",
                        "OPEN_SUPPORT",
                        null,
                        null
                )
        );

        var updated = clubTodoService.updateTodo(
                clubId,
                created.todoItemId(),
                "todo-member-011",
                new UpdateClubTodoRequest(
                        "현장 운영",
                        "원본 설명",
                        "OPERATIONS",
                        "DIRECT_ASSIGN",
                        memberClubProfileId,
                        null
                )
        );

        assertThat(updated.assignedClubProfileId()).isEqualTo(memberClubProfileId);
        assertThat(updated.assignedDisplayName()).isEqualTo("Todo Member 11");
    }

    @Test
    void updateTodo_createOnly_cannotChangeAssignment() {
        Long clubId = createEnabledClubWithRoleManagement("todo-owner-012", "Todo Owner 12", "Todo Club 12");
        Long memberClubProfileId = addActiveMember(clubId, "todo-member-012", "Todo Member 12");
        grantPermission(clubId, memberClubProfileId, "TODO_CREATE", "todo-owner-012");

        var created = clubTodoService.createTodo(
                clubId,
                "todo-owner-012",
                new CreateClubTodoRequest(
                        "게시판 정리",
                        "원본 설명",
                        "OPERATIONS",
                        "OPEN_SUPPORT",
                        null,
                        null
                )
        );

        assertThatThrownBy(() -> clubTodoService.updateTodo(
                clubId,
                created.todoItemId(),
                "todo-member-012",
                new UpdateClubTodoRequest(
                        "게시판 정리",
                        "원본 설명",
                        "OPERATIONS",
                        "DIRECT_ASSIGN",
                        memberClubProfileId,
                        null
                )
        )).hasMessageContaining("담당자를 조정할 권한");
    }

    @Test
    void updateTodo_createOnly_canChangeMetadata() {
        Long clubId = createEnabledClubWithRoleManagement("todo-owner-013", "Todo Owner 13", "Todo Club 13");
        Long memberClubProfileId = addActiveMember(clubId, "todo-member-013", "Todo Member 13");
        grantPermission(clubId, memberClubProfileId, "TODO_CREATE", "todo-owner-013");

        var created = clubTodoService.createTodo(
                clubId,
                "todo-owner-013",
                new CreateClubTodoRequest(
                        "SNS 업로드",
                        "원본 설명",
                        "OPERATIONS",
                        "OPEN_SUPPORT",
                        null,
                        null
                )
        );

        var updated = clubTodoService.updateTodo(
                clubId,
                created.todoItemId(),
                "todo-member-013",
                new UpdateClubTodoRequest(
                        "SNS 업로드 수정",
                        "수정 설명",
                        "OPERATIONS",
                        "OPEN_SUPPORT",
                        null,
                        null
                )
        );

        assertThat(updated.title()).isEqualTo("SNS 업로드 수정");
        assertThat(updated.description()).isEqualTo("수정 설명");
    }

    @Test
    void updateTodo_createOnly_preservesAssignedByWhenMetadataChanges() {
        Long clubId = createEnabledClubWithRoleManagement("todo-owner-014", "Todo Owner 14", "Todo Club 14");
        Long ownerClubProfileId = findClubProfileId(clubId, "todo-owner-014");
        Long memberClubProfileId = addActiveMember(clubId, "todo-member-014", "Todo Member 14");
        grantPermission(clubId, memberClubProfileId, "TODO_CREATE", "todo-owner-014");

        var created = clubTodoService.createTodo(
                clubId,
                "todo-owner-014",
                new CreateClubTodoRequest(
                        "출석 체크 보조",
                        "원본 설명",
                        "OPERATIONS",
                        "DIRECT_ASSIGN",
                        memberClubProfileId,
                        null
                )
        );

        clubTodoService.updateTodo(
                clubId,
                created.todoItemId(),
                "todo-member-014",
                new UpdateClubTodoRequest(
                        "출석 체크 보조 수정",
                        "수정 설명",
                        "OPERATIONS",
                        "DIRECT_ASSIGN",
                        memberClubProfileId,
                        null
                )
        );

        TodoItem updated = todoItemRepository.findById(created.todoItemId()).orElseThrow();

        assertThat(updated.getAssignedByClubProfileId()).isEqualTo(ownerClubProfileId);
    }

    private Long createEnabledClub(String ownerUserKey, String ownerDisplayName, String clubName) {
        Long clubId = createClub(ownerUserKey, ownerDisplayName, clubName);
        clubFeatureService.updateClubFeatures(
                clubId,
                ownerUserKey,
                new UpdateClubFeaturesRequest(List.of("TODO"))
        );
        return clubId;
    }

    private Long createEnabledClubWithRoleManagement(String ownerUserKey, String ownerDisplayName, String clubName) {
        Long clubId = createClub(ownerUserKey, ownerDisplayName, clubName);
        clubFeatureService.updateClubFeatures(
                clubId,
                ownerUserKey,
                new UpdateClubFeaturesRequest(List.of("TODO", "ROLE_MANAGEMENT"))
        );
        return clubId;
    }

    private Long createClub(String ownerUserKey, String ownerDisplayName, String clubName) {
        return clubService.createClub(
                ownerUserKey,
                ownerDisplayName,
                new CreateClubRequest(
                        clubName,
                        "할 일 테스트",
                        "OTHER",
                        "PUBLIC",
                        "APPROVAL",
                        null
                )
        ).clubId();
    }

    private Long addActiveMember(Long clubId, String userKey, String displayName) {
        Long profileId = profileUserService.resolveProfileId(userKey, displayName);
        ClubMember member = clubMemberRepository.save(ClubMember.builder()
                .clubId(clubId)
                .profileId(profileId)
                .roleCode("MEMBER")
                .membershipStatus("ACTIVE")
                .joinedAt(LocalDateTime.now())
                .lastActivityAt(LocalDateTime.now())
                .build());
        ClubProfile clubProfile = clubProfileRepository.save(ClubProfile.builder()
                .clubMemberId(member.getClubMemberId())
                .displayName(displayName)
                .tagline(null)
                .introText(null)
                .avatarFileName(null)
                .build());
        return clubProfile.getClubProfileId();
    }

    private void grantPermission(Long clubId, Long memberClubProfileId, String permissionKey, String ownerUserKey) {
        Long ownerClubProfileId = findClubProfileId(clubId, ownerUserKey);
        ClubProfile memberClubProfile = clubProfileRepository.findById(memberClubProfileId).orElseThrow();
        ClubPosition position = clubPositionRepository.save(ClubPosition.builder()
                .clubId(clubId)
                .positionCode("TODO_" + permissionKey)
                .displayName(permissionKey)
                .description(null)
                .iconName(null)
                .colorHex(null)
                .active(true)
                .createdByClubProfileId(ownerClubProfileId)
                .build());
        clubPositionPermissionRepository.save(ClubPositionPermission.builder()
                .clubPositionId(position.getClubPositionId())
                .permissionKey(permissionKey)
                .build());
        clubMemberPositionRepository.save(ClubMemberPosition.builder()
                .clubMemberId(memberClubProfile.getClubMemberId())
                .clubPositionId(position.getClubPositionId())
                .assignedByClubProfileId(ownerClubProfileId)
                .assignedAt(LocalDateTime.now())
                .build());
    }

    private void markMemberInactive(Long clubProfileId) {
        ClubProfile clubProfile = clubProfileRepository.findById(clubProfileId).orElseThrow();
        ClubMember clubMember = clubMemberRepository.findById(clubProfile.getClubMemberId()).orElseThrow();
        clubMember.updateMembershipStatus("INACTIVE");
        clubMemberRepository.save(clubMember);
    }

    private Long findClubProfileId(Long clubId, String userKey) {
        Long profileId = profileUserRepository.findByUserKey(userKey).orElseThrow().getProfileId();
        Long clubMemberId = clubMemberRepository.findByClubIdAndProfileId(clubId, profileId).orElseThrow().getClubMemberId();
        return clubProfileRepository.findByClubMemberId(clubMemberId).orElseThrow().getClubProfileId();
    }
}
