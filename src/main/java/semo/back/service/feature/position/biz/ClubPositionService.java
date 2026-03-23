package semo.back.service.feature.position.biz;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import semo.back.service.common.exception.SemoException;
import semo.back.service.database.pub.entity.ClubMember;
import semo.back.service.database.pub.entity.ClubMemberPosition;
import semo.back.service.database.pub.entity.ClubPosition;
import semo.back.service.database.pub.entity.ClubPositionPermission;
import semo.back.service.database.pub.entity.ClubFeature;
import semo.back.service.database.pub.entity.FeatureCatalog;
import semo.back.service.database.pub.entity.FeaturePermissionCatalog;
import semo.back.service.database.pub.repository.ClubFeatureRepository;
import semo.back.service.database.pub.repository.ClubMemberPositionRepository;
import semo.back.service.database.pub.repository.ClubPositionPermissionRepository;
import semo.back.service.database.pub.repository.ClubPositionRepository;
import semo.back.service.database.pub.repository.FeatureCatalogRepository;
import semo.back.service.database.pub.repository.FeaturePermissionCatalogRepository;
import semo.back.service.feature.club.biz.ClubAccessResolver;
import semo.back.service.feature.position.vo.ClubAdminRoleManagementResponse;
import semo.back.service.feature.position.vo.ClubPermissionGroupResponse;
import semo.back.service.feature.position.vo.ClubPermissionItemResponse;
import semo.back.service.feature.position.vo.ClubPositionDetailResponse;
import semo.back.service.feature.position.vo.ClubPositionSummaryResponse;
import semo.back.service.feature.position.vo.CreateClubPositionRequest;
import semo.back.service.feature.position.vo.UpdateClubPositionRequest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubPositionService {
    private static final String FEATURE_ATTENDANCE = "ATTENDANCE";
    private static final String FEATURE_NOTICE = "NOTICE";
    private static final String FEATURE_POLL = "POLL";
    private static final String FEATURE_ROLE_MANAGEMENT = "ROLE_MANAGEMENT";
    private static final String FEATURE_SCHEDULE_MANAGE = "SCHEDULE_MANAGE";
    private static final String FEATURE_TIMELINE = "TIMELINE";

    private final ClubAccessResolver clubAccessResolver;
    private final ClubPositionPermissionEvaluator clubPositionPermissionEvaluator;
    private final ClubFeatureRepository clubFeatureRepository;
    private final FeatureCatalogRepository featureCatalogRepository;
    private final FeaturePermissionCatalogRepository featurePermissionCatalogRepository;
    private final ClubPositionRepository clubPositionRepository;
    private final ClubPositionPermissionRepository clubPositionPermissionRepository;
    private final ClubMemberPositionRepository clubMemberPositionRepository;

    public ClubAdminRoleManagementResponse getRoleManagement(Long clubId, String userKey) {
        requireRoleManagementFeature(clubId);
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireAdmin(clubId, userKey);
        PositionSnapshot snapshot = loadSnapshot(clubId);
        return new ClubAdminRoleManagementResponse(
                access.club().getClubId(),
                access.club().getName(),
                true,
                true,
                snapshot.positionSummaries(),
                snapshot.permissionGroups()
        );
    }

    public ClubPositionDetailResponse getPositionDetail(Long clubId, Long clubPositionId, String userKey) {
        requireRoleManagementFeature(clubId);
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireAdmin(clubId, userKey);
        PositionSnapshot snapshot = loadSnapshot(clubId);
        ClubPositionSummaryResponse position = snapshot.positionSummaryById().get(clubPositionId);
        if (position == null) {
            throw new SemoException.ResourceNotFoundException("ClubPosition", "clubPositionId", clubPositionId);
        }
        return new ClubPositionDetailResponse(
                access.club().getClubId(),
                access.club().getName(),
                true,
                true,
                position,
                snapshot.permissionGroups()
        );
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    public ClubPositionDetailResponse createPosition(Long clubId, String userKey, CreateClubPositionRequest request) {
        requireRoleManagementFeature(clubId);
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireAdmin(clubId, userKey);
        PermissionCatalogSnapshot permissionCatalogSnapshot = loadPermissionCatalogSnapshot(clubId);
        String normalizedCode = normalizePositionCode(request.positionCode());
        if (clubPositionRepository.existsByClubIdAndPositionCode(clubId, normalizedCode)) {
            throw new SemoException.ValidationException("이미 사용 중인 직책 코드입니다.");
        }

        ClubPosition position = clubPositionRepository.save(ClubPosition.builder()
                .clubId(clubId)
                .positionCode(normalizedCode)
                .displayName(requireDisplayName(request.displayName()))
                .description(trimToNull(request.description()))
                .iconName(trimToNull(request.iconName()))
                .colorHex(normalizeColorHex(request.colorHex()))
                .active(true)
                .createdByClubProfileId(access.clubProfile().getClubProfileId())
                .build());
        replacePositionPermissions(
                position.getClubPositionId(),
                normalizePermissionKeys(request.permissionKeys(), permissionCatalogSnapshot.visiblePermissionKeys())
        );
        return getPositionDetail(clubId, position.getClubPositionId(), userKey);
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    public ClubPositionDetailResponse updatePosition(
            Long clubId,
            Long clubPositionId,
            String userKey,
            UpdateClubPositionRequest request
    ) {
        requireRoleManagementFeature(clubId);
        clubAccessResolver.requireAdmin(clubId, userKey);
        PermissionCatalogSnapshot permissionCatalogSnapshot = loadPermissionCatalogSnapshot(clubId);
        ClubPosition current = requirePosition(clubId, clubPositionId);
        String normalizedCode = normalizePositionCode(request.positionCode());
        if (clubPositionRepository.existsByClubIdAndPositionCodeAndClubPositionIdNot(clubId, normalizedCode, clubPositionId)) {
            throw new SemoException.ValidationException("이미 사용 중인 직책 코드입니다.");
        }

        clubPositionRepository.save(ClubPosition.builder()
                .clubPositionId(current.getClubPositionId())
                .clubId(current.getClubId())
                .positionCode(normalizedCode)
                .displayName(requireDisplayName(request.displayName()))
                .description(trimToNull(request.description()))
                .iconName(trimToNull(request.iconName()))
                .colorHex(normalizeColorHex(request.colorHex()))
                .active(request.active() == null || request.active())
                .createdByClubProfileId(current.getCreatedByClubProfileId())
                .build());
        Set<String> hiddenPermissionKeys = clubPositionPermissionRepository.findByClubPositionId(clubPositionId).stream()
                .map(ClubPositionPermission::getPermissionKey)
                .filter(permissionKey -> !permissionCatalogSnapshot.visiblePermissionKeys().contains(permissionKey))
                .collect(Collectors.toSet());
        Set<String> nextPermissionKeys = normalizePermissionKeys(request.permissionKeys(), permissionCatalogSnapshot.visiblePermissionKeys());
        nextPermissionKeys.addAll(hiddenPermissionKeys);
        replacePositionPermissions(clubPositionId, nextPermissionKeys);
        return getPositionDetail(clubId, clubPositionId, userKey);
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    public void deletePosition(Long clubId, Long clubPositionId, String userKey) {
        requireRoleManagementFeature(clubId);
        clubAccessResolver.requireAdmin(clubId, userKey);
        ClubPosition current = requirePosition(clubId, clubPositionId);
        clubMemberPositionRepository.deleteByClubPositionId(current.getClubPositionId());
        clubPositionPermissionRepository.deleteByClubPositionId(current.getClubPositionId());
        clubPositionRepository.delete(current);
    }

    public boolean isRoleManagementEnabled(Long clubId) {
        return clubPositionPermissionEvaluator.isRoleManagementEnabled(clubId);
    }

    public List<ClubPositionSummaryResponse> getAvailablePositionSummaries(Long clubId) {
        if (!isRoleManagementEnabled(clubId)) {
            return List.of();
        }
        return loadSnapshot(clubId).positionSummaries().stream()
                .filter(ClubPositionSummaryResponse::active)
                .toList();
    }

    public Map<Long, List<ClubPositionSummaryResponse>> getAssignedPositionSummaries(Long clubId, List<Long> clubMemberIds) {
        if (!isRoleManagementEnabled(clubId) || clubMemberIds.isEmpty()) {
            return Map.of();
        }

        PositionSnapshot snapshot = loadSnapshot(clubId);
        Map<Long, List<Long>> positionIdsByMemberId = clubMemberPositionRepository.findByClubMemberIdIn(clubMemberIds).stream()
                .collect(Collectors.groupingBy(
                        ClubMemberPosition::getClubMemberId,
                        Collectors.mapping(ClubMemberPosition::getClubPositionId, Collectors.toList())
                ));

        Map<Long, List<ClubPositionSummaryResponse>> result = new LinkedHashMap<>();
        for (Long clubMemberId : clubMemberIds) {
            List<ClubPositionSummaryResponse> assigned = positionIdsByMemberId.getOrDefault(clubMemberId, List.of()).stream()
                    .map(snapshot.positionSummaryById()::get)
                    .filter(item -> item != null && item.active())
                    .sorted(Comparator.comparing(ClubPositionSummaryResponse::displayName, String.CASE_INSENSITIVE_ORDER))
                    .toList();
            result.put(clubMemberId, assigned);
        }
        return result;
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    public void replaceMemberPositions(
            ClubAccessResolver.ClubAccess actorAccess,
            ClubMember target,
            List<Long> clubPositionIds
    ) {
        if (!clubPositionPermissionEvaluator.isRoleManagementEnabled(actorAccess.club().getClubId())) {
            throw new SemoException.ValidationException("직책관리 기능이 활성화되지 않았습니다.");
        }
        List<Long> normalizedPositionIds = normalizePositionIds(clubPositionIds);
        List<ClubPosition> positions = normalizedPositionIds.isEmpty()
                ? List.of()
                : clubPositionRepository.findAllById(normalizedPositionIds);
        if (positions.size() != normalizedPositionIds.size() || positions.stream().anyMatch(position -> !position.getClubId().equals(target.getClubId()))) {
            throw new SemoException.ValidationException("다른 모임의 직책은 할당할 수 없습니다.");
        }

        clubMemberPositionRepository.deleteByClubMemberId(target.getClubMemberId());
        LocalDateTime now = LocalDateTime.now();
        for (Long clubPositionId : normalizedPositionIds) {
            clubMemberPositionRepository.save(ClubMemberPosition.builder()
                    .clubMemberId(target.getClubMemberId())
                    .clubPositionId(clubPositionId)
                    .assignedByClubProfileId(actorAccess.clubProfile().getClubProfileId())
                    .assignedAt(now)
                    .build());
        }
    }

    private PositionSnapshot loadSnapshot(Long clubId) {
        PermissionCatalogSnapshot permissionCatalogSnapshot = loadPermissionCatalogSnapshot(clubId);
        List<ClubPosition> positions = clubPositionRepository.findByClubIdOrderByDisplayNameAscClubPositionIdAsc(clubId);
        List<Long> positionIds = positions.stream().map(ClubPosition::getClubPositionId).toList();
        Map<Long, List<ClubPositionPermission>> permissionsByPositionId = positionIds.isEmpty()
                ? Map.of()
                : clubPositionPermissionRepository.findByClubPositionIdIn(positionIds).stream()
                .collect(Collectors.groupingBy(ClubPositionPermission::getClubPositionId));
        Map<Long, Long> memberCountByPositionId = positionIds.isEmpty()
                ? Map.of()
                : clubMemberPositionRepository.findByClubPositionIdIn(positionIds).stream()
                .collect(Collectors.groupingBy(ClubMemberPosition::getClubPositionId, Collectors.counting()));

        Map<Long, ClubPositionSummaryResponse> summaryById = new LinkedHashMap<>();
        List<ClubPositionSummaryResponse> summaries = positions.stream()
                .map(position -> {
                    List<String> permissionKeys = permissionsByPositionId.getOrDefault(position.getClubPositionId(), List.of()).stream()
                            .map(ClubPositionPermission::getPermissionKey)
                            .filter(permissionCatalogSnapshot.visiblePermissionKeys()::contains)
                            .sorted()
                            .toList();
                    ClubPositionSummaryResponse summary = new ClubPositionSummaryResponse(
                            position.getClubPositionId(),
                            position.getPositionCode(),
                            position.getDisplayName(),
                            position.getDescription(),
                            position.getIconName(),
                            position.getColorHex(),
                            position.isActive(),
                            permissionKeys.size(),
                            memberCountByPositionId.getOrDefault(position.getClubPositionId(), 0L).intValue(),
                            permissionKeys
                    );
                    summaryById.put(position.getClubPositionId(), summary);
                    return summary;
                })
                .toList();

        return new PositionSnapshot(summaries, summaryById, permissionCatalogSnapshot.permissionGroups());
    }

    private PermissionCatalogSnapshot loadPermissionCatalogSnapshot(Long clubId) {
        Set<String> visibleFeatureKeys = resolveVisibleFeatureKeys(clubId);
        List<FeatureCatalog> catalogs = featureCatalogRepository.findByActiveTrueOrderBySortOrderAscFeatureKeyAsc().stream()
                .filter(catalog -> visibleFeatureKeys.contains(catalog.getFeatureKey()))
                .toList();
        Map<String, List<FeaturePermissionCatalog>> permissionsByFeatureKey = featurePermissionCatalogRepository
                .findByActiveTrueOrderByFeatureKeyAscSortOrderAscPermissionKeyAsc().stream()
                .filter(permission -> visibleFeatureKeys.contains(permission.getFeatureKey()))
                .collect(Collectors.groupingBy(FeaturePermissionCatalog::getFeatureKey));

        List<ClubPermissionGroupResponse> groups = new ArrayList<>();
        for (FeatureCatalog catalog : catalogs) {
            List<FeaturePermissionCatalog> permissions = permissionsByFeatureKey.getOrDefault(catalog.getFeatureKey(), List.of());
            if (permissions.isEmpty()) {
                continue;
            }
            groups.add(new ClubPermissionGroupResponse(
                    catalog.getFeatureKey(),
                    resolvePermissionGroupDisplayName(catalog),
                    catalog.getDescription(),
                    catalog.getIconName(),
                    permissions.stream()
                            .map(permission -> new ClubPermissionItemResponse(
                                    permission.getPermissionKey(),
                                    permission.getDisplayName(),
                                    permission.getDescription(),
                                    permission.getOwnershipScope()
                            ))
                            .toList()
            ));
        }
        Set<String> visiblePermissionKeys = groups.stream()
                .flatMap(group -> group.permissions().stream())
                .map(ClubPermissionItemResponse::permissionKey)
                .collect(Collectors.toSet());
        return new PermissionCatalogSnapshot(groups, visiblePermissionKeys);
    }

    private Set<String> resolveVisibleFeatureKeys(Long clubId) {
        Set<String> visibleFeatureKeys = clubFeatureRepository.findByClubId(clubId).stream()
                .filter(ClubFeature::isEnabled)
                .map(ClubFeature::getFeatureKey)
                .collect(Collectors.toSet());
        visibleFeatureKeys.add(FEATURE_ROLE_MANAGEMENT);
        return visibleFeatureKeys;
    }

    private String resolvePermissionGroupDisplayName(FeatureCatalog catalog) {
        return switch (catalog.getFeatureKey()) {
            case FEATURE_ATTENDANCE -> "출석관리";
            case FEATURE_NOTICE -> "공지관리";
            case FEATURE_POLL -> "투표관리";
            case FEATURE_SCHEDULE_MANAGE -> "일정관리";
            case FEATURE_TIMELINE -> "타임라인관리";
            default -> catalog.getDisplayName();
        };
    }

    private void replacePositionPermissions(Long clubPositionId, Set<String> permissionKeys) {
        clubPositionPermissionRepository.deleteByClubPositionId(clubPositionId);
        clubPositionPermissionRepository.flush();
        for (String permissionKey : permissionKeys) {
            clubPositionPermissionRepository.save(ClubPositionPermission.builder()
                    .clubPositionId(clubPositionId)
                    .permissionKey(permissionKey)
                    .build());
        }
    }

    private ClubPosition requirePosition(Long clubId, Long clubPositionId) {
        return clubPositionRepository.findByClubPositionIdAndClubId(clubPositionId, clubId)
                .orElseThrow(() -> new SemoException.ResourceNotFoundException("ClubPosition", "clubPositionId", clubPositionId));
    }

    private void requireRoleManagementFeature(Long clubId) {
        if (!isRoleManagementEnabled(clubId)) {
            throw new SemoException.ValidationException("직책관리 기능이 활성화되지 않았습니다.");
        }
    }

    private String requireDisplayName(String displayName) {
        if (!StringUtils.hasText(displayName)) {
            throw new SemoException.ValidationException("직책 이름은 필수입니다.");
        }
        return displayName.trim();
    }

    private String normalizePositionCode(String positionCode) {
        String trimmed = trimToNull(positionCode);
        if (trimmed == null) {
            throw new SemoException.ValidationException("직책 코드는 필수입니다.");
        }
        String normalized = trimmed
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
        if (!StringUtils.hasText(normalized)) {
            throw new SemoException.ValidationException("직책 코드를 확인해주세요.");
        }
        return normalized;
    }

    private String normalizeColorHex(String colorHex) {
        String trimmed = trimToNull(colorHex);
        if (trimmed == null) {
            return null;
        }
        if (!trimmed.startsWith("#")) {
            return "#" + trimmed;
        }
        return trimmed;
    }

    private Set<String> normalizePermissionKeys(List<String> permissionKeys, Set<String> allowedKeys) {
        Set<String> normalized = (permissionKeys == null ? List.<String>of() : permissionKeys).stream()
                .map(this::trimToNull)
                .filter(StringUtils::hasText)
                .map(value -> value.toUpperCase(Locale.ROOT))
                .collect(Collectors.toSet());
        if (!allowedKeys.containsAll(normalized)) {
            throw new SemoException.ValidationException("지원하지 않는 하위 권한이 포함되어 있습니다.");
        }
        return normalized;
    }

    private List<Long> normalizePositionIds(List<Long> clubPositionIds) {
        if (clubPositionIds == null) {
            return List.of();
        }
        return clubPositionIds.stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private record PositionSnapshot(
            List<ClubPositionSummaryResponse> positionSummaries,
            Map<Long, ClubPositionSummaryResponse> positionSummaryById,
            List<ClubPermissionGroupResponse> permissionGroups
    ) {
    }

    private record PermissionCatalogSnapshot(
            List<ClubPermissionGroupResponse> permissionGroups,
            Set<String> visiblePermissionKeys
    ) {
    }
}
