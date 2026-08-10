package semo.back.service.feature.clubfeature.biz;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import semo.back.service.common.exception.SemoException;
import semo.back.service.database.pub.entity.FeatureCatalog;
import semo.back.service.database.pub.repository.ClubPositionRepository;
import semo.back.service.database.pub.repository.FeatureCatalogRepository;
import semo.back.service.database.pub.repository.FeaturePermissionCatalogRepository;
import semo.back.service.feature.club.biz.policy.ClubAccessResolver;
import semo.back.service.feature.clubfeature.vo.ApplyClubOperationTemplateRequest;
import semo.back.service.feature.clubfeature.vo.ApplyClubOperationTemplateResponse;
import semo.back.service.feature.clubfeature.vo.ApplyClubPresetRequest;
import semo.back.service.feature.clubfeature.vo.ApplyClubPresetResponse;
import semo.back.service.feature.clubfeature.vo.ClubFeatureResponse;
import semo.back.service.feature.clubfeature.vo.ClubOperationTemplateResponse;
import semo.back.service.feature.clubfeature.vo.ClubOperationsCatalogResponse;
import semo.back.service.feature.clubfeature.vo.ClubPresetResponse;
import semo.back.service.feature.clubfeature.vo.UpdateClubFeaturesRequest;
import semo.back.service.feature.dashboard.biz.ClubDashboardService;
import semo.back.service.feature.dashboard.vo.ClubDashboardEditorResponse;
import semo.back.service.feature.dashboard.vo.ClubDashboardWidgetResponse;
import semo.back.service.feature.dashboard.vo.UpdateClubDashboardLayoutRequest;
import semo.back.service.feature.dashboard.vo.UpdateClubDashboardWidgetItemRequest;
import semo.back.service.feature.position.biz.ClubPositionService;
import semo.back.service.feature.position.vo.CreateClubPositionRequest;
import semo.back.service.feature.todo.biz.ClubTodoCollaborationService;
import semo.back.service.feature.todo.biz.ClubTodoService;
import semo.back.service.feature.todo.vo.CreateClubTodoRequest;
import semo.back.service.feature.todo.vo.CreateTodoChecklistItemRequest;
import semo.back.service.feature.todo.vo.TodoSummaryResponse;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubOperationsCatalogService {
    private static final String APPLY_MODE_MERGE = "MERGE";
    private static final String APPLY_MODE_REPLACE = "REPLACE";
    private static final String FEATURE_TODO = "TODO";

    private static final List<PresetDefinition> PRESETS = List.of(
            new PresetDefinition(
                    "SPORTS",
                    "스포츠 모임",
                    "일정·출석부터 대회, 대진표, 회비와 경기 운영 직책까지 연결합니다.",
                    "sports_tennis",
                    List.of(
                            "JOIN_REQUEST", "NOTICE", "SCHEDULE_MANAGE", "ATTENDANCE",
                            "TOURNAMENT_RECORD", "BRACKET", "TODO", "FINANCE",
                            "MEMBER_DIRECTORY", "FEEDBACK", "ROLE_MANAGEMENT", "HANDOVER", "DECISION_LOG"
                    ),
                    List.of(
                            "BOARD_NOTICE", "SCHEDULE_OVERVIEW", "TOURNAMENT_RECORD_LATEST",
                            "ATTENDANCE_STATUS", "FINANCE_STATUS"
                    ),
                    List.of(
                            new PositionDefinition(
                                    "SPORTS_OPERATIONS",
                                    "경기 운영",
                                    "일정, 출석, 대회와 대진표를 운영합니다.",
                                    "sports_score",
                                    "#2563EB",
                                    List.of(
                                            "SCHEDULE_CREATE", "SCHEDULE_UPDATE_SELF", "SCHEDULE_DELETE_SELF",
                                            "ATTENDANCE_MANAGE", "TOURNAMENT_RECORD_CREATE",
                                            "TOURNAMENT_RECORD_UPDATE_SELF", "TOURNAMENT_RECORD_REVIEW",
                                            "BRACKET_CREATE", "BRACKET_UPDATE_SELF", "BRACKET_REVIEW",
                                            "TODO_VIEW", "TODO_CREATE", "TODO_ASSIGN", "TODO_MANAGE_STATUS"
                                    )
                            ),
                            treasurerPosition()
                    )
            ),
            new PresetDefinition(
                    "ACADEMIC",
                    "학회·동아리",
                    "공지, 일정, 투표, 업무, 재정과 의사결정 기록 중심으로 집행부 운영을 구성합니다.",
                    "school",
                    List.of(
                            "JOIN_REQUEST", "NOTICE", "SCHEDULE_MANAGE", "POLL", "ATTENDANCE",
                            "TODO", "FINANCE", "MEMBER_DIRECTORY", "FEEDBACK",
                            "ROLE_MANAGEMENT", "HANDOVER", "DECISION_LOG"
                    ),
                    List.of("BOARD_NOTICE", "SCHEDULE_OVERVIEW", "POLL_STATUS", "FINANCE_STATUS", "PROFILE_SUMMARY"),
                    List.of(
                            new PositionDefinition(
                                    "PROGRAM_MANAGER",
                                    "프로그램 운영",
                                    "공지, 일정, 투표, 업무와 의사결정을 운영합니다.",
                                    "event_note",
                                    "#7C3AED",
                                    List.of(
                                            "NOTICE_CREATE", "NOTICE_UPDATE_SELF", "NOTICE_DELETE_SELF",
                                            "SCHEDULE_CREATE", "SCHEDULE_UPDATE_SELF", "SCHEDULE_DELETE_SELF",
                                            "POLL_CREATE", "POLL_UPDATE_SELF", "POLL_DELETE_SELF",
                                            "TODO_VIEW", "TODO_CREATE", "TODO_ASSIGN", "TODO_MANAGE_STATUS",
                                            "DECISION_VIEW", "DECISION_MANAGE"
                                    )
                            ),
                            treasurerPosition()
                    )
            ),
            new PresetDefinition(
                    "STUDY",
                    "스터디",
                    "가벼운 일정·투표·출석과 반복 업무 중심으로 필요한 기능만 간결하게 구성합니다.",
                    "menu_book",
                    List.of(
                            "JOIN_REQUEST", "NOTICE", "SCHEDULE_MANAGE", "POLL", "ATTENDANCE",
                            "TODO", "MEMBER_DIRECTORY", "FEEDBACK", "ROLE_MANAGEMENT", "DECISION_LOG"
                    ),
                    List.of("BOARD_STRIP", "SCHEDULE_OVERVIEW", "POLL_STATUS", "ATTENDANCE_STATUS", "PROFILE_SUMMARY"),
                    List.of(new PositionDefinition(
                            "STUDY_MANAGER",
                            "스터디 운영",
                            "공지, 일정, 투표, 출석과 업무를 관리합니다.",
                            "co_present",
                            "#059669",
                            List.of(
                                    "NOTICE_CREATE", "NOTICE_UPDATE_SELF", "NOTICE_DELETE_SELF",
                                    "SCHEDULE_CREATE", "SCHEDULE_UPDATE_SELF", "SCHEDULE_DELETE_SELF",
                                    "POLL_CREATE", "POLL_UPDATE_SELF", "POLL_DELETE_SELF",
                                    "ATTENDANCE_MANAGE", "TODO_VIEW", "TODO_CREATE", "TODO_ASSIGN", "TODO_MANAGE_STATUS"
                            )
                    ))
            )
    );

    private static final List<TemplateDefinition> TEMPLATES = List.of(
            new TemplateDefinition(
                    "REGULAR_MEETING",
                    "정기 모임 운영",
                    "장소 확정부터 참석 조사, 공지와 정산까지 반복 업무로 만듭니다.",
                    "event_repeat",
                    List.of("TODO", "SCHEDULE_MANAGE"),
                    List.of("일정과 장소 확정", "참석 응답 요청", "준비물·안건 공지", "당일 출석 확인", "비용 정산과 회고"),
                    7,
                    "WEEKLY",
                    "NORMAL"
            ),
            new TemplateDefinition(
                    "MONTHLY_FEE",
                    "월 회비 발행",
                    "청구 기준 확정부터 미납 안내와 월 마감까지 반복 체크리스트를 만듭니다.",
                    "payments",
                    List.of("TODO", "FINANCE"),
                    List.of("회비 금액과 대상 확정", "회비 청구 발행", "납부 현황 확인", "미납 멤버 리마인드", "재정 기간 마감"),
                    5,
                    "MONTHLY",
                    "HIGH"
            ),
            new TemplateDefinition(
                    "TOURNAMENT_OPERATIONS",
                    "대회 운영 체크리스트",
                    "모집, 대기열, 참가비, 코트 시간표, 체크인과 결과 확정을 한 업무로 묶습니다.",
                    "emoji_events",
                    List.of("TODO", "TOURNAMENT_RECORD"),
                    List.of("대회 요강과 모집 기간 확정", "참가 신청·대기열 검토", "참가비 납부 확인", "코트·시간표 등록", "현장 체크인", "결과·순위 확정 및 공유"),
                    14,
                    "NONE",
                    "HIGH"
            ),
            new TemplateDefinition(
                    "RECRUITMENT_ONBOARDING",
                    "신입 모집과 온보딩",
                    "모집 공고, 신청 검토, 승인 안내와 첫 활동 연결을 표준화합니다.",
                    "person_add",
                    List.of("TODO", "JOIN_REQUEST"),
                    List.of("모집 일정과 정원 확정", "모집 공고 게시", "가입 신청 검토", "승인·반려 결과 안내", "신입 안내 자료 전달", "첫 일정과 담당자 연결"),
                    21,
                    "NONE",
                    "HIGH"
            ),
            new TemplateDefinition(
                    "TERM_HANDOVER",
                    "임기 종료 인수인계",
                    "미완료 업무, 재정, 계정과 주요 결정을 다음 집행부에 넘기는 절차를 만듭니다.",
                    "handshake",
                    List.of("TODO", "HANDOVER"),
                    List.of("미완료·지연 업무 점검", "미납 회비·미처리 정산 확인", "계정·문서·자산 목록 정리", "주요 결정과 진행 배경 기록", "차기 담당자 지정", "인수인계 확인 완료"),
                    30,
                    "NONE",
                    "HIGH"
            )
    );

    private final ClubAccessResolver clubAccessResolver;
    private final ClubFeatureService clubFeatureService;
    private final ClubDashboardService clubDashboardService;
    private final ClubPositionService clubPositionService;
    private final ClubTodoService clubTodoService;
    private final ClubTodoCollaborationService clubTodoCollaborationService;
    private final FeatureCatalogRepository featureCatalogRepository;
    private final FeaturePermissionCatalogRepository featurePermissionCatalogRepository;
    private final ClubPositionRepository clubPositionRepository;

    public ClubOperationsCatalogResponse getCatalog(Long clubId, String userKey) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireAdmin(clubId, userKey);
        List<FeatureCatalog> activeCatalogs = featureCatalogRepository.findByActiveTrueOrderBySortOrderAscFeatureKeyAsc();
        Set<String> availableFeatureKeys = activeCatalogs.stream()
                .map(FeatureCatalog::getFeatureKey)
                .collect(Collectors.toSet());
        Map<String, String> displayNameByFeatureKey = activeCatalogs.stream()
                .collect(Collectors.toMap(FeatureCatalog::getFeatureKey, FeatureCatalog::getDisplayName));
        Set<String> enabledFeatureKeys = clubFeatureService.getEnabledFeatureKeys(clubId);

        return new ClubOperationsCatalogResponse(
                clubId,
                access.club().getName(),
                PRESETS.stream()
                        .map(preset -> toPresetResponse(
                                preset,
                                availableFeatureKeys,
                                displayNameByFeatureKey,
                                enabledFeatureKeys
                        ))
                        .toList(),
                TEMPLATES.stream()
                        .map(template -> toTemplateResponse(clubId, template))
                        .toList()
        );
    }

    @Transactional(transactionManager = "pubTransactionManager")
    public ApplyClubPresetResponse applyPreset(
            Long clubId,
            String presetKey,
            String userKey,
            ApplyClubPresetRequest request
    ) {
        clubAccessResolver.requireAdmin(clubId, userKey);
        PresetDefinition preset = requirePreset(presetKey);
        String applyMode = normalizeApplyMode(request == null ? null : request.applyMode());
        Set<String> availableFeatureKeys = featureCatalogRepository.findByActiveTrueOrderBySortOrderAscFeatureKeyAsc()
                .stream()
                .map(FeatureCatalog::getFeatureKey)
                .collect(Collectors.toSet());
        List<String> presetFeatureKeys = preset.featureKeys().stream()
                .filter(availableFeatureKeys::contains)
                .toList();
        List<String> enabledFeatureKeys = new ArrayList<>(presetFeatureKeys);
        if (APPLY_MODE_MERGE.equals(applyMode)) {
            clubFeatureService.getClubFeatures(clubId, userKey).stream()
                    .filter(ClubFeatureResponse::enabled)
                    .map(ClubFeatureResponse::featureKey)
                    .filter(featureKey -> !enabledFeatureKeys.contains(featureKey))
                    .forEach(enabledFeatureKeys::add);
        }
        List<ClubFeatureResponse> features = clubFeatureService.updateClubFeatures(
                clubId,
                userKey,
                new UpdateClubFeaturesRequest(enabledFeatureKeys)
        );
        List<String> enabledWidgetKeys = applyWidgetPreset(clubId, userKey, preset.recommendedWidgetKeys());
        List<String> createdPositionNames = createMissingDelegatedPositions(clubId, userKey, preset.positions());
        return new ApplyClubPresetResponse(
                preset.presetKey(),
                preset.displayName(),
                applyMode,
                enabledFeatureKeys,
                enabledWidgetKeys,
                createdPositionNames,
                features
        );
    }

    @Transactional(transactionManager = "pubTransactionManager")
    public ApplyClubOperationTemplateResponse applyTemplate(
            Long clubId,
            String templateKey,
            String userKey,
            ApplyClubOperationTemplateRequest request
    ) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireAdmin(clubId, userKey);
        TemplateDefinition template = requireTemplate(templateKey);
        Set<String> enabledFeatureKeys = clubFeatureService.getEnabledFeatureKeys(clubId);
        List<String> missingFeatureKeys = template.requiredFeatureKeys().stream()
                .filter(featureKey -> !enabledFeatureKeys.contains(featureKey))
                .toList();
        if (!missingFeatureKeys.isEmpty()) {
            throw new SemoException.ValidationException(
                    "템플릿 적용에 필요한 기능을 먼저 활성화하세요: " + String.join(", ", missingFeatureKeys)
            );
        }

        String title = normalizeTitle(request == null ? null : request.titleOverride(), template.displayName());
        LocalDateTime dueAt = resolveDueAt(request == null ? null : request.dueAt(), template.defaultDueDays());
        TodoSummaryResponse todo = clubTodoService.createTodo(
                clubId,
                userKey,
                new CreateClubTodoRequest(
                        title,
                        template.description() + "\n운영 템플릿: " + template.displayName(),
                        "OPERATIONS",
                        "DIRECT_ASSIGN",
                        access.clubProfile().getClubProfileId(),
                        dueAt.toString(),
                        null,
                        template.priorityCode(),
                        1,
                        null,
                        null,
                        null,
                        null,
                        template.recurrenceFrequency(),
                        1,
                        null
                )
        );
        template.checklistItems().forEach(content -> clubTodoCollaborationService.addChecklistItem(
                clubId,
                todo.todoItemId(),
                userKey,
                new CreateTodoChecklistItemRequest(content)
        ));
        return new ApplyClubOperationTemplateResponse(
                template.templateKey(),
                template.displayName(),
                todo.todoItemId(),
                template.checklistItems().size(),
                dueAt.toString(),
                "/clubs/" + clubId + "/admin/more/todos"
        );
    }

    private ClubPresetResponse toPresetResponse(
            PresetDefinition preset,
            Set<String> availableFeatureKeys,
            Map<String, String> displayNameByFeatureKey,
            Set<String> enabledFeatureKeys
    ) {
        List<String> featureKeys = preset.featureKeys().stream().filter(availableFeatureKeys::contains).toList();
        return new ClubPresetResponse(
                preset.presetKey(),
                preset.displayName(),
                preset.description(),
                preset.iconName(),
                featureKeys,
                featureKeys.stream().map(displayNameByFeatureKey::get).toList(),
                preset.recommendedWidgetKeys(),
                preset.positions().stream().map(PositionDefinition::displayName).toList(),
                enabledFeatureKeys.containsAll(featureKeys)
        );
    }

    private ClubOperationTemplateResponse toTemplateResponse(Long clubId, TemplateDefinition template) {
        return new ClubOperationTemplateResponse(
                template.templateKey(),
                template.displayName(),
                template.description(),
                template.iconName(),
                template.requiredFeatureKeys(),
                template.checklistItems(),
                template.defaultDueDays(),
                template.recurrenceFrequency(),
                "/clubs/" + clubId + "/admin/more/todos"
        );
    }

    private List<String> applyWidgetPreset(Long clubId, String userKey, List<String> recommendedWidgetKeys) {
        ClubDashboardEditorResponse editor = clubDashboardService.getDashboardWidgetEditor(
                clubId,
                userKey,
                ClubDashboardService.SCOPE_USER_HOME
        );
        Set<String> availableRecommendedKeys = editor.widgets().stream()
                .filter(ClubDashboardWidgetResponse::available)
                .map(ClubDashboardWidgetResponse::widgetKey)
                .filter(recommendedWidgetKeys::contains)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<String, Integer> recommendedSortOrder = recommendedWidgetKeys.stream()
                .filter(availableRecommendedKeys::contains)
                .collect(Collectors.toMap(
                        Function.identity(),
                        key -> (recommendedWidgetKeys.indexOf(key) + 1) * 10
                ));
        List<UpdateClubDashboardWidgetItemRequest> widgets = editor.widgets().stream()
                .map(widget -> new UpdateClubDashboardWidgetItemRequest(
                        widget.widgetKey(),
                        widget.enabled() || availableRecommendedKeys.contains(widget.widgetKey()),
                        recommendedSortOrder.getOrDefault(widget.widgetKey(), 1000 + widget.sortOrder()),
                        widget.columnSpan(),
                        widget.rowSpan(),
                        null
                ))
                .toList();
        clubDashboardService.updateDashboardWidgetLayout(
                clubId,
                userKey,
                new UpdateClubDashboardLayoutRequest(ClubDashboardService.SCOPE_USER_HOME, widgets)
        );
        return List.copyOf(availableRecommendedKeys);
    }

    private List<String> createMissingDelegatedPositions(
            Long clubId,
            String userKey,
            List<PositionDefinition> positions
    ) {
        Set<String> availablePermissionKeys = featurePermissionCatalogRepository
                .findByActiveTrueOrderByFeatureKeyAscSortOrderAscPermissionKeyAsc()
                .stream()
                .map(permission -> permission.getPermissionKey())
                .collect(Collectors.toSet());
        Set<String> existingPositionCodes = clubPositionRepository
                .findByClubIdOrderByDisplayNameAscClubPositionIdAsc(clubId)
                .stream()
                .map(position -> position.getPositionCode())
                .collect(Collectors.toSet());
        List<String> createdPositionNames = new ArrayList<>();
        for (PositionDefinition position : positions) {
            if (existingPositionCodes.contains(position.positionCode())) {
                continue;
            }
            List<String> permissionKeys = position.permissionKeys().stream()
                    .filter(availablePermissionKeys::contains)
                    .toList();
            clubPositionService.createPosition(
                    clubId,
                    userKey,
                    new CreateClubPositionRequest(
                            position.displayName(),
                            position.positionCode(),
                            position.description(),
                            position.iconName(),
                            position.colorHex(),
                            permissionKeys
                    )
            );
            createdPositionNames.add(position.displayName());
            existingPositionCodes.add(position.positionCode());
        }
        return createdPositionNames;
    }

    private PresetDefinition requirePreset(String presetKey) {
        String normalized = normalizeKey(presetKey);
        return PRESETS.stream()
                .filter(preset -> preset.presetKey().equals(normalized))
                .findFirst()
                .orElseThrow(() -> new SemoException.ResourceNotFoundException("ClubPreset", "presetKey", presetKey));
    }

    private TemplateDefinition requireTemplate(String templateKey) {
        String normalized = normalizeKey(templateKey);
        return TEMPLATES.stream()
                .filter(template -> template.templateKey().equals(normalized))
                .findFirst()
                .orElseThrow(() -> new SemoException.ResourceNotFoundException(
                        "ClubOperationTemplate",
                        "templateKey",
                        templateKey
                ));
    }

    private String normalizeApplyMode(String applyMode) {
        String normalized = StringUtils.hasText(applyMode)
                ? applyMode.trim().toUpperCase(Locale.ROOT)
                : APPLY_MODE_MERGE;
        if (!Set.of(APPLY_MODE_MERGE, APPLY_MODE_REPLACE).contains(normalized)) {
            throw new SemoException.ValidationException("프리셋 적용 방식은 MERGE 또는 REPLACE만 가능합니다.");
        }
        return normalized;
    }

    private String normalizeKey(String key) {
        return key == null ? "" : key.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeTitle(String titleOverride, String fallback) {
        if (!StringUtils.hasText(titleOverride)) {
            return fallback;
        }
        String normalized = titleOverride.trim();
        if (normalized.length() > 150) {
            throw new SemoException.ValidationException("템플릿 업무 제목은 150자 이하여야 합니다.");
        }
        return normalized;
    }

    private LocalDateTime resolveDueAt(String value, int defaultDueDays) {
        if (!StringUtils.hasText(value)) {
            return LocalDateTime.now().plusDays(defaultDueDays).withHour(18).withMinute(0).withSecond(0).withNano(0);
        }
        try {
            LocalDateTime dueAt = LocalDateTime.parse(value.trim());
            if (dueAt.isBefore(LocalDateTime.now())) {
                throw new SemoException.ValidationException("템플릿 업무 마감일은 현재 이후여야 합니다.");
            }
            return dueAt;
        } catch (DateTimeParseException exception) {
            throw new SemoException.ValidationException("템플릿 업무 마감일 형식이 잘못되었습니다.");
        }
    }

    private static PositionDefinition treasurerPosition() {
        return new PositionDefinition(
                "TREASURER",
                "회계",
                "회비 발행, 수납, 지출, 정산과 기간 마감을 담당합니다.",
                "account_balance_wallet",
                "#D97706",
                List.of(
                        "FINANCE_VIEW", "FINANCE_BILLING_ISSUE", "FINANCE_REQUEST_REVIEW",
                        "FINANCE_EXPENSE_CREATE", "FINANCE_PAYMENT_UPDATE", "FINANCE_EXPORT",
                        "FINANCE_PERIOD_CLOSE"
                )
        );
    }

    private record PresetDefinition(
            String presetKey,
            String displayName,
            String description,
            String iconName,
            List<String> featureKeys,
            List<String> recommendedWidgetKeys,
            List<PositionDefinition> positions
    ) {
    }

    private record PositionDefinition(
            String positionCode,
            String displayName,
            String description,
            String iconName,
            String colorHex,
            List<String> permissionKeys
    ) {
    }

    private record TemplateDefinition(
            String templateKey,
            String displayName,
            String description,
            String iconName,
            List<String> requiredFeatureKeys,
            List<String> checklistItems,
            int defaultDueDays,
            String recurrenceFrequency,
            String priorityCode
    ) {
    }
}
