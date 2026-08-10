package semo.back.service.feature.clubfeature.biz;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import semo.back.service.feature.club.biz.policy.ClubAccessResolver;
import semo.back.service.feature.clubfeature.vo.ClubFeatureResponse;
import semo.back.service.feature.clubfeature.vo.ClubMoreSummaryResponse;
import semo.back.service.feature.position.biz.ClubPositionPermissionEvaluator;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubMoreSummaryService {
    private static final Map<String, Set<String>> ADMIN_TOOL_VIEW_PERMISSIONS = adminToolViewPermissions();

    private final ClubAccessResolver clubAccessResolver;
    private final ClubFeatureService clubFeatureService;
    private final ClubPositionPermissionEvaluator clubPositionPermissionEvaluator;

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

        return new ClubMoreSummaryResponse(
                access.club().getClubId(),
                access.club().getName(),
                access.isAdmin(),
                permissionKeys.stream().sorted().toList(),
                adminToolFeatureKeys,
                features
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
        return Map.copyOf(permissions);
    }
}
