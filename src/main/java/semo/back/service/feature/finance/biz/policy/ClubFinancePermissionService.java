package semo.back.service.feature.finance.biz.policy;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import semo.back.service.feature.club.biz.policy.ClubAccessResolver;
import semo.back.service.feature.clubfeature.biz.ClubFeatureService;
import semo.back.service.feature.position.biz.ClubPositionPermissionEvaluator;

import java.util.LinkedHashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubFinancePermissionService {
    public static final String FEATURE_FINANCE = "FINANCE";
    public static final String PERMISSION_FINANCE_VIEW = "FINANCE_VIEW";
    public static final String PERMISSION_FINANCE_ISSUE = "FINANCE_ISSUE";
    public static final String PERMISSION_FINANCE_MARK_PAID = "FINANCE_MARK_PAID";
    public static final String PERMISSION_FINANCE_MARK_WAIVED = "FINANCE_MARK_WAIVED";

    private final ClubFeatureService clubFeatureService;
    private final ClubPositionPermissionEvaluator clubPositionPermissionEvaluator;

    public boolean isFinanceEnabled(Long clubId) {
        return clubFeatureService.isFeatureEnabled(clubId, FEATURE_FINANCE);
    }

    public boolean canViewAdminFinance(ClubAccessResolver.ClubAccess access) {
        if (access.isAdmin()) {
            return true;
        }
        return hasRolePermission(access, PERMISSION_FINANCE_VIEW);
    }

    public boolean canIssueFinance(ClubAccessResolver.ClubAccess access) {
        if (access.isAdmin()) {
            return true;
        }
        return hasRolePermission(access, PERMISSION_FINANCE_ISSUE);
    }

    public boolean canMarkPaid(ClubAccessResolver.ClubAccess access) {
        if (access.isAdmin()) {
            return true;
        }
        return hasRolePermission(access, PERMISSION_FINANCE_MARK_PAID);
    }

    public boolean canMarkWaived(ClubAccessResolver.ClubAccess access) {
        if (access.isAdmin()) {
            return true;
        }
        return hasRolePermission(access, PERMISSION_FINANCE_MARK_WAIVED);
    }

    private boolean hasRolePermission(ClubAccessResolver.ClubAccess access, String... permissionKeys) {
        if (!clubPositionPermissionEvaluator.isRoleManagementEnabled(access.club().getClubId())) {
            return false;
        }
        Set<String> grantedPermissionKeys = clubPositionPermissionEvaluator.getPermissionKeysForMember(
                access.club().getClubId(),
                access.membership().getClubMemberId()
        );
        Set<String> normalizedCandidates = new LinkedHashSet<>();
        for (String permissionKey : permissionKeys) {
            if (permissionKey != null) {
                normalizedCandidates.add(permissionKey);
            }
        }
        return normalizedCandidates.stream().anyMatch(grantedPermissionKeys::contains);
    }
}
