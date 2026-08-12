package semo.back.service.feature.position.biz;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import semo.back.service.common.exception.SemoException;
import semo.back.service.database.pub.entity.ClubPositionFeatureGrant;
import semo.back.service.database.pub.entity.ClubPositionPermission;
import semo.back.service.database.pub.entity.ClubPositionSensitiveGrant;
import semo.back.service.database.pub.repository.ClubPositionFeatureGrantRepository;
import semo.back.service.database.pub.repository.ClubPositionPermissionRepository;
import semo.back.service.database.pub.repository.ClubPositionSensitiveGrantRepository;
import semo.back.service.feature.position.vo.ClubPositionFeatureGrantRequest;
import semo.back.service.feature.position.vo.ClubPositionFeatureGrantResponse;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubPositionGrantService {
    public static final String STATUS_CURRENT = "CURRENT";
    public static final String STATUS_POLICY_UPDATE_AVAILABLE = "POLICY_UPDATE_AVAILABLE";
    public static final String STATUS_LEGACY_DERIVED = "LEGACY_DERIVED";
    public static final String STATUS_LEGACY_CUSTOM = "LEGACY_CUSTOM";

    private final ClubPositionAccessPolicy accessPolicy;
    private final ClubPositionFeatureGrantRepository featureGrantRepository;
    private final ClubPositionSensitiveGrantRepository sensitiveGrantRepository;
    private final ClubPositionPermissionRepository permissionRepository;

    public GrantSnapshot loadSnapshot(Collection<Long> positionIds) {
        if (positionIds == null || positionIds.isEmpty()) {
            return GrantSnapshot.empty();
        }
        List<Long> ids = positionIds.stream().filter(java.util.Objects::nonNull).distinct().toList();
        Map<Long, List<ClubPositionFeatureGrant>> featureGrants = featureGrantRepository
                .findByClubPositionIdIn(ids).stream()
                .collect(Collectors.groupingBy(ClubPositionFeatureGrant::getClubPositionId));
        Map<Long, List<ClubPositionSensitiveGrant>> sensitiveGrants = sensitiveGrantRepository
                .findByClubPositionIdIn(ids).stream()
                .collect(Collectors.groupingBy(ClubPositionSensitiveGrant::getClubPositionId));
        return new GrantSnapshot(featureGrants, sensitiveGrants);
    }

    public List<ClubPositionFeatureGrantResponse> resolveFeatureGrants(
            Long positionId,
            Collection<String> effectivePermissionKeys,
            GrantSnapshot snapshot
    ) {
        Set<ClubCapability> effectiveCapabilities = effectivePermissionKeys.stream()
                .map(ClubCapability::fromPermissionKey)
                .flatMap(java.util.Optional::stream)
                .collect(Collectors.toSet());
        Map<String, ClubPositionFeatureGrant> storedByFeature = snapshot.featureGrants(positionId).stream()
                .collect(Collectors.toMap(
                        ClubPositionFeatureGrant::getFeatureKey,
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
        Map<String, Set<String>> storedSensitiveByFeature = snapshot.sensitiveGrants(positionId).stream()
                .map(ClubPositionSensitiveGrant::getPermissionKey)
                .map(ClubCapability::fromPermissionKey)
                .flatMap(java.util.Optional::stream)
                .collect(Collectors.groupingBy(
                        ClubCapability::featureKey,
                        Collectors.mapping(ClubCapability::permissionKey, Collectors.toSet())
                ));

        Set<String> featureKeys = new LinkedHashSet<>(storedByFeature.keySet());
        effectiveCapabilities.stream().map(ClubCapability::featureKey).forEach(featureKeys::add);

        return featureKeys.stream()
                .sorted()
                .map(featureKey -> resolveFeatureGrant(
                        featureKey,
                        storedByFeature.get(featureKey),
                        storedSensitiveByFeature.getOrDefault(featureKey, Set.of()),
                        effectiveCapabilities
                ))
                .toList();
    }

    @Transactional(transactionManager = "pubTransactionManager")
    public GrantChange replaceVisibleGrants(
            Long positionId,
            List<ClubPositionFeatureGrantRequest> requestedGrants,
            Set<String> visibleFeatureKeys,
            Long actorClubProfileId
    ) {
        Set<String> previousPermissionKeys = permissionRepository.findByClubPositionId(positionId).stream()
                .map(ClubPositionPermission::getPermissionKey)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<ClubPositionFeatureGrant> existingFeatureGrants = featureGrantRepository.findByClubPositionId(positionId);
        List<ClubPositionSensitiveGrant> existingSensitiveGrants = sensitiveGrantRepository.findByClubPositionId(positionId);
        List<NormalizedGrant> normalizedGrants = normalizeGrants(
                requestedGrants,
                visibleFeatureKeys,
                previousPermissionKeys,
                existingFeatureGrants,
                existingSensitiveGrants
        );

        List<ClubPositionFeatureGrant> retainedFeatureGrants = existingFeatureGrants.stream()
                .filter(grant -> !visibleFeatureKeys.contains(grant.getFeatureKey()))
                .toList();
        List<ClubPositionSensitiveGrant> retainedSensitiveGrants = existingSensitiveGrants.stream()
                .filter(grant -> ClubCapability.fromPermissionKey(grant.getPermissionKey())
                        .map(capability -> !visibleFeatureKeys.contains(capability.featureKey()))
                        .orElse(true))
                .toList();
        Set<String> nextPermissionKeys = previousPermissionKeys.stream()
                .filter(permissionKey -> ClubCapability.fromPermissionKey(permissionKey)
                        .map(capability -> !visibleFeatureKeys.contains(capability.featureKey()))
                        .orElse(true))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        featureGrantRepository.deleteByClubPositionId(positionId);
        featureGrantRepository.flush();
        sensitiveGrantRepository.deleteByClubPositionId(positionId);
        sensitiveGrantRepository.flush();

        for (ClubPositionFeatureGrant retained : retainedFeatureGrants) {
            featureGrantRepository.save(ClubPositionFeatureGrant.builder()
                    .clubPositionId(positionId)
                    .featureKey(retained.getFeatureKey())
                    .accessLevel(retained.getAccessLevel())
                    .policyVersion(retained.getPolicyVersion())
                    .build());
        }
        for (ClubPositionSensitiveGrant retained : retainedSensitiveGrants) {
            sensitiveGrantRepository.save(ClubPositionSensitiveGrant.builder()
                    .clubPositionId(positionId)
                    .permissionKey(retained.getPermissionKey())
                    .grantedByClubProfileId(retained.getGrantedByClubProfileId())
                    .grantedAt(retained.getGrantedAt())
                    .build());
        }

        LocalDateTime grantedAt = LocalDateTime.now();
        for (NormalizedGrant grant : normalizedGrants) {
            if (grant.preserveProjection()) {
                if (grant.persistSourceGrant()) {
                    featureGrantRepository.save(ClubPositionFeatureGrant.builder()
                            .clubPositionId(positionId)
                            .featureKey(grant.featureKey())
                            .accessLevel(grant.accessLevelCode())
                            .policyVersion(grant.sourcePolicyVersion())
                            .build());
                    existingSensitiveGrants.stream()
                            .filter(existing -> capabilityFeatureKey(existing.getPermissionKey())
                                    .map(grant.featureKey()::equals)
                                    .orElse(false))
                            .forEach(existing -> sensitiveGrantRepository.save(ClubPositionSensitiveGrant.builder()
                                    .clubPositionId(positionId)
                                    .permissionKey(existing.getPermissionKey())
                                    .grantedByClubProfileId(existing.getGrantedByClubProfileId())
                                    .grantedAt(existing.getGrantedAt())
                                    .build()));
                }
                previousPermissionKeys.stream()
                        .filter(permissionKey -> capabilityFeatureKey(permissionKey)
                                .map(grant.featureKey()::equals)
                                .orElse(false))
                        .forEach(nextPermissionKeys::add);
                continue;
            }
            featureGrantRepository.save(ClubPositionFeatureGrant.builder()
                    .clubPositionId(positionId)
                    .featureKey(grant.featureKey())
                    .accessLevel(grant.accessLevelCode())
                    .policyVersion(grant.policy().policyVersion())
                    .build());
            grant.accessLevel().capabilities().stream()
                    .map(ClubCapability::permissionKey)
                    .forEach(nextPermissionKeys::add);
            for (ClubCapability sensitiveCapability : grant.sensitiveCapabilities()) {
                nextPermissionKeys.add(sensitiveCapability.permissionKey());
                sensitiveGrantRepository.save(ClubPositionSensitiveGrant.builder()
                        .clubPositionId(positionId)
                        .permissionKey(sensitiveCapability.permissionKey())
                        .grantedByClubProfileId(actorClubProfileId)
                        .grantedAt(grantedAt)
                        .build());
            }
        }

        replaceEffectivePermissions(positionId, nextPermissionKeys);
        return new GrantChange(previousPermissionKeys, Set.copyOf(nextPermissionKeys));
    }

    private ClubPositionFeatureGrantResponse resolveFeatureGrant(
            String featureKey,
            ClubPositionFeatureGrant storedGrant,
            Set<String> storedSensitiveKeys,
            Set<ClubCapability> effectiveCapabilities
    ) {
        ClubPositionAccessPolicy.FeatureAccessPolicy policy = accessPolicy.findPolicy(featureKey).orElse(null);
        Set<ClubCapability> effectiveForFeature = effectiveCapabilities.stream()
                .filter(capability -> capability.featureKey().equals(featureKey))
                .collect(Collectors.toSet());
        if (policy == null) {
            return new ClubPositionFeatureGrantResponse(
                    featureKey,
                    "CUSTOM",
                    0,
                    0,
                    STATUS_LEGACY_CUSTOM,
                    List.of(),
                    effectiveForFeature.size()
            );
        }

        Set<ClubCapability> effectiveSensitive = effectiveForFeature.stream()
                .filter(policy.sensitiveCapabilities()::contains)
                .collect(Collectors.toSet());
        if (storedGrant != null) {
            String status = storedGrant.getPolicyVersion() == policy.policyVersion()
                    ? STATUS_CURRENT
                    : STATUS_POLICY_UPDATE_AVAILABLE;
            List<String> sensitiveKeys = storedSensitiveKeys.stream().sorted().toList();
            return new ClubPositionFeatureGrantResponse(
                    featureKey,
                    storedGrant.getAccessLevel(),
                    storedGrant.getPolicyVersion(),
                    policy.policyVersion(),
                    status,
                    sensitiveKeys,
                    effectiveForFeature.size()
            );
        }

        Set<ClubCapability> regularCapabilities = effectiveForFeature.stream()
                .filter(capability -> !policy.sensitiveCapabilities().contains(capability))
                .collect(Collectors.toSet());
        ClubPositionAccessPolicy.AccessLevel matchedLevel = policy.accessLevels().stream()
                .filter(level -> level.capabilities().equals(regularCapabilities))
                .findFirst()
                .orElse(null);
        return new ClubPositionFeatureGrantResponse(
                featureKey,
                matchedLevel == null ? "CUSTOM" : matchedLevel.accessLevel(),
                0,
                policy.policyVersion(),
                matchedLevel == null ? STATUS_LEGACY_CUSTOM : STATUS_LEGACY_DERIVED,
                effectiveSensitive.stream().map(ClubCapability::permissionKey).sorted().toList(),
                effectiveForFeature.size()
        );
    }

    private List<NormalizedGrant> normalizeGrants(
            List<ClubPositionFeatureGrantRequest> requestedGrants,
            Set<String> visibleFeatureKeys,
            Set<String> previousPermissionKeys,
            List<ClubPositionFeatureGrant> existingFeatureGrants,
            List<ClubPositionSensitiveGrant> existingSensitiveGrants
    ) {
        List<ClubPositionFeatureGrantRequest> requests = requestedGrants == null ? List.of() : requestedGrants;
        Map<String, ClubPositionFeatureGrantRequest> requestByFeature = new LinkedHashMap<>();
        for (ClubPositionFeatureGrantRequest request : requests) {
            if (request == null) {
                throw new SemoException.ValidationException("기능별 운영 수준을 확인해주세요.");
            }
            String featureKey = normalizeCode(request.featureKey());
            if (!visibleFeatureKeys.contains(featureKey)) {
                throw new SemoException.ValidationException("활성화되지 않은 기능의 권한은 설정할 수 없습니다.");
            }
            if (requestByFeature.putIfAbsent(featureKey, request) != null) {
                throw new SemoException.ValidationException("같은 기능의 운영 수준을 중복해서 설정할 수 없습니다.");
            }
        }

        List<NormalizedGrant> normalized = new ArrayList<>();
        Map<String, ClubPositionFeatureGrant> existingFeatureGrantByKey = existingFeatureGrants.stream()
                .collect(Collectors.toMap(ClubPositionFeatureGrant::getFeatureKey, Function.identity()));
        Map<String, Set<String>> existingSensitiveKeysByFeature = existingSensitiveGrants.stream()
                .collect(Collectors.groupingBy(
                        grant -> capabilityFeatureKey(grant.getPermissionKey()).orElse(""),
                        Collectors.mapping(ClubPositionSensitiveGrant::getPermissionKey, Collectors.toSet())
                ));
        for (Map.Entry<String, ClubPositionFeatureGrantRequest> entry : requestByFeature.entrySet()) {
            String featureKey = entry.getKey();
            ClubPositionFeatureGrantRequest request = entry.getValue();
            ClubPositionAccessPolicy.FeatureAccessPolicy policy = accessPolicy.findPolicy(featureKey)
                    .orElseThrow(() -> new SemoException.ValidationException("위임할 수 없는 기능이 포함되어 있습니다."));
            String accessLevelCode = normalizeCode(request.accessLevel());
            if (ClubPositionAccessPolicy.ACCESS_NONE.equals(accessLevelCode)) {
                if (request.sensitivePermissionKeys() != null && !request.sensitivePermissionKeys().isEmpty()) {
                    throw new SemoException.ValidationException("추가 승인 권한에는 기본 운영 수준이 필요합니다.");
                }
                continue;
            }
            Integer requestedPolicyVersion = request.policyVersion();
            if (requestedPolicyVersion != null && requestedPolicyVersion > policy.policyVersion()) {
                throw new SemoException.ValidationException("현재 서버보다 최신인 권한 정책 버전은 적용할 수 없습니다.");
            }
            Set<String> requestedSensitiveKeys = normalizeSensitiveKeys(request.sensitivePermissionKeys());
            if (requestedPolicyVersion != null && requestedPolicyVersion < policy.policyVersion()) {
                ClubPositionFeatureGrant existingSource = existingFeatureGrantByKey.get(featureKey);
                Set<String> existingSensitiveKeys = existingSensitiveKeysByFeature.getOrDefault(featureKey, Set.of());
                if (existingSource != null) {
                    boolean unchangedSource = existingSource.getPolicyVersion() == requestedPolicyVersion
                            && existingSource.getAccessLevel().equals(accessLevelCode)
                            && existingSensitiveKeys.equals(requestedSensitiveKeys);
                    if (!unchangedSource) {
                        throw new SemoException.ConflictException(
                                "이전 정책 권한은 그대로 보존하거나 최신 정책으로 명시적으로 전환해야 합니다."
                        );
                    }
                    normalized.add(NormalizedGrant.preserved(
                            featureKey,
                            policy,
                            accessLevelCode,
                            requestedPolicyVersion,
                            true
                    ));
                    continue;
                }
                if (requestedPolicyVersion == 0 && legacyRequestMatchesProjection(
                        featureKey,
                        accessLevelCode,
                        requestedSensitiveKeys,
                        policy,
                        previousPermissionKeys
                )) {
                    normalized.add(NormalizedGrant.preserved(featureKey, policy, accessLevelCode, 0, false));
                    continue;
                }
                throw new SemoException.ConflictException(
                        "기존 권한 구성이 변경되었습니다. 최신 내용을 다시 불러온 뒤 저장해주세요."
                );
            }
            ClubPositionAccessPolicy.AccessLevel level = policy.accessLevels().stream()
                    .filter(candidate -> candidate.accessLevel().equals(accessLevelCode))
                    .findFirst()
                    .orElseThrow(() -> new SemoException.ValidationException("지원하지 않는 운영 수준입니다."));
            Set<String> supportedSensitiveKeys = policy.sensitiveCapabilities().stream()
                    .map(ClubCapability::permissionKey)
                    .collect(Collectors.toSet());
            if (!supportedSensitiveKeys.containsAll(requestedSensitiveKeys)) {
                throw new SemoException.ValidationException("지원하지 않는 추가 승인 권한이 포함되어 있습니다.");
            }
            Set<ClubCapability> sensitiveCapabilities = requestedSensitiveKeys.stream()
                    .map(ClubCapability::fromPermissionKey)
                    .flatMap(java.util.Optional::stream)
                    .collect(Collectors.toSet());
            normalized.add(NormalizedGrant.current(featureKey, policy, level, sensitiveCapabilities));
        }
        return normalized.stream()
                .sorted(Comparator.comparing(NormalizedGrant::featureKey))
                .toList();
    }

    private Set<String> normalizeSensitiveKeys(List<String> permissionKeys) {
        return (permissionKeys == null ? List.<String>of() : permissionKeys).stream()
                .map(this::normalizeCode)
                .collect(Collectors.toSet());
    }

    private boolean legacyRequestMatchesProjection(
            String featureKey,
            String accessLevelCode,
            Set<String> requestedSensitiveKeys,
            ClubPositionAccessPolicy.FeatureAccessPolicy policy,
            Set<String> previousPermissionKeys
    ) {
        Set<ClubCapability> capabilities = previousPermissionKeys.stream()
                .map(ClubCapability::fromPermissionKey)
                .flatMap(java.util.Optional::stream)
                .filter(capability -> featureKey.equals(capability.featureKey()))
                .collect(Collectors.toSet());
        Set<String> projectedSensitiveKeys = capabilities.stream()
                .filter(policy.sensitiveCapabilities()::contains)
                .map(ClubCapability::permissionKey)
                .collect(Collectors.toSet());
        if (!projectedSensitiveKeys.equals(requestedSensitiveKeys)) {
            return false;
        }
        Set<ClubCapability> regularCapabilities = capabilities.stream()
                .filter(capability -> !policy.sensitiveCapabilities().contains(capability))
                .collect(Collectors.toSet());
        if ("CUSTOM".equals(accessLevelCode)) {
            return policy.accessLevels().stream().noneMatch(level -> level.capabilities().equals(regularCapabilities));
        }
        return policy.accessLevels().stream()
                .anyMatch(level -> level.accessLevel().equals(accessLevelCode)
                        && level.capabilities().equals(regularCapabilities));
    }

    private java.util.Optional<String> capabilityFeatureKey(String permissionKey) {
        return ClubCapability.fromPermissionKey(permissionKey).map(ClubCapability::featureKey);
    }

    private void replaceEffectivePermissions(Long positionId, Set<String> permissionKeys) {
        permissionRepository.deleteByClubPositionId(positionId);
        permissionRepository.flush();
        permissionKeys.stream().sorted().forEach(permissionKey -> permissionRepository.save(
                ClubPositionPermission.builder()
                        .clubPositionId(positionId)
                        .permissionKey(permissionKey)
                        .build()
        ));
    }

    private String normalizeCode(String value) {
        if (!StringUtils.hasText(value)) {
            throw new SemoException.ValidationException("기능별 운영 수준을 확인해주세요.");
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private record NormalizedGrant(
            String featureKey,
            ClubPositionAccessPolicy.FeatureAccessPolicy policy,
            ClubPositionAccessPolicy.AccessLevel accessLevel,
            String accessLevelCode,
            Set<ClubCapability> sensitiveCapabilities,
            int sourcePolicyVersion,
            boolean preserveProjection,
            boolean persistSourceGrant
    ) {
        private static NormalizedGrant current(
                String featureKey,
                ClubPositionAccessPolicy.FeatureAccessPolicy policy,
                ClubPositionAccessPolicy.AccessLevel accessLevel,
                Set<ClubCapability> sensitiveCapabilities
        ) {
            return new NormalizedGrant(
                    featureKey,
                    policy,
                    accessLevel,
                    accessLevel.accessLevel(),
                    sensitiveCapabilities,
                    policy.policyVersion(),
                    false,
                    true
            );
        }

        private static NormalizedGrant preserved(
                String featureKey,
                ClubPositionAccessPolicy.FeatureAccessPolicy policy,
                String accessLevelCode,
                int sourcePolicyVersion,
                boolean persistSourceGrant
        ) {
            return new NormalizedGrant(
                    featureKey,
                    policy,
                    null,
                    accessLevelCode,
                    Set.of(),
                    sourcePolicyVersion,
                    true,
                    persistSourceGrant
            );
        }
    }

    public record GrantChange(Set<String> previousPermissionKeys, Set<String> nextPermissionKeys) {
        public boolean changed() {
            return !previousPermissionKeys.equals(nextPermissionKeys);
        }

        public int addedCount() {
            return (int) nextPermissionKeys.stream().filter(key -> !previousPermissionKeys.contains(key)).count();
        }

        public int removedCount() {
            return (int) previousPermissionKeys.stream().filter(key -> !nextPermissionKeys.contains(key)).count();
        }
    }

    public record GrantSnapshot(
            Map<Long, List<ClubPositionFeatureGrant>> featureGrantsByPositionId,
            Map<Long, List<ClubPositionSensitiveGrant>> sensitiveGrantsByPositionId
    ) {
        public GrantSnapshot {
            featureGrantsByPositionId = Map.copyOf(featureGrantsByPositionId);
            sensitiveGrantsByPositionId = Map.copyOf(sensitiveGrantsByPositionId);
        }

        public static GrantSnapshot empty() {
            return new GrantSnapshot(Map.of(), Map.of());
        }

        public List<ClubPositionFeatureGrant> featureGrants(Long positionId) {
            return featureGrantsByPositionId.getOrDefault(positionId, List.of());
        }

        public List<ClubPositionSensitiveGrant> sensitiveGrants(Long positionId) {
            return sensitiveGrantsByPositionId.getOrDefault(positionId, List.of());
        }
    }
}
