package semo.back.service.feature.clubfeature.biz;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import semo.back.service.database.pub.entity.ClubMorePreference;
import semo.back.service.database.pub.repository.ClubMorePreferenceRepository;
import semo.back.service.feature.club.biz.policy.ClubAccessResolver;
import semo.back.service.feature.clubfeature.vo.ClubFeatureResponse;
import semo.back.service.feature.clubfeature.vo.ClubMoreFeatureStatusResponse;
import semo.back.service.feature.clubfeature.vo.ClubMoreSummaryResponse;
import semo.back.service.feature.position.biz.ClubPositionPermissionEvaluator;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubMoreSummaryService {
    private static final Map<String, Set<String>> ADMIN_TOOL_VIEW_PERMISSIONS = adminToolViewPermissions();

    private final ClubAccessResolver clubAccessResolver;
    private final ClubFeatureService clubFeatureService;
    private final ClubPositionPermissionEvaluator clubPositionPermissionEvaluator;
    private final ClubMorePreferenceRepository clubMorePreferenceRepository;
    private final ClubMoreWorkQueueService clubMoreWorkQueueService;

    public ClubMoreSummaryResponse getSummary(Long clubId, String userKey) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        List<ClubFeatureResponse> features = clubFeatureService.getClubFeatures(clubId, userKey);
        Set<String> permissionKeys = access.isAdmin()
                ? Set.of()
                : clubPositionPermissionEvaluator.getPermissionKeysForMember(
                        clubId,
                        access.membership().getClubMemberId()
                );
        List<String> adminToolFeatureKeys = features.stream()
                .filter(ClubFeatureResponse::enabled)
                .map(ClubFeatureResponse::featureKey)
                .filter(featureKey -> access.isAdmin() || hasAdminToolViewPermission(featureKey, permissionKeys))
                .toList();
        Set<String> adminToolFeatureKeySet = Set.copyOf(adminToolFeatureKeys);
        Map<String, ClubMorePreference> preferencesByFeatureKey = clubMorePreferenceRepository
                .findByClubIdAndClubProfileId(clubId, access.clubProfile().getClubProfileId()).stream()
                .collect(Collectors.toMap(ClubMorePreference::getFeatureKey, Function.identity()));
        Map<String, ClubMoreWorkQueueService.FeatureQueueCounts> queueCounts =
                clubMoreWorkQueueService.getQueueCounts(
                        clubId,
                        access.clubProfile().getClubProfileId(),
                        features,
                        adminToolFeatureKeySet
                );
        List<ClubMoreFeatureStatusResponse> featureStatuses = features.stream()
                .filter(ClubFeatureResponse::enabled)
                .map(feature -> toFeatureStatus(
                        feature,
                        adminToolFeatureKeySet,
                        preferencesByFeatureKey.get(feature.featureKey()),
                        queueCounts.getOrDefault(
                                feature.featureKey(),
                                ClubMoreWorkQueueService.FeatureQueueCounts.empty()
                        )
                ))
                .toList();

        return new ClubMoreSummaryResponse(
                access.club().getClubId(),
                access.club().getName(),
                access.isAdmin(),
                permissionKeys.stream().sorted().toList(),
                adminToolFeatureKeys,
                featureStatuses.stream().mapToInt(ClubMoreFeatureStatusResponse::userPendingCount).sum(),
                featureStatuses.stream().mapToInt(ClubMoreFeatureStatusResponse::userOverdueCount).sum(),
                featureStatuses.stream().mapToInt(ClubMoreFeatureStatusResponse::adminPendingCount).sum(),
                featureStatuses.stream().mapToInt(ClubMoreFeatureStatusResponse::adminOverdueCount).sum(),
                featureStatuses,
                features
        );
    }

    private ClubMoreFeatureStatusResponse toFeatureStatus(
            ClubFeatureResponse feature,
            Set<String> adminToolFeatureKeys,
            ClubMorePreference preference,
            ClubMoreWorkQueueService.FeatureQueueCounts queueCounts
    ) {
        boolean userAccessible = !"ADMIN_ONLY".equals(feature.navigationScope());
        boolean adminAccessible = adminToolFeatureKeys.contains(feature.featureKey());
        return new ClubMoreFeatureStatusResponse(
                feature.featureKey(),
                userAccessible,
                adminAccessible,
                userAccessible ? queueCounts.userPendingCount() : 0,
                userAccessible ? queueCounts.userOverdueCount() : 0,
                adminAccessible ? queueCounts.adminPendingCount() : 0,
                adminAccessible ? queueCounts.adminOverdueCount() : 0,
                preference != null && preference.isFavorite(),
                preference == null ? null : preference.getLastUsedAt()
        );
    }

    private boolean hasAdminToolViewPermission(String featureKey, Set<String> permissionKeys) {
        return ADMIN_TOOL_VIEW_PERMISSIONS.getOrDefault(featureKey, Set.of()).stream()
                .anyMatch(permissionKeys::contains);
    }

    private static Map<String, Set<String>> adminToolViewPermissions() {
        Map<String, Set<String>> permissions = new LinkedHashMap<>();
        permissions.put("FINANCE", Set.of(ClubPositionPermissionEvaluator.PERMISSION_FINANCE_VIEW));
        permissions.put("TODO", Set.of(ClubPositionPermissionEvaluator.PERMISSION_TODO_VIEW));
        permissions.put("ROLE_MANAGEMENT", Set.of(ClubPositionPermissionEvaluator.PERMISSION_ROLE_MANAGEMENT_VIEW));
        permissions.put("TOURNAMENT_RECORD", Set.of(
                ClubPositionPermissionEvaluator.PERMISSION_TOURNAMENT_REVIEW,
                ClubPositionPermissionEvaluator.PERMISSION_TOURNAMENT_DELETE_ANY
        ));
        permissions.put("BRACKET", Set.of(
                ClubPositionPermissionEvaluator.PERMISSION_BRACKET_REVIEW,
                ClubPositionPermissionEvaluator.PERMISSION_BRACKET_DELETE_ANY
        ));
        permissions.put("ATTENDANCE", Set.of(
                ClubPositionPermissionEvaluator.PERMISSION_ATTENDANCE_MANAGE
        ));
        permissions.put("HANDOVER", Set.of(
                ClubPositionPermissionEvaluator.PERMISSION_HANDOVER_VIEW,
                ClubPositionPermissionEvaluator.PERMISSION_HANDOVER_MANAGE
        ));
        return Map.copyOf(permissions);
    }
}
