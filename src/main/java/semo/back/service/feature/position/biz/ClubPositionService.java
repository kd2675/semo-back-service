package semo.back.service.feature.position.biz;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import semo.back.service.common.exception.SemoException;
import semo.back.service.common.jpa.MonotonicDateTimeProvider;
import semo.back.service.database.pub.entity.ClubMember;
import semo.back.service.database.pub.entity.ClubMemberPosition;
import semo.back.service.database.pub.entity.ClubMemberPositionHistory;
import semo.back.service.database.pub.entity.ClubPosition;
import semo.back.service.database.pub.entity.ClubPositionPermission;
import semo.back.service.database.pub.entity.ClubProfile;
import semo.back.service.database.pub.entity.ClubFeature;
import semo.back.service.database.pub.entity.FeatureCatalog;
import semo.back.service.database.pub.entity.FeaturePermissionCatalog;
import semo.back.service.database.pub.repository.ClubFeatureRepository;
import semo.back.service.database.pub.repository.ClubMemberPositionRepository;
import semo.back.service.database.pub.repository.ClubMemberPositionHistoryRepository;
import semo.back.service.database.pub.repository.ClubPositionPermissionRepository;
import semo.back.service.database.pub.repository.ClubPositionRepository;
import semo.back.service.database.pub.repository.ClubProfileRepository;
import semo.back.service.database.pub.repository.FeatureCatalogRepository;
import semo.back.service.database.pub.repository.FeaturePermissionCatalogRepository;
import semo.back.service.feature.activity.biz.ClubActivityContextHolder;
import semo.back.service.feature.activity.biz.RecordClubActivity;
import semo.back.service.feature.club.biz.policy.ClubAccessResolver;
import semo.back.service.feature.position.vo.ClubAdminRoleManagementResponse;
import semo.back.service.feature.position.vo.ClubFeatureAccessLevelResponse;
import semo.back.service.feature.position.vo.ClubPermissionGroupResponse;
import semo.back.service.feature.position.vo.ClubPermissionItemResponse;
import semo.back.service.feature.position.vo.ClubPositionFeatureGrantResponse;
import semo.back.service.feature.position.vo.ClubPositionFeatureGrantRequest;
import semo.back.service.feature.position.vo.ClubPositionHistoryItemResponse;
import semo.back.service.feature.position.vo.ClubPositionHistoryResponse;
import semo.back.service.feature.position.vo.ClubPositionDetailResponse;
import semo.back.service.feature.position.vo.ClubPositionSummaryResponse;
import semo.back.service.feature.position.vo.ClubPositionTemplateResponse;
import semo.back.service.feature.position.vo.ClubPositionTemplateGrantResponse;
import semo.back.service.feature.position.vo.CreateClubPositionRequest;
import semo.back.service.feature.position.vo.UpdateClubPositionRequest;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    private final MonotonicDateTimeProvider dateTimeProvider;
    private static final String FEATURE_NOTICE = "NOTICE";
    private static final String FEATURE_POLL = "POLL";
    private static final String FEATURE_ROLE_MANAGEMENT = "ROLE_MANAGEMENT";
    private static final String FEATURE_SCHEDULE_MANAGE = "SCHEDULE_MANAGE";
    private static final String FEATURE_TOURNAMENT_RECORD = "TOURNAMENT_RECORD";
    private static final String FEATURE_BRACKET = "BRACKET";
    private static final String FEATURE_FINANCE = "FINANCE";
    private static final String FEATURE_TODO = "TODO";
    private static final String MEMBERSHIP_STATUS_ACTIVE = "ACTIVE";
    private static final DateTimeFormatter DATE_TIME_VALUE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter DATE_TIME_LABEL_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm", Locale.KOREAN);

    private final ClubAccessResolver clubAccessResolver;
    private final ClubPositionAccessPolicy clubPositionAccessPolicy;
    private final ClubPositionGrantService clubPositionGrantService;
    private final ClubPositionPermissionEvaluator clubPositionPermissionEvaluator;
    private final ClubFeatureRepository clubFeatureRepository;
    private final FeatureCatalogRepository featureCatalogRepository;
    private final FeaturePermissionCatalogRepository featurePermissionCatalogRepository;
    private final ClubPositionRepository clubPositionRepository;
    private final ClubPositionPermissionRepository clubPositionPermissionRepository;
    private final ClubMemberPositionRepository clubMemberPositionRepository;
    private final ClubMemberPositionHistoryRepository clubMemberPositionHistoryRepository;
    private final ClubProfileRepository clubProfileRepository;

    public ClubAdminRoleManagementResponse getRoleManagement(Long clubId, String userKey) {
        requireRoleManagementFeature(clubId);
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireAdmin(clubId, userKey);
        PositionSnapshot snapshot = loadSnapshot(clubId);
        return new ClubAdminRoleManagementResponse(
                access.club().getClubId(),
                access.club().getName(),
                access.isAdmin(),
                true,
                true,
                true,
                true,
                true,
                snapshot.assignedMemberCount(),
                snapshot.positionSummaries(),
                snapshot.permissionGroups(),
                snapshot.positionTemplates()
        );
    }

    public ClubPositionDetailResponse getPositionDetail(Long clubId, Long clubPositionId, String userKey) {
        requireRoleManagementFeature(clubId);
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireAdmin(clubId, userKey);
        return buildPositionDetail(access, clubPositionId);
    }

    @Transactional(transactionManager = "pubTransactionManager")
    @RecordClubActivity(subject = "직책관리")
    public ClubPositionDetailResponse createPosition(Long clubId, String userKey, CreateClubPositionRequest request) {
        requireRoleManagementFeature(clubId);
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireAdmin(clubId, userKey);
        PermissionCatalogSnapshot permissionCatalogSnapshot = loadPermissionCatalogSnapshot(clubId);
        String normalizedCode = normalizePositionCode(request.positionCode());
        if (clubPositionRepository.existsByClubIdAndPositionCode(clubId, normalizedCode)) {
            throw new SemoException.ValidationException("이미 사용 중인 직책 코드입니다.");
        }
        String displayName = requireDisplayName(request.displayName());
        ClubActivityContextHolder.setDetails(
                "직책 '" + displayName + "'을 생성했습니다.",
                "직책 '" + displayName + "' 생성에 실패했습니다."
        );
        validateRequestedGrantCatalog(request.featureGrants(), permissionCatalogSnapshot);

        ClubPosition position = clubPositionRepository.save(ClubPosition.builder()
                .clubId(clubId)
                .positionCode(normalizedCode)
                .displayName(displayName)
                .description(trimToNull(request.description()))
                .iconName(trimToNull(request.iconName()))
                .colorHex(normalizeColorHex(request.colorHex()))
                .active(true)
                .createdByClubProfileId(access.clubProfile().getClubProfileId())
                .build());
        clubPositionGrantService.replaceVisibleGrants(
                position.getClubPositionId(),
                request.featureGrants(),
                permissionCatalogSnapshot.visibleFeatureKeys(),
                access.clubProfile().getClubProfileId()
        );
        return buildPositionDetail(access, position.getClubPositionId());
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "직책관리")
    public ClubPositionDetailResponse updatePosition(
            Long clubId,
            Long clubPositionId,
            String userKey,
            UpdateClubPositionRequest request
    ) {
        requireRoleManagementFeature(clubId);
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireAdmin(clubId, userKey);
        PermissionCatalogSnapshot permissionCatalogSnapshot = loadPermissionCatalogSnapshot(clubId);
        ClubPosition current = requirePosition(clubId, clubPositionId);
        requireMatchingVersion(current, request.version());
        String normalizedCode = normalizePositionCode(request.positionCode());
        if (!current.getPositionCode().equals(normalizedCode)) {
            throw new SemoException.ValidationException("직책 코드는 생성 후 변경할 수 없습니다.");
        }
        String displayName = requireDisplayName(request.displayName());
        if (clubPositionRepository.claimVersion(clubPositionId, clubId, request.version()) != 1) {
            throw stalePositionConflict();
        }
        current = requirePosition(clubId, clubPositionId);
        boolean nextActive = request.active() == null || request.active();
        if (current.isActive() && !nextActive) {
            retireCurrentAssignments(clubId, current.getClubPositionId(), access.clubProfile().getClubProfileId());
        }
        current.updateDetails(
                displayName,
                trimToNull(request.description()),
                trimToNull(request.iconName()),
                normalizeColorHex(request.colorHex()),
                nextActive
        );
        ClubPositionGrantService.GrantChange grantChange = request.featureGrants() == null
                ? null
                : replaceValidatedGrants(
                        clubPositionId,
                        request.featureGrants(),
                        permissionCatalogSnapshot,
                        access.clubProfile().getClubProfileId()
                );
        String changeSummary = grantChange != null && grantChange.changed()
                ? " 권한 " + grantChange.addedCount() + "개 추가, " + grantChange.removedCount() + "개 회수"
                : "";
        ClubActivityContextHolder.setDetails(
                "직책 '" + current.getDisplayName() + "'을 수정했습니다." + changeSummary,
                "직책 '" + current.getDisplayName() + "' 수정에 실패했습니다."
        );
        return buildPositionDetail(access, clubPositionId);
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "직책관리")
    public void deletePosition(Long clubId, Long clubPositionId, Long expectedVersion, String userKey) {
        requireRoleManagementFeature(clubId);
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireAdmin(clubId, userKey);
        ClubPosition current = requirePosition(clubId, clubPositionId);
        requireMatchingVersion(current, expectedVersion);
        if (clubPositionRepository.claimVersion(clubPositionId, clubId, expectedVersion) != 1) {
            throw stalePositionConflict();
        }
        current = requirePosition(clubId, clubPositionId);
        ClubActivityContextHolder.setDetails(
                "직책 '" + current.getDisplayName() + "'의 사용을 종료했습니다.",
                "직책 '" + current.getDisplayName() + "' 사용 종료에 실패했습니다."
        );
        retireCurrentAssignments(clubId, current.getClubPositionId(), access.clubProfile().getClubProfileId());
        current.deactivate();
    }

    public ClubPositionHistoryResponse getPositionHistory(Long clubId, String userKey) {
        requireRoleManagementFeature(clubId);
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireAdmin(clubId, userKey);
        List<ClubMemberPositionHistory> histories = clubMemberPositionHistoryRepository
                .findByClubIdAndDeletedFalseOrderByStartedAtDescClubMemberPositionHistoryIdDesc(clubId);
        List<Long> clubMemberIds = histories.stream()
                .map(ClubMemberPositionHistory::getClubMemberId)
                .distinct()
                .toList();
        Map<Long, String> displayNameByMemberId = clubMemberIds.isEmpty()
                ? Map.of()
                : clubProfileRepository.findByClubMemberIdIn(clubMemberIds).stream()
                        .collect(Collectors.toMap(
                                ClubProfile::getClubMemberId,
                                ClubProfile::getDisplayName,
                                (left, right) -> left
                        ));

        return new ClubPositionHistoryResponse(
                access.club().getClubId(),
                access.club().getName(),
                access.isAdmin(),
                histories.stream()
                        .map(history -> toHistoryResponse(history, displayNameByMemberId.get(history.getClubMemberId())))
                        .toList()
        );
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

    @Transactional(transactionManager = "pubTransactionManager")
    public void replaceMemberPositions(
            ClubAccessResolver.ClubAccess actorAccess,
            ClubMember target,
            List<Long> clubPositionIds
    ) {
        Long clubId = actorAccess.club().getClubId();
        if (!clubId.equals(target.getClubId())) {
            throw new SemoException.ValidationException("다른 모임의 멤버 직책은 변경할 수 없습니다.");
        }
        if (!MEMBERSHIP_STATUS_ACTIVE.equals(target.getMembershipStatus())) {
            throw new SemoException.ValidationException("활동 중인 멤버의 직책만 변경할 수 있습니다.");
        }
        if (!clubPositionPermissionEvaluator.isRoleManagementEnabled(clubId)) {
            throw new SemoException.ValidationException("직책관리 기능이 활성화되지 않았습니다.");
        }
        if (!actorAccess.isAdmin()) {
            throw new SemoException.ForbiddenException("직책 배정 권한이 필요합니다.");
        }
        List<Long> normalizedPositionIds = normalizePositionIds(clubPositionIds);
        List<ClubPosition> positions = normalizedPositionIds.isEmpty()
                ? List.of()
                : clubPositionRepository.findAllById(normalizedPositionIds);
        if (positions.size() != normalizedPositionIds.size()
                || positions.stream().anyMatch(position -> !position.getClubId().equals(clubId))) {
            throw new SemoException.ValidationException("다른 모임의 직책은 할당할 수 없습니다.");
        }
        if (positions.stream().anyMatch(position -> !position.isActive())) {
            throw new SemoException.ValidationException("사용 종료된 직책은 새로 할당할 수 없습니다.");
        }
        Map<Long, ClubPosition> positionById = positions.stream()
                .collect(Collectors.toMap(ClubPosition::getClubPositionId, Function.identity()));

        List<ClubMemberPosition> existingAssignments = clubMemberPositionRepository.findByClubMemberId(target.getClubMemberId());
        Set<Long> requestedPositionIds = Set.copyOf(normalizedPositionIds);
        Set<Long> existingPositionIds = existingAssignments.stream()
                .map(ClubMemberPosition::getClubPositionId)
                .collect(Collectors.toSet());
        LocalDateTime now = currentTimestamp();
        Long actorClubProfileId = actorAccess.clubProfile().getClubProfileId();
        Long targetClubProfileId = resolveClubProfileId(target.getClubMemberId());
        List<ClubMemberPosition> assignmentsToRemove = existingAssignments.stream()
                .filter(assignment -> !requestedPositionIds.contains(assignment.getClubPositionId()))
                .toList();
        if (!assignmentsToRemove.isEmpty()) {
            for (ClubMemberPosition assignment : assignmentsToRemove) {
                closeOpenHistories(target.getClubMemberId(), assignment.getClubPositionId(), actorClubProfileId, now);
            }
            clubMemberPositionRepository.deleteAllInBatch(assignmentsToRemove);
        }

        List<Long> positionIdsToAdd = normalizedPositionIds.stream()
                .filter(clubPositionId -> !existingPositionIds.contains(clubPositionId))
                .toList();
        if (positionIdsToAdd.isEmpty()) {
            return;
        }

        for (Long clubPositionId : positionIdsToAdd) {
            clubMemberPositionRepository.save(ClubMemberPosition.builder()
                    .clubMemberId(target.getClubMemberId())
                    .clubPositionId(clubPositionId)
                    .assignedByClubProfileId(actorClubProfileId)
                    .assignedAt(now)
                    .build());
            ClubPosition position = positionById.get(clubPositionId);
            clubMemberPositionHistoryRepository.save(ClubMemberPositionHistory.builder()
                    .clubId(target.getClubId())
                    .clubMemberId(target.getClubMemberId())
                    .clubProfileId(targetClubProfileId)
                    .clubPositionId(clubPositionId)
                    .positionCodeSnapshot(position.getPositionCode())
                    .positionDisplayNameSnapshot(position.getDisplayName())
                    .startedAt(now)
                    .endedAt(null)
                    .assignedByClubProfileId(actorClubProfileId)
                    .endedByClubProfileId(null)
                    .deleted(false)
                    .build());
        }
    }

    private void closeOpenHistories(Long clubMemberId, Long clubPositionId, Long actorClubProfileId, LocalDateTime endedAt) {
        clubMemberPositionHistoryRepository.findOpenHistories(clubMemberId, clubPositionId)
                .forEach(history -> history.close(actorClubProfileId, endedAt));
    }

    private void closeOpenHistoriesForPosition(Long clubId, Long clubPositionId, Long actorClubProfileId, LocalDateTime endedAt) {
        clubMemberPositionHistoryRepository.findOpenHistoriesByPosition(clubId, clubPositionId)
                .forEach(history -> history.close(actorClubProfileId, endedAt));
    }

    private void retireCurrentAssignments(Long clubId, Long clubPositionId, Long actorClubProfileId) {
        LocalDateTime endedAt = currentTimestamp();
        closeOpenHistoriesForPosition(clubId, clubPositionId, actorClubProfileId, endedAt);
        clubMemberPositionRepository.deleteByClubPositionId(clubPositionId);
    }

    private LocalDateTime currentTimestamp() {
        return dateTimeProvider.now();
    }

    private ClubPositionDetailResponse buildPositionDetail(
            ClubAccessResolver.ClubAccess access,
            Long clubPositionId
    ) {
        PositionSnapshot snapshot = loadSnapshot(access.club().getClubId());
        ClubPositionSummaryResponse position = snapshot.positionSummaryById().get(clubPositionId);
        if (position == null) {
            throw new SemoException.ResourceNotFoundException("ClubPosition", "clubPositionId", clubPositionId);
        }
        return new ClubPositionDetailResponse(
                access.club().getClubId(),
                access.club().getName(),
                access.isAdmin(),
                true,
                true,
                true,
                true,
                position,
                snapshot.permissionGroups(),
                snapshot.positionTemplates()
        );
    }

    private Long resolveClubProfileId(Long clubMemberId) {
        return clubProfileRepository.findByClubMemberId(clubMemberId)
                .map(ClubProfile::getClubProfileId)
                .orElse(null);
    }

    private ClubPositionHistoryItemResponse toHistoryResponse(
            ClubMemberPositionHistory history,
            String memberDisplayName
    ) {
        return new ClubPositionHistoryItemResponse(
                history.getClubMemberPositionHistoryId(),
                history.getClubMemberId(),
                history.getClubProfileId(),
                StringUtils.hasText(memberDisplayName) ? memberDisplayName : "알 수 없는 멤버",
                history.getClubPositionId(),
                history.getPositionCodeSnapshot(),
                history.getPositionDisplayNameSnapshot(),
                formatDateTimeValue(history.getStartedAt()),
                formatDateTimeLabel(history.getStartedAt()),
                formatDateTimeValue(history.getEndedAt()),
                formatDateTimeLabel(history.getEndedAt()),
                history.getEndedAt() == null && !history.isDeleted(),
                history.isDeleted(),
                history.getDeleteReason()
        );
    }

    private PositionSnapshot loadSnapshot(Long clubId) {
        PermissionCatalogSnapshot permissionCatalogSnapshot = loadPermissionCatalogSnapshot(clubId);
        List<ClubPosition> positions = clubPositionRepository.findByClubIdOrderByDisplayNameAscClubPositionIdAsc(clubId);
        List<Long> positionIds = positions.stream().map(ClubPosition::getClubPositionId).toList();
        Map<Long, List<ClubPositionPermission>> permissionsByPositionId = positionIds.isEmpty()
                ? Map.of()
                : clubPositionPermissionRepository.findByClubPositionIdIn(positionIds).stream()
                .collect(Collectors.groupingBy(ClubPositionPermission::getClubPositionId));
        ClubPositionGrantService.GrantSnapshot grantSnapshot = clubPositionGrantService.loadSnapshot(positionIds);
        List<ClubMemberPosition> assignments = positionIds.isEmpty()
                ? List.of()
                : clubMemberPositionRepository.findByClubPositionIdIn(positionIds);
        Map<Long, Long> memberCountByPositionId = assignments.stream()
                .collect(Collectors.groupingBy(ClubMemberPosition::getClubPositionId, Collectors.counting()));
        int assignedMemberCount = (int) assignments.stream()
                .map(ClubMemberPosition::getClubMemberId)
                .distinct()
                .count();

        Map<Long, ClubPositionSummaryResponse> summaryById = new LinkedHashMap<>();
        List<ClubPositionSummaryResponse> summaries = positions.stream()
                .map(position -> {
                    List<String> permissionKeys = permissionsByPositionId.getOrDefault(position.getClubPositionId(), List.of()).stream()
                            .map(ClubPositionPermission::getPermissionKey)
                            .sorted()
                            .toList();
                    List<ClubPositionFeatureGrantResponse> featureGrants = clubPositionGrantService.resolveFeatureGrants(
                            position.getClubPositionId(),
                            permissionKeys,
                            grantSnapshot
                    ).stream()
                            .filter(grant -> permissionCatalogSnapshot.visibleFeatureKeys().contains(grant.featureKey()))
                            .toList();
                    ClubPositionSummaryResponse summary = new ClubPositionSummaryResponse(
                            position.getClubPositionId(),
                            position.getPositionCode(),
                            position.getDisplayName(),
                            position.getDescription(),
                            position.getIconName(),
                            position.getColorHex(),
                            position.isActive(),
                            position.getVersion() == null ? 0L : position.getVersion(),
                            permissionKeys.size(),
                            memberCountByPositionId.getOrDefault(position.getClubPositionId(), 0L).intValue(),
                            featureGrants,
                            permissionKeys
                    );
                    summaryById.put(position.getClubPositionId(), summary);
                    return summary;
                })
                .toList();

        return new PositionSnapshot(
                summaries,
                summaryById,
                permissionCatalogSnapshot.permissionGroups(),
                permissionCatalogSnapshot.positionTemplates(),
                assignedMemberCount
        );
    }

    private PermissionCatalogSnapshot loadPermissionCatalogSnapshot(Long clubId) {
        Set<String> visibleFeatureKeys = resolveVisibleFeatureKeys(clubId);
        List<FeatureCatalog> catalogs = featureCatalogRepository.findByActiveTrueOrderBySortOrderAscFeatureKeyAsc().stream()
                .filter(catalog -> visibleFeatureKeys.contains(catalog.getFeatureKey()))
                .toList();
        List<FeaturePermissionCatalog> activePermissions = featurePermissionCatalogRepository
                .findByActiveTrueOrderByFeatureKeyAscSortOrderAscPermissionKeyAsc();
        validateCapabilityCatalog(activePermissions, visibleFeatureKeys);
        Map<String, List<FeaturePermissionCatalog>> permissionsByFeatureKey = activePermissions.stream()
                .filter(permission -> visibleFeatureKeys.contains(permission.getFeatureKey()))
                .filter(permission -> !FEATURE_ROLE_MANAGEMENT.equals(permission.getFeatureKey()))
                .collect(Collectors.groupingBy(FeaturePermissionCatalog::getFeatureKey));

        List<ClubPermissionGroupResponse> groups = new ArrayList<>();
        for (FeatureCatalog catalog : catalogs) {
            ClubPositionAccessPolicy.FeatureAccessPolicy accessPolicy = clubPositionAccessPolicy
                    .findPolicy(catalog.getFeatureKey())
                    .orElse(null);
            if (accessPolicy == null) {
                continue;
            }
            Set<String> supportedPermissionKeys = accessPolicy.supportedPermissionKeys();
            List<FeaturePermissionCatalog> permissions = permissionsByFeatureKey
                    .getOrDefault(catalog.getFeatureKey(), List.of())
                    .stream()
                    .filter(permission -> supportedPermissionKeys.contains(permission.getPermissionKey()))
                    .toList();
            if (permissions.isEmpty()) {
                continue;
            }
            Set<String> activePermissionKeys = permissions.stream()
                    .map(FeaturePermissionCatalog::getPermissionKey)
                    .collect(Collectors.toSet());
            Set<String> sensitivePermissionKeys = Set.copyOf(accessPolicy.sensitivePermissionKeys());
            groups.add(new ClubPermissionGroupResponse(
                    catalog.getFeatureKey(),
                    resolvePermissionGroupDisplayName(catalog),
                    catalog.getDescription(),
                    catalog.getIconName(),
                    accessPolicy.policyVersion(),
                    accessPolicy.accessLevels().stream()
                            .filter(accessLevel -> activePermissionKeys.containsAll(accessLevel.permissionKeys()))
                            .map(accessLevel -> new ClubFeatureAccessLevelResponse(
                                    accessLevel.accessLevel(),
                                    accessLevel.displayName(),
                                    accessLevel.description(),
                                    accessPolicy.policyVersion(),
                                    accessLevel.permissionKeys()
                            ))
                            .toList(),
                    permissions.stream()
                            .map(permission -> new ClubPermissionItemResponse(
                                    permission.getPermissionKey(),
                                    permission.getDisplayName(),
                                    permission.getDescription(),
                                    permission.getOwnershipScope(),
                                    sensitivePermissionKeys.contains(permission.getPermissionKey())
                            ))
                            .toList()
            ));
        }
        List<ClubPositionTemplateResponse> positionTemplates = buildPositionTemplates(groups);
        Set<String> delegableFeatureKeys = groups.stream()
                .map(ClubPermissionGroupResponse::featureKey)
                .collect(Collectors.toSet());
        return new PermissionCatalogSnapshot(groups, delegableFeatureKeys, positionTemplates);
    }

    private List<ClubPositionTemplateResponse> buildPositionTemplates(List<ClubPermissionGroupResponse> groups) {
        Map<String, ClubPermissionGroupResponse> groupByFeatureKey = groups.stream()
                .collect(Collectors.toMap(ClubPermissionGroupResponse::featureKey, Function.identity()));

        return clubPositionAccessPolicy.positionTemplates().stream()
                .map(template -> {
                    Set<String> permissionKeys = new java.util.LinkedHashSet<>();
                    List<ClubPositionTemplateGrantResponse> featureGrants = new ArrayList<>();
                    int featureCount = 0;
                    for (ClubPositionAccessPolicy.TemplateGrant grant : template.grants()) {
                        ClubPermissionGroupResponse group = groupByFeatureKey.get(grant.featureKey());
                        if (group == null) {
                            continue;
                        }
                        ClubFeatureAccessLevelResponse level = group.accessLevels().stream()
                                .filter(candidate -> candidate.accessLevel().equals(grant.accessLevel()))
                                .findFirst()
                                .orElse(null);
                        if (level == null || level.permissionKeys().isEmpty()) {
                            continue;
                        }
                        permissionKeys.addAll(level.permissionKeys());
                        featureGrants.add(new ClubPositionTemplateGrantResponse(
                                grant.featureKey(),
                                grant.accessLevel()
                        ));
                        featureCount++;
                    }
                    return new ClubPositionTemplateResponse(
                            template.templateKey(),
                            template.displayName(),
                            template.description(),
                            template.iconName(),
                            template.colorHex(),
                            List.copyOf(featureGrants),
                            List.copyOf(permissionKeys),
                            featureCount
                    );
                })
                .filter(template -> template.featureCount() > 0)
                .toList();
    }

    private Set<String> resolveVisibleFeatureKeys(Long clubId) {
        Set<String> visibleFeatureKeys = clubFeatureRepository.findByClubId(clubId).stream()
                .filter(ClubFeature::isEnabled)
                .map(ClubFeature::getFeatureKey)
                .collect(Collectors.toSet());
        visibleFeatureKeys.add(FEATURE_ROLE_MANAGEMENT);
        return visibleFeatureKeys;
    }

    private ClubPositionGrantService.GrantChange replaceValidatedGrants(
            Long clubPositionId,
            List<ClubPositionFeatureGrantRequest> featureGrants,
            PermissionCatalogSnapshot snapshot,
            Long actorClubProfileId
    ) {
        validateRequestedGrantCatalog(featureGrants, snapshot);
        return clubPositionGrantService.replaceVisibleGrants(
                clubPositionId,
                featureGrants,
                snapshot.visibleFeatureKeys(),
                actorClubProfileId
        );
    }

    private void validateRequestedGrantCatalog(
            List<ClubPositionFeatureGrantRequest> featureGrants,
            PermissionCatalogSnapshot snapshot
    ) {
        if (featureGrants == null) {
            return;
        }
        Map<String, ClubPermissionGroupResponse> groupByFeatureKey = snapshot.permissionGroups().stream()
                .collect(Collectors.toMap(ClubPermissionGroupResponse::featureKey, Function.identity()));
        for (ClubPositionFeatureGrantRequest grant : featureGrants) {
            if (grant == null || !StringUtils.hasText(grant.featureKey())) {
                continue;
            }
            String featureKey = grant.featureKey().trim().toUpperCase(Locale.ROOT);
            ClubPermissionGroupResponse group = groupByFeatureKey.get(featureKey);
            if (group == null) {
                throw new SemoException.ValidationException("활성화되지 않은 기능이거나 위임할 수 없는 기능이 포함되어 있습니다.");
            }
            boolean previousPolicy = grant.policyVersion() != null
                    && grant.policyVersion() < group.policyVersion();
            if (previousPolicy) {
                continue;
            }
            String accessLevel = StringUtils.hasText(grant.accessLevel())
                    ? grant.accessLevel().trim().toUpperCase(Locale.ROOT)
                    : "";
            boolean supportedLevel = group.accessLevels().stream()
                    .anyMatch(level -> level.accessLevel().equals(accessLevel));
            if (!supportedLevel) {
                throw new SemoException.ValidationException("지원하지 않는 운영 수준이거나 현재 카탈로그에서 비활성화된 수준입니다.");
            }
            Set<String> activeSensitiveKeys = group.permissions().stream()
                    .filter(ClubPermissionItemResponse::sensitive)
                    .map(ClubPermissionItemResponse::permissionKey)
                    .collect(Collectors.toSet());
            Set<String> requestedSensitiveKeys = (grant.sensitivePermissionKeys() == null
                    ? List.<String>of()
                    : grant.sensitivePermissionKeys()).stream()
                    .filter(StringUtils::hasText)
                    .map(key -> key.trim().toUpperCase(Locale.ROOT))
                    .collect(Collectors.toSet());
            if (!activeSensitiveKeys.containsAll(requestedSensitiveKeys)) {
                throw new SemoException.ValidationException("현재 카탈로그에서 사용할 수 없는 추가 승인 권한입니다.");
            }
        }
    }

    private String resolvePermissionGroupDisplayName(FeatureCatalog catalog) {
        return switch (catalog.getFeatureKey()) {
            case FEATURE_NOTICE -> "게시판 공지";
            case FEATURE_POLL -> "캘린더 투표";
            case FEATURE_SCHEDULE_MANAGE -> "캘린더 일정";
            case FEATURE_TOURNAMENT_RECORD -> "대회 운영";
            case FEATURE_BRACKET -> "대진표 초안";
            case FEATURE_FINANCE -> "회비·정산";
            case FEATURE_TODO -> "업무 운영";
            default -> catalog.getDisplayName();
        };
    }

    private void validateCapabilityCatalog(
            List<FeaturePermissionCatalog> activePermissions,
            Set<String> visibleFeatureKeys
    ) {
        Map<String, FeaturePermissionCatalog> catalogByPermissionKey = activePermissions.stream()
                .collect(Collectors.toMap(FeaturePermissionCatalog::getPermissionKey, Function.identity()));
        List<String> missingOrMismatchedKeys = clubPositionAccessPolicy.supportedCapabilities().stream()
                .filter(capability -> visibleFeatureKeys.contains(capability.featureKey()))
                .filter(capability -> {
                    FeaturePermissionCatalog catalog = catalogByPermissionKey.get(capability.permissionKey());
                    return catalog == null || !capability.featureKey().equals(catalog.getFeatureKey());
                })
                .map(ClubCapability::permissionKey)
                .sorted()
                .toList();
        if (!missingOrMismatchedKeys.isEmpty()) {
            throw new IllegalStateException(
                    "직책 권한 정책과 활성 권한 카탈로그가 일치하지 않습니다: " + String.join(", ", missingOrMismatchedKeys)
            );
        }
    }

    private ClubPosition requirePosition(Long clubId, Long clubPositionId) {
        return clubPositionRepository.findByClubPositionIdAndClubId(clubPositionId, clubId)
                .orElseThrow(() -> new SemoException.ResourceNotFoundException("ClubPosition", "clubPositionId", clubPositionId));
    }

    private void requireMatchingVersion(ClubPosition position, Long expectedVersion) {
        long currentVersion = position.getVersion() == null ? 0L : position.getVersion();
        if (expectedVersion == null || expectedVersion != currentVersion) {
            throw stalePositionConflict();
        }
    }

    private SemoException.ConflictException stalePositionConflict() {
        return new SemoException.ConflictException(
                "다른 관리자가 직책을 먼저 수정했습니다. 최신 내용을 다시 불러온 뒤 저장해주세요."
        );
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
        String normalized = trimmed.startsWith("#") ? trimmed : "#" + trimmed;
        if (!normalized.matches("#[0-9A-Fa-f]{6}")) {
            throw new SemoException.ValidationException("직책 색상은 6자리 HEX 형식이어야 합니다.");
        }
        return normalized.toUpperCase(Locale.ROOT);
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

    private String formatDateTimeValue(LocalDateTime value) {
        return value == null ? null : value.format(DATE_TIME_VALUE_FORMATTER);
    }

    private String formatDateTimeLabel(LocalDateTime value) {
        return value == null ? null : value.format(DATE_TIME_LABEL_FORMATTER);
    }

    private record PositionSnapshot(
            List<ClubPositionSummaryResponse> positionSummaries,
            Map<Long, ClubPositionSummaryResponse> positionSummaryById,
            List<ClubPermissionGroupResponse> permissionGroups,
            List<ClubPositionTemplateResponse> positionTemplates,
            int assignedMemberCount
    ) {
    }

    private record PermissionCatalogSnapshot(
            List<ClubPermissionGroupResponse> permissionGroups,
            Set<String> visibleFeatureKeys,
            List<ClubPositionTemplateResponse> positionTemplates
    ) {
    }
}
