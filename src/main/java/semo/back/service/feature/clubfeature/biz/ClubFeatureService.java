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

import java.time.LocalDateTime;
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
                    .displayName("Attendance Check")
                    .description("Check in members and manage attendance sessions.")
                    .iconName("fact_check")
                    .active(true)
                    .sortOrder(10)
                    .build()
    );

    private final FeatureCatalogRepository featureCatalogRepository;
    private final ClubFeatureRepository clubFeatureRepository;
    private final ClubAccessResolver clubAccessResolver;

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
        Set<String> enabledFeatureKeys = normalizeEnabledFeatureKeys(request, allowedFeatureKeys);
        Map<String, ClubFeature> existingByKey = clubFeatureRepository.findByClubId(clubId).stream()
                .collect(Collectors.toMap(ClubFeature::getFeatureKey, Function.identity()));
        LocalDateTime now = LocalDateTime.now();

        for (FeatureCatalog catalog : catalogs) {
            boolean enabled = enabledFeatureKeys.contains(catalog.getFeatureKey());
            ClubFeature existing = existingByKey.get(catalog.getFeatureKey());
            if (existing == null) {
                clubFeatureRepository.save(ClubFeature.builder()
                        .clubId(clubId)
                        .featureKey(catalog.getFeatureKey())
                        .enabled(enabled)
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
                    .enabledByClubProfileId(enabled ? access.clubProfile().getClubProfileId() : null)
                    .enabledAt(enabled ? now : null)
                    .build());
        }

        return getClubFeatureResponses(clubId);
    }

    public boolean isFeatureEnabled(Long clubId, String featureKey) {
        ensureDefaultCatalog();
        String normalizedFeatureKey = normalizeFeatureKey(featureKey);
        return clubFeatureRepository.findByClubIdAndFeatureKey(clubId, normalizedFeatureKey)
                .map(ClubFeature::isEnabled)
                .orElse(false);
    }

    private Set<String> normalizeEnabledFeatureKeys(UpdateClubFeaturesRequest request, Set<String> allowedFeatureKeys) {
        if (request == null || request.enabledFeatureKeys() == null) {
            return Set.of();
        }

        Set<String> normalizedKeys = request.enabledFeatureKeys().stream()
                .map(this::normalizeFeatureKey)
                .collect(Collectors.toSet());

        if (!allowedFeatureKeys.containsAll(normalizedKeys)) {
            throw new SemoException.ValidationException("지원하지 않는 기능 키가 포함되어 있습니다.");
        }

        return normalizedKeys;
    }

    private List<ClubFeatureResponse> getClubFeatureResponses(Long clubId) {
        ensureDefaultCatalog();
        List<FeatureCatalog> catalogs = featureCatalogRepository.findByActiveTrueOrderBySortOrderAscFeatureKeyAsc();
        Map<String, ClubFeature> clubFeaturesByKey = clubFeatureRepository.findByClubId(clubId).stream()
                .collect(Collectors.toMap(ClubFeature::getFeatureKey, Function.identity()));

        return catalogs.stream()
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
            default -> "/clubs/%d".formatted(clubId);
        };
    }

    private String toAdminPath(Long clubId, String featureKey) {
        return switch (normalizeFeatureKey(featureKey)) {
            case "ATTENDANCE" -> "/clubs/%d/admin/more/attendance".formatted(clubId);
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
