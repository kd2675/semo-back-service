package semo.back.service.feature.clubfeature.biz;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import semo.back.service.common.exception.SemoException;
import semo.back.service.database.pub.entity.ClubFeature;
import semo.back.service.database.pub.entity.FeatureCatalog;
import semo.back.service.database.pub.repository.ClubFeatureRepository;
import semo.back.service.database.pub.repository.FeatureCatalogRepository;
import semo.back.service.feature.club.biz.ClubAccessResolver;
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
    private static final List<FeatureCatalog> DEFAULT_CATALOGS = List.of(
            FeatureCatalog.builder()
                    .featureKey("ATTENDANCE")
                    .displayName("출석 체크")
                    .description("멤버 출석을 체크하고 출석 세션을 관리합니다.")
                    .iconName("fact_check")
                    .active(true)
                    .sortOrder(10)
                    .build(),
            FeatureCatalog.builder()
                    .featureKey("TIMELINE")
                    .displayName("타임라인")
                    .description("공지 기반 타임라인 카드로 모임 활동을 확인합니다.")
                    .iconName("timeline")
                    .active(true)
                    .sortOrder(20)
                    .build(),
            FeatureCatalog.builder()
                    .featureKey("NOTICE")
                    .displayName("공지")
                    .description("모임 공지를 작성, 관리, 공유합니다.")
                    .iconName("campaign")
                    .active(true)
                    .sortOrder(30)
                    .build(),
            FeatureCatalog.builder()
                    .featureKey("POLL")
                    .displayName("투표")
                    .description("모임 투표를 작성, 공유, 관리합니다.")
                    .iconName("poll")
                    .active(true)
                    .sortOrder(40)
                    .build(),
            FeatureCatalog.builder()
                    .featureKey("SCHEDULE_MANAGE")
                    .displayName("일정 관리")
                    .description("일정과 투표를 작성하고 관리합니다.")
                    .iconName("edit_calendar")
                    .active(true)
                    .sortOrder(50)
                    .build()
    );

    private final FeatureCatalogRepository featureCatalogRepository;
    private final ClubFeatureRepository clubFeatureRepository;
    private final ClubAccessResolver clubAccessResolver;
    private final ClubDashboardService clubDashboardService;

    @Transactional(transactionManager = "pubTransactionManager")
    public List<ClubFeatureResponse> getClubFeatures(Long clubId, String userKey) {
        clubAccessResolver.requireActiveMember(clubId, userKey);
        return getClubFeatureResponses(clubId);
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    public List<ClubFeatureResponse> updateClubFeatures(Long clubId, String userKey, UpdateClubFeaturesRequest request) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireAdmin(clubId, userKey);
        ensureDefaultCatalog();
        List<FeatureCatalog> catalogs = featureCatalogRepository.findByActiveTrueOrderBySortOrderAscFeatureKeyAsc();
        Set<String> allowedFeatureKeys = catalogs.stream()
                .map(FeatureCatalog::getFeatureKey)
                .collect(Collectors.toSet());
        List<String> enabledFeatureKeysInOrder = normalizeEnabledFeatureKeysInOrder(request, allowedFeatureKeys);
        Set<String> enabledFeatureKeys = Set.copyOf(enabledFeatureKeysInOrder);
        Map<String, Integer> enabledSortOrderByKey = toEnabledSortOrderByKey(enabledFeatureKeysInOrder);
        Map<String, ClubFeature> existingByKey = clubFeatureRepository.findByClubId(clubId).stream()
                .collect(Collectors.toMap(ClubFeature::getFeatureKey, Function.identity()));
        LocalDateTime now = LocalDateTime.now();

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

        return getClubFeatureResponses(clubId);
    }

    public boolean isFeatureEnabled(Long clubId, String featureKey) {
        ensureDefaultCatalog();
        String normalizedFeatureKey = normalizeFeatureKey(featureKey);
        return clubFeatureRepository.findByClubIdAndFeatureKey(clubId, normalizedFeatureKey)
                .map(ClubFeature::isEnabled)
                .orElse(false);
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

    private int resolveResponseSortOrder(FeatureCatalog catalog, ClubFeature clubFeature) {
        if (clubFeature == null || clubFeature.getSortOrder() == null || clubFeature.getSortOrder() <= 0) {
            return 1000 + catalog.getSortOrder();
        }
        return clubFeature.getSortOrder();
    }

    private List<ClubFeatureResponse> getClubFeatureResponses(Long clubId) {
        ensureDefaultCatalog();
        List<FeatureCatalog> catalogs = featureCatalogRepository.findByActiveTrueOrderBySortOrderAscFeatureKeyAsc();
        Map<String, ClubFeature> clubFeaturesByKey = clubFeatureRepository.findByClubId(clubId).stream()
                .collect(Collectors.toMap(ClubFeature::getFeatureKey, Function.identity()));

        return catalogs.stream()
                .sorted(
                        Comparator
                                .comparingInt((FeatureCatalog catalog) ->
                                        resolveResponseSortOrder(catalog, clubFeaturesByKey.get(catalog.getFeatureKey()))
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
                            clubFeature != null && clubFeature.isEnabled(),
                            toUserPath(clubId, catalog.getFeatureKey()),
                            toAdminPath(clubId, catalog.getFeatureKey())
                    );
                })
                .toList();
    }

    private String toUserPath(Long clubId, String featureKey) {
        return switch (normalizeFeatureKey(featureKey)) {
            case "ATTENDANCE" -> "/clubs/%d/more/attendance".formatted(clubId);
            case "TIMELINE" -> "/clubs/%d/more/timeline".formatted(clubId);
            case "NOTICE" -> "/clubs/%d/more/notices".formatted(clubId);
            case "POLL" -> "/clubs/%d/more/polls".formatted(clubId);
            case "SCHEDULE_MANAGE" -> "/clubs/%d/more/schedules".formatted(clubId);
            default -> "/clubs/%d".formatted(clubId);
        };
    }

    private String toAdminPath(Long clubId, String featureKey) {
        return switch (normalizeFeatureKey(featureKey)) {
            case "ATTENDANCE" -> "/clubs/%d/admin/more/attendance".formatted(clubId);
            case "TIMELINE" -> "/clubs/%d/admin/more/timeline".formatted(clubId);
            case "NOTICE" -> "/clubs/%d/admin/more/notices".formatted(clubId);
            case "POLL" -> "/clubs/%d/admin/more/polls".formatted(clubId);
            case "SCHEDULE_MANAGE" -> "/clubs/%d/admin/more/schedules".formatted(clubId);
            default -> "/clubs/%d/admin".formatted(clubId);
        };
    }

    private String normalizeFeatureKey(String featureKey) {
        if (featureKey == null) {
            return "";
        }
        return featureKey.trim().toUpperCase(Locale.ROOT);
    }

    private void ensureDefaultCatalog() {
        Set<String> existingKeys = featureCatalogRepository.findAll().stream()
                .map(FeatureCatalog::getFeatureKey)
                .collect(Collectors.toSet());

        DEFAULT_CATALOGS.stream()
                .filter(catalog -> !existingKeys.contains(catalog.getFeatureKey()))
                .forEach(featureCatalogRepository::save);
    }
}
