package semo.back.service.feature.position.biz;

import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ClubPositionAccessPolicy {
    public static final String ACCESS_NONE = "NONE";
    public static final String ACCESS_VIEWER = "VIEWER";
    public static final String ACCESS_OPERATOR = "OPERATOR";
    public static final String ACCESS_MANAGER = "MANAGER";

    private static final int INITIAL_POLICY_VERSION = 1;

    private static final Map<String, FeatureAccessPolicy> FEATURE_POLICIES = Map.ofEntries(
            Map.entry("NOTICE", policy(
                    level(
                            ACCESS_OPERATOR,
                            "콘텐츠 담당",
                            "공지를 작성하고 본인이 작성한 공지를 수정·삭제합니다.",
                            ClubCapability.NOTICE_CREATE,
                            ClubCapability.NOTICE_UPDATE_SELF,
                            ClubCapability.NOTICE_DELETE_SELF
                    )
            )),
            Map.entry("POLL", policy(
                    level(
                            ACCESS_OPERATOR,
                            "투표 담당",
                            "투표를 만들고 본인이 만든 투표를 수정·삭제합니다.",
                            ClubCapability.POLL_CREATE,
                            ClubCapability.POLL_UPDATE_SELF,
                            ClubCapability.POLL_DELETE_SELF
                    )
            )),
            Map.entry("SCHEDULE_MANAGE", policy(
                    level(
                            ACCESS_OPERATOR,
                            "일정 담당",
                            "일정을 만들고 본인이 만든 일정을 수정·삭제합니다.",
                            ClubCapability.SCHEDULE_CREATE,
                            ClubCapability.SCHEDULE_UPDATE_SELF,
                            ClubCapability.SCHEDULE_DELETE_SELF
                    )
            )),
            Map.entry("ATTENDANCE", policy(
                    level(
                            ACCESS_OPERATOR,
                            "출석 담당",
                            "일정별 실제 출석 상태와 확인 메모를 관리합니다.",
                            ClubCapability.ATTENDANCE_MANAGE
                    )
            )),
            Map.entry("TOURNAMENT_RECORD", policyWithSensitivePermissions(
                    List.of(level(
                            ACCESS_OPERATOR,
                            "대회 작성자",
                            "대회를 만들고 본인이 만든 대회를 수정·고정합니다.",
                            ClubCapability.TOURNAMENT_RECORD_CREATE,
                            ClubCapability.TOURNAMENT_RECORD_UPDATE_SELF,
                            ClubCapability.TOURNAMENT_RECORD_PIN
                    ),
                    level(
                            ACCESS_MANAGER,
                            "대회 관리자",
                            "대회 작성 권한과 승인·반려 권한을 함께 가집니다.",
                            ClubCapability.TOURNAMENT_RECORD_CREATE,
                            ClubCapability.TOURNAMENT_RECORD_UPDATE_SELF,
                            ClubCapability.TOURNAMENT_RECORD_PIN,
                            ClubCapability.TOURNAMENT_RECORD_REVIEW
                    )),
                    ClubCapability.TOURNAMENT_RECORD_DELETE_ANY
            )),
            Map.entry("BRACKET", policyWithSensitivePermissions(
                    List.of(
                            level(
                                    ACCESS_OPERATOR,
                                    "대진표 작성자",
                                    "대진표 초안을 만들고 본인이 만든 초안을 수정합니다.",
                                    ClubCapability.BRACKET_CREATE,
                                    ClubCapability.BRACKET_UPDATE_SELF
                            ),
                            level(
                                    ACCESS_MANAGER,
                                    "대진표 관리자",
                                    "대진표 작성 권한과 승인·반려 권한을 함께 가집니다.",
                                    ClubCapability.BRACKET_CREATE,
                                    ClubCapability.BRACKET_UPDATE_SELF,
                                    ClubCapability.BRACKET_REVIEW
                            )
                    ),
                    ClubCapability.BRACKET_DELETE_ANY
            )),
            Map.entry("FINANCE", policyWithSensitivePermissions(
                    List.of(
                            level(
                                    ACCESS_VIEWER,
                                    "재정 조회자",
                                    "재정 운영 화면과 납부 현황을 조회합니다.",
                                    ClubCapability.FINANCE_VIEW
                            ),
                            level(
                                    ACCESS_OPERATOR,
                                    "재정 운영자",
                                    "재정 조회, 청구 발행, 지출 입력 업무를 수행합니다.",
                                    ClubCapability.FINANCE_VIEW,
                                    ClubCapability.FINANCE_BILLING_ISSUE,
                                    ClubCapability.FINANCE_EXPENSE_CREATE
                            )
                    ),
                    ClubCapability.FINANCE_REQUEST_REVIEW,
                    ClubCapability.FINANCE_PAYMENT_UPDATE,
                    ClubCapability.FINANCE_EXPORT,
                    ClubCapability.FINANCE_PERIOD_CLOSE
            )),
            Map.entry("TODO", policy(
                    level(
                            ACCESS_VIEWER,
                            "업무 조회자",
                            "할 일 운영 화면과 담당 현황을 조회합니다.",
                            ClubCapability.TODO_VIEW
                    ),
                    level(
                            ACCESS_OPERATOR,
                            "업무 운영자",
                            "할 일을 만들고 담당자와 진행 상태를 관리합니다.",
                            ClubCapability.TODO_VIEW,
                            ClubCapability.TODO_CREATE,
                            ClubCapability.TODO_ASSIGN,
                            ClubCapability.TODO_MANAGE_STATUS
                    ),
                    level(
                            ACCESS_MANAGER,
                            "업무 관리자",
                            "업무 운영 권한과 모든 할 일 삭제 권한을 함께 가집니다.",
                            ClubCapability.TODO_VIEW,
                            ClubCapability.TODO_CREATE,
                            ClubCapability.TODO_ASSIGN,
                            ClubCapability.TODO_MANAGE_STATUS,
                            ClubCapability.TODO_DELETE_ANY
                    )
            )),
            Map.entry("HANDOVER", policy(
                    level(
                            ACCESS_VIEWER,
                            "인수인계 조회자",
                            "인수인계 문서와 준비 현황을 조회합니다.",
                            ClubCapability.HANDOVER_VIEW
                    ),
                    level(
                            ACCESS_MANAGER,
                            "인수인계 관리자",
                            "인수인계 문서를 조회하고 작성·수정·확정합니다.",
                            ClubCapability.HANDOVER_VIEW,
                            ClubCapability.HANDOVER_MANAGE
                    )
            )),
            Map.entry("DECISION_LOG", policy(
                    level(
                            ACCESS_VIEWER,
                            "결정 기록 조회자",
                            "운영 결정 기록을 조회합니다.",
                            ClubCapability.DECISION_VIEW
                    ),
                    level(
                            ACCESS_MANAGER,
                            "결정 기록 관리자",
                            "운영 결정 기록을 조회하고 작성·수정합니다.",
                            ClubCapability.DECISION_VIEW,
                            ClubCapability.DECISION_MANAGE
                    )
            ))
    );

    private static final List<PositionTemplate> POSITION_TEMPLATES = List.of(
            template(
                    "CONTENT_COORDINATOR",
                    "콘텐츠 담당",
                    "공지, 투표, 일정을 일관되게 운영합니다.",
                    "campaign",
                    "#904e00",
                    grant("NOTICE", ACCESS_OPERATOR),
                    grant("POLL", ACCESS_OPERATOR),
                    grant("SCHEDULE_MANAGE", ACCESS_OPERATOR)
            ),
            template(
                    "EVENT_COORDINATOR",
                    "행사 담당",
                    "일정, 출석, 대회와 대진표의 실무를 맡습니다.",
                    "event_available",
                    "#0053dd",
                    grant("SCHEDULE_MANAGE", ACCESS_OPERATOR),
                    grant("ATTENDANCE", ACCESS_OPERATOR),
                    grant("TOURNAMENT_RECORD", ACCESS_OPERATOR),
                    grant("BRACKET", ACCESS_OPERATOR)
            ),
            template(
                    "TREASURER",
                    "재정 담당",
                    "청구와 지출 입력을 담당하며 승인·마감 권한은 별도로 둡니다.",
                    "payments",
                    "#15803d",
                    grant("FINANCE", ACCESS_OPERATOR)
            ),
            template(
                    "OPERATIONS_LEAD",
                    "운영 책임자",
                    "업무, 인수인계와 결정 기록을 관리합니다.",
                    "admin_panel_settings",
                    "#7c3aed",
                    grant("TODO", ACCESS_MANAGER),
                    grant("HANDOVER", ACCESS_MANAGER),
                    grant("DECISION_LOG", ACCESS_MANAGER)
            )
    );

    public ClubPositionAccessPolicy() {
        validatePolicies();
    }

    public Optional<FeatureAccessPolicy> findPolicy(String featureKey) {
        return Optional.ofNullable(FEATURE_POLICIES.get(featureKey));
    }

    public List<PositionTemplate> positionTemplates() {
        return POSITION_TEMPLATES;
    }

    public Set<ClubCapability> supportedCapabilities() {
        return FEATURE_POLICIES.values().stream()
                .flatMap(policy -> policy.supportedCapabilities().stream())
                .collect(Collectors.toUnmodifiableSet());
    }

    private void validatePolicies() {
        FEATURE_POLICIES.forEach((featureKey, policy) -> {
            if (policy.policyVersion() < 1) {
                throw new IllegalStateException("직책 권한 정책 버전은 1 이상이어야 합니다: " + featureKey);
            }
            Set<String> levelKeys = new java.util.HashSet<>();
            Set<ClubCapability> previousCapabilities = Set.of();
            for (AccessLevel level : policy.accessLevels()) {
                if (!levelKeys.add(level.accessLevel())) {
                    throw new IllegalStateException("직책 권한 수준이 중복되었습니다: " + featureKey);
                }
                if (!level.capabilities().stream().allMatch(capability -> featureKey.equals(capability.featureKey()))) {
                    throw new IllegalStateException("다른 기능의 capability가 권한 정책에 포함되었습니다: " + featureKey);
                }
                if (!level.capabilities().containsAll(previousCapabilities)) {
                    throw new IllegalStateException("직책 권한 수준은 이전 수준의 capability를 포함해야 합니다: " + featureKey);
                }
                previousCapabilities = level.capabilities();
            }
            if (!levelKeys.contains(ACCESS_NONE)) {
                throw new IllegalStateException("권한 없음 수준이 누락되었습니다: " + featureKey);
            }
            if (!policy.sensitiveCapabilities().stream()
                    .allMatch(capability -> featureKey.equals(capability.featureKey()))) {
                throw new IllegalStateException("다른 기능의 민감 capability가 포함되었습니다: " + featureKey);
            }
            Set<ClubCapability> regularCapabilities = policy.accessLevels().stream()
                    .flatMap(level -> level.capabilities().stream())
                    .collect(Collectors.toSet());
            if (policy.sensitiveCapabilities().stream().anyMatch(regularCapabilities::contains)) {
                throw new IllegalStateException("민감 capability는 기본 운영 수준에 포함할 수 없습니다: " + featureKey);
            }
        });

        for (PositionTemplate template : POSITION_TEMPLATES) {
            Set<String> featureKeys = new java.util.HashSet<>();
            for (TemplateGrant grant : template.grants()) {
                if (!featureKeys.add(grant.featureKey())) {
                    throw new IllegalStateException("직책 템플릿 기능이 중복되었습니다: " + template.templateKey());
                }
                FeatureAccessPolicy policy = FEATURE_POLICIES.get(grant.featureKey());
                boolean supportedLevel = policy != null && policy.accessLevels().stream()
                        .anyMatch(level -> level.accessLevel().equals(grant.accessLevel())
                                && !ACCESS_NONE.equals(level.accessLevel()));
                if (!supportedLevel) {
                    throw new IllegalStateException("직책 템플릿에 지원하지 않는 운영 수준이 있습니다: " + template.templateKey());
                }
            }
        }
    }

    private static FeatureAccessPolicy policy(AccessLevel... accessLevels) {
        return policyWithSensitivePermissions(List.of(accessLevels));
    }

    private static FeatureAccessPolicy policyWithSensitivePermissions(
            List<AccessLevel> accessLevels,
            ClubCapability... sensitiveCapabilities
    ) {
        List<AccessLevel> levelsWithNone = new java.util.ArrayList<>();
        levelsWithNone.add(new AccessLevel(
                ACCESS_NONE,
                "권한 없음",
                "이 기능의 운영 권한을 위임하지 않습니다.",
                Set.of()
        ));
        levelsWithNone.addAll(accessLevels);
        return new FeatureAccessPolicy(INITIAL_POLICY_VERSION, levelsWithNone, List.of(sensitiveCapabilities));
    }

    private static AccessLevel level(
            String accessLevel,
            String displayName,
            String description,
            ClubCapability... capabilities
    ) {
        return new AccessLevel(accessLevel, displayName, description, Set.of(capabilities));
    }

    private static PositionTemplate template(
            String templateKey,
            String displayName,
            String description,
            String iconName,
            String colorHex,
            TemplateGrant... grants
    ) {
        return new PositionTemplate(
                templateKey,
                displayName,
                description,
                iconName,
                colorHex,
                List.of(grants)
        );
    }

    private static TemplateGrant grant(String featureKey, String accessLevel) {
        return new TemplateGrant(featureKey, accessLevel);
    }

    public record FeatureAccessPolicy(
            int policyVersion,
            List<AccessLevel> accessLevels,
            List<ClubCapability> sensitiveCapabilities
    ) {
        public FeatureAccessPolicy {
            accessLevels = List.copyOf(accessLevels);
            sensitiveCapabilities = List.copyOf(sensitiveCapabilities);
        }

        public Set<ClubCapability> supportedCapabilities() {
            Set<ClubCapability> capabilities = new LinkedHashSet<>();
            accessLevels.stream()
                    .flatMap(accessLevel -> accessLevel.capabilities().stream())
                    .forEach(capabilities::add);
            capabilities.addAll(sensitiveCapabilities);
            return Set.copyOf(capabilities);
        }

        public Set<String> supportedPermissionKeys() {
            return supportedCapabilities().stream()
                    .map(ClubCapability::permissionKey)
                    .collect(Collectors.toUnmodifiableSet());
        }

        public List<String> sensitivePermissionKeys() {
            return sensitiveCapabilities.stream()
                    .map(ClubCapability::permissionKey)
                    .toList();
        }
    }

    public record AccessLevel(
            String accessLevel,
            String displayName,
            String description,
            Set<ClubCapability> capabilities
    ) {
        public AccessLevel {
            capabilities = Set.copyOf(capabilities);
        }

        public List<String> permissionKeys() {
            return capabilities.stream()
                    .map(ClubCapability::permissionKey)
                    .sorted()
                    .toList();
        }
    }

    public record PositionTemplate(
            String templateKey,
            String displayName,
            String description,
            String iconName,
            String colorHex,
            List<TemplateGrant> grants
    ) {
        public PositionTemplate {
            grants = List.copyOf(grants);
        }
    }

    public record TemplateGrant(String featureKey, String accessLevel) {
    }
}
