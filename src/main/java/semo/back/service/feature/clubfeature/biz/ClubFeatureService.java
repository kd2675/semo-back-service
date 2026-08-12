package semo.back.service.feature.clubfeature.biz;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import semo.back.service.common.exception.SemoException;
import semo.back.service.database.pub.entity.Club;
import semo.back.service.database.pub.entity.ClubFeature;
import semo.back.service.database.pub.entity.FeatureCatalog;
import semo.back.service.database.pub.repository.ClubFeatureRepository;
import semo.back.service.database.pub.repository.ClubRepository;
import semo.back.service.database.pub.repository.FeatureCatalogRepository;
import semo.back.service.feature.activity.biz.ClubActivityContextHolder;
import semo.back.service.feature.activity.biz.RecordClubActivity;
import semo.back.service.feature.club.biz.policy.ClubAccessResolver;
import semo.back.service.feature.clubfeature.vo.ClubFeatureResponse;
import semo.back.service.feature.clubfeature.vo.UpdateClubFeaturesRequest;
import semo.back.service.feature.dashboard.biz.ClubDashboardService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubFeatureService {
    private static final String NAVIGATION_SCOPE_USER_AND_ADMIN = "USER_AND_ADMIN";
    private static final String NAVIGATION_SCOPE_ADMIN_ONLY = "ADMIN_ONLY";
    private static final String FEATURE_JOIN_REQUEST = "JOIN_REQUEST";
    private static final String FEATURE_SCHEDULE_MANAGE = "SCHEDULE_MANAGE";
    private static final String FEATURE_ATTENDANCE = "ATTENDANCE";
    private static final String FEATURE_ROLE_MANAGEMENT = "ROLE_MANAGEMENT";
    private static final String FEATURE_HANDOVER = "HANDOVER";
    private static final String MEMBERSHIP_POLICY_APPROVAL = "APPROVAL";

    private final FeatureCatalogRepository featureCatalogRepository;
    private final ClubFeatureRepository clubFeatureRepository;
    private final ClubRepository clubRepository;
    private final ClubAccessResolver clubAccessResolver;
    private final ClubDashboardService clubDashboardService;

    @Transactional(transactionManager = "pubTransactionManager")
    public List<ClubFeatureResponse> getClubFeatures(Long clubId, String userKey) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        return getClubFeatureResponses(access.club());
    }

    @Transactional(transactionManager = "pubTransactionManager")
    @RecordClubActivity(subject = "기능관리", failureDetail = "활성 기능 구성을 업데이트하지 못했습니다.")
    public List<ClubFeatureResponse> updateClubFeatures(Long clubId, String userKey, UpdateClubFeaturesRequest request) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireAdmin(clubId, userKey);
        List<FeatureCatalog> catalogs = featureCatalogRepository.findByActiveTrueOrderBySortOrderAscFeatureKeyAsc();
        Set<String> allowedFeatureKeys = catalogs.stream()
                .map(FeatureCatalog::getFeatureKey)
                .collect(Collectors.toSet());
        List<String> enabledFeatureKeysInOrder = includeRequiredFeatures(
                access.club(),
                normalizeEnabledFeatureKeysInOrder(request, allowedFeatureKeys)
        );
        Set<String> enabledFeatureKeys = Set.copyOf(enabledFeatureKeysInOrder);
        Map<String, Integer> enabledSortOrderByKey = toEnabledSortOrderByKey(enabledFeatureKeysInOrder);
        if (isApprovalClub(access.club())) {
            enabledSortOrderByKey.put(FEATURE_JOIN_REQUEST, 5);
        }
        Map<String, ClubFeature> existingByKey = clubFeatureRepository.findByClubId(clubId).stream()
                .collect(Collectors.toMap(ClubFeature::getFeatureKey, Function.identity()));
        LocalDateTime now = LocalDateTime.now();
        ClubActivityContextHolder.setDetails(
                buildFeatureUpdateDetail(catalogs, enabledFeatureKeys),
                "활성 기능 구성을 업데이트하지 못했습니다."
        );

        for (FeatureCatalog catalog : catalogs) {
            boolean enabled = enabledFeatureKeys.contains(catalog.getFeatureKey());
            int sortOrder = resolveSortOrder(catalog, enabled, enabledSortOrderByKey);
            ClubFeature existing = existingByKey.get(catalog.getFeatureKey());
            if (existing == null) {
                clubFeatureRepository.save(ClubFeature.builder()
                        .clubId(clubId)
                        .featureKey(catalog.getFeatureKey())
                        .enabled(enabled)
                        .sortOrder(sortOrder)
                        .enabledByClubProfileId(enabled ? access.clubProfile().getClubProfileId() : null)
                        .enabledAt(enabled ? now : null)
                        .build());
                continue;
            }

            clubFeatureRepository.save(ClubFeature.builder()
                    .clubFeatureId(existing.getClubFeatureId())
                    .clubId(existing.getClubId())
                    .featureKey(existing.getFeatureKey())
                    .enabled(enabled)
                    .sortOrder(sortOrder)
                    .enabledByClubProfileId(enabled ? access.clubProfile().getClubProfileId() : null)
                    .enabledAt(enabled ? now : null)
                    .build());
        }
        clubDashboardService.syncWidgetsForClub(clubId);

        return getClubFeatureResponses(access.club());
    }

    public boolean isFeatureEnabled(Long clubId, String featureKey) {
        String normalizedFeatureKey = normalizeFeatureKey(featureKey);
        if (FEATURE_ROLE_MANAGEMENT.equals(normalizedFeatureKey)) {
            return true;
        }
        if (FEATURE_JOIN_REQUEST.equals(normalizedFeatureKey)) {
            return isApprovalClub(clubId);
        }
        boolean explicitlyEnabled = clubFeatureRepository.findByClubIdAndFeatureKey(clubId, normalizedFeatureKey)
                .map(ClubFeature::isEnabled)
                .orElse(false);
        return explicitlyEnabled && requiredFeatureKeys(normalizedFeatureKey).stream()
                .allMatch(requiredFeatureKey -> isFeatureEnabled(clubId, requiredFeatureKey));
    }

    public Set<String> getEnabledFeatureKeys(Long clubId) {
        Club club = clubRepository.findById(clubId).orElse(null);
        Map<String, ClubFeature> clubFeaturesByKey = clubFeatureRepository.findByClubId(clubId).stream()
                .collect(Collectors.toMap(ClubFeature::getFeatureKey, Function.identity()));
        return featureCatalogRepository.findByActiveTrueOrderBySortOrderAscFeatureKeyAsc().stream()
                .map(FeatureCatalog::getFeatureKey)
                .filter(featureKey -> resolveEnabled(featureKey, clubFeaturesByKey, club, new HashSet<>()))
                .collect(Collectors.toUnmodifiableSet());
    }

    public void requireFeatureEnabled(Long clubId, String featureKey, String displayName) {
        if (!isFeatureEnabled(clubId, featureKey)) {
            throw new SemoException.ForbiddenException(displayName + " 기능이 활성화되지 않았습니다.");
        }
    }

    private List<String> normalizeEnabledFeatureKeysInOrder(
            UpdateClubFeaturesRequest request,
            Set<String> allowedFeatureKeys
    ) {
        if (request == null || request.enabledFeatureKeys() == null) {
            return List.of();
        }

        List<String> normalizedKeys = request.enabledFeatureKeys().stream()
                .map(this::normalizeFeatureKey)
                .toList();

        if (!allowedFeatureKeys.containsAll(Set.copyOf(normalizedKeys))) {
            throw new SemoException.ValidationException("지원하지 않는 기능 키가 포함되어 있습니다.");
        }

        List<String> deduplicated = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String featureKey : normalizedKeys) {
            if (seen.add(featureKey)) {
                deduplicated.add(featureKey);
            }
        }
        return deduplicated;
    }

    private List<String> includeRequiredFeatures(Club club, List<String> requestedFeatureKeys) {
        List<String> normalized = new ArrayList<>(requestedFeatureKeys);
        insertBefore(normalized, FEATURE_ROLE_MANAGEMENT, null);
        if (isApprovalClub(club)) {
            insertBefore(normalized, FEATURE_JOIN_REQUEST, null);
        } else {
            normalized.remove(FEATURE_JOIN_REQUEST);
        }
        insertRequirementBeforeDependent(normalized, FEATURE_ATTENDANCE, FEATURE_SCHEDULE_MANAGE);
        insertRequirementBeforeDependent(normalized, FEATURE_HANDOVER, FEATURE_ROLE_MANAGEMENT);
        return List.copyOf(normalized);
    }

    private void insertRequirementBeforeDependent(List<String> featureKeys, String dependent, String requirement) {
        if (!featureKeys.contains(dependent) || featureKeys.contains(requirement)) {
            return;
        }
        insertBefore(featureKeys, requirement, dependent);
    }

    private void insertBefore(List<String> featureKeys, String featureKey, String beforeFeatureKey) {
        if (featureKeys.contains(featureKey)) {
            return;
        }
        int targetIndex = beforeFeatureKey == null ? -1 : featureKeys.indexOf(beforeFeatureKey);
        featureKeys.add(targetIndex < 0 ? featureKeys.size() : targetIndex, featureKey);
    }

    private Map<String, Integer> toEnabledSortOrderByKey(List<String> enabledFeatureKeysInOrder) {
        Map<String, Integer> sortOrderByKey = new HashMap<>();
        for (int index = 0; index < enabledFeatureKeysInOrder.size(); index++) {
            sortOrderByKey.put(enabledFeatureKeysInOrder.get(index), (index + 1) * 10);
        }
        return sortOrderByKey;
    }

    private int resolveSortOrder(
            FeatureCatalog catalog,
            boolean enabled,
            Map<String, Integer> enabledSortOrderByKey
    ) {
        if (enabled) {
            return enabledSortOrderByKey.getOrDefault(catalog.getFeatureKey(), 1000 + catalog.getSortOrder());
        }
        return 1000 + catalog.getSortOrder();
    }

    private int resolveResponseSortOrder(FeatureCatalog catalog, ClubFeature clubFeature, Club club) {
        if (clubFeature == null && isMandatoryFeature(club, catalog.getFeatureKey())) {
            return catalog.getSortOrder();
        }
        if (clubFeature == null || clubFeature.getSortOrder() == null || clubFeature.getSortOrder() <= 0) {
            return 1000 + catalog.getSortOrder();
        }
        return clubFeature.getSortOrder();
    }

    private List<ClubFeatureResponse> getClubFeatureResponses(Club club) {
        Long clubId = club.getClubId();
        List<FeatureCatalog> catalogs = featureCatalogRepository.findByActiveTrueOrderBySortOrderAscFeatureKeyAsc();
        Map<String, ClubFeature> clubFeaturesByKey = clubFeatureRepository.findByClubId(clubId).stream()
                .collect(Collectors.toMap(ClubFeature::getFeatureKey, Function.identity()));

        return catalogs.stream()
                .sorted(
                        Comparator
                                .comparingInt((FeatureCatalog catalog) ->
                                        resolveResponseSortOrder(catalog, clubFeaturesByKey.get(catalog.getFeatureKey()), club)
                                )
                                .thenComparingInt(FeatureCatalog::getSortOrder)
                                .thenComparing(FeatureCatalog::getFeatureKey)
                )
                .map(catalog -> {
                    ClubFeature clubFeature = clubFeaturesByKey.get(catalog.getFeatureKey());
                    return new ClubFeatureResponse(
                            catalog.getFeatureKey(),
                            catalog.getDisplayName(),
                            catalog.getDescription(),
                            catalog.getIconName(),
                            resolveNavigationScope(catalog),
                            resolveResponseSortOrder(catalog, clubFeature, club),
                            resolveEnabled(catalog.getFeatureKey(), clubFeaturesByKey, club, new HashSet<>()),
                            toUserPath(clubId, catalog.getFeatureKey()),
                            toAdminPath(clubId, catalog.getFeatureKey()),
                            requiredFeatureKeys(catalog.getFeatureKey()),
                            isMandatoryFeature(club, catalog.getFeatureKey()),
                            mandatoryReason(club, catalog.getFeatureKey()),
                            isFeatureAvailable(club, catalog.getFeatureKey()),
                            unavailableReason(club, catalog.getFeatureKey())
                    );
                })
                .toList();
    }

    private String toUserPath(Long clubId, String featureKey) {
        return switch (normalizeFeatureKey(featureKey)) {
            case FEATURE_JOIN_REQUEST -> "/clubs/%d".formatted(clubId);
            case "ATTENDANCE" -> "/clubs/%d/schedule".formatted(clubId);
            case "NOTICE" -> "/clubs/%d/board".formatted(clubId);
            case "POLL", "SCHEDULE_MANAGE" -> "/clubs/%d/schedule".formatted(clubId);
            case "TOURNAMENT_RECORD" -> "/clubs/%d/more/tournaments".formatted(clubId);
            case "BRACKET" -> "/clubs/%d/more/brackets".formatted(clubId);
            case "FINANCE" -> "/clubs/%d/more/finance".formatted(clubId);
            case "FEEDBACK" -> "/clubs/%d/more/feedback".formatted(clubId);
            case "TODO" -> "/clubs/%d/more/todos".formatted(clubId);
            case "MEMBER_DIRECTORY" -> "/clubs/%d/more/members".formatted(clubId);
            case "ROLE_MANAGEMENT" -> "/clubs/%d/admin/more/roles".formatted(clubId);
            case FEATURE_HANDOVER -> "/clubs/%d/admin/more/handover".formatted(clubId);
            case "DECISION_LOG" -> "/clubs/%d/more/decisions".formatted(clubId);
            default -> "/clubs/%d".formatted(clubId);
        };
    }

    private String toAdminPath(Long clubId, String featureKey) {
        return switch (normalizeFeatureKey(featureKey)) {
            case FEATURE_JOIN_REQUEST -> "/clubs/%d/admin/more/join-requests".formatted(clubId);
            case "ATTENDANCE" -> "/clubs/%d/schedule".formatted(clubId);
            case "NOTICE" -> "/clubs/%d/board".formatted(clubId);
            case "POLL", "SCHEDULE_MANAGE" -> "/clubs/%d/schedule".formatted(clubId);
            case "TOURNAMENT_RECORD" -> "/clubs/%d/admin/more/tournaments".formatted(clubId);
            case "BRACKET" -> "/clubs/%d/admin/more/brackets".formatted(clubId);
            case "FINANCE" -> "/clubs/%d/admin/more/finance".formatted(clubId);
            case "FEEDBACK" -> "/clubs/%d/admin/more/feedback".formatted(clubId);
            case "TODO" -> "/clubs/%d/admin/more/todos".formatted(clubId);
            case "MEMBER_DIRECTORY" -> "/clubs/%d/admin/more/members".formatted(clubId);
            case "ROLE_MANAGEMENT" -> "/clubs/%d/admin/more/roles".formatted(clubId);
            case FEATURE_HANDOVER -> "/clubs/%d/admin/more/handover".formatted(clubId);
            case "DECISION_LOG" -> "/clubs/%d/admin/more/decisions".formatted(clubId);
            default -> "/clubs/%d/admin".formatted(clubId);
        };
    }

    private String resolveNavigationScope(FeatureCatalog catalog) {
        if (catalog != null && FEATURE_JOIN_REQUEST.equals(normalizeFeatureKey(catalog.getFeatureKey()))) {
            return NAVIGATION_SCOPE_ADMIN_ONLY;
        }
        if (catalog == null || !StringUtils.hasText(catalog.getNavigationScope())) {
            return NAVIGATION_SCOPE_USER_AND_ADMIN;
        }
        String normalized = catalog.getNavigationScope().trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case NAVIGATION_SCOPE_ADMIN_ONLY -> NAVIGATION_SCOPE_ADMIN_ONLY;
            default -> NAVIGATION_SCOPE_USER_AND_ADMIN;
        };
    }

    private String normalizeFeatureKey(String featureKey) {
        if (featureKey == null) {
            return "";
        }
        return featureKey.trim().toUpperCase(Locale.ROOT);
    }

    private boolean resolveEnabled(
            String featureKey,
            Map<String, ClubFeature> clubFeaturesByKey,
            Club club,
            Set<String> resolving
    ) {
        String normalizedFeatureKey = normalizeFeatureKey(featureKey);
        if (!isFeatureAvailable(club, normalizedFeatureKey)) {
            return false;
        }
        if (!resolving.add(normalizedFeatureKey)) {
            return false;
        }
        ClubFeature clubFeature = clubFeaturesByKey.get(normalizedFeatureKey);
        boolean configured = isMandatoryFeature(club, normalizedFeatureKey)
                || clubFeature != null && clubFeature.isEnabled();
        boolean requirementsEnabled = requiredFeatureKeys(normalizedFeatureKey).stream()
                .allMatch(requiredFeatureKey -> resolveEnabled(
                        requiredFeatureKey,
                        clubFeaturesByKey,
                        club,
                        new HashSet<>(resolving)
                ));
        return configured && requirementsEnabled;
    }

    private List<String> requiredFeatureKeys(String featureKey) {
        return switch (normalizeFeatureKey(featureKey)) {
            case FEATURE_ATTENDANCE -> List.of(FEATURE_SCHEDULE_MANAGE);
            case FEATURE_HANDOVER -> List.of(FEATURE_ROLE_MANAGEMENT);
            default -> List.of();
        };
    }

    private boolean isMandatoryFeature(Club club, String featureKey) {
        String normalizedFeatureKey = normalizeFeatureKey(featureKey);
        return FEATURE_ROLE_MANAGEMENT.equals(normalizedFeatureKey)
                || FEATURE_JOIN_REQUEST.equals(normalizedFeatureKey) && isApprovalClub(club);
    }

    private String mandatoryReason(Club club, String featureKey) {
        if (FEATURE_ROLE_MANAGEMENT.equals(normalizeFeatureKey(featureKey))) {
            return "직책과 기능 권한은 클럽 운영을 위한 기본 관리자 도구입니다.";
        }
        if (isMandatoryFeature(club, featureKey)) {
            return "가입 승인제 클럽에서 신청자를 검토하기 위한 필수 기능입니다.";
        }
        return null;
    }

    private boolean isFeatureAvailable(Club club, String featureKey) {
        return !FEATURE_JOIN_REQUEST.equals(normalizeFeatureKey(featureKey)) || isApprovalClub(club);
    }

    private String unavailableReason(Club club, String featureKey) {
        if (!isFeatureAvailable(club, featureKey)) {
            return "가입 신청은 가입 승인제 클럽에서만 사용합니다.";
        }
        return null;
    }

    private boolean isApprovalClub(Long clubId) {
        return clubRepository.findById(clubId)
                .map(this::isApprovalClub)
                .orElse(false);
    }

    private boolean isApprovalClub(Club club) {
        return club != null && MEMBERSHIP_POLICY_APPROVAL.equals(club.getMembershipPolicy());
    }

    private String buildFeatureUpdateDetail(List<FeatureCatalog> catalogs, Set<String> enabledFeatureKeys) {
        List<String> enabledDisplayNames = catalogs.stream()
                .filter(catalog -> enabledFeatureKeys.contains(catalog.getFeatureKey()))
                .map(FeatureCatalog::getDisplayName)
                .toList();
        if (enabledDisplayNames.isEmpty()) {
            return "활성 기능을 모두 비활성화했습니다.";
        }
        return "활성 기능을 " + String.join(", ", enabledDisplayNames) + "로 업데이트했습니다.";
    }
}
