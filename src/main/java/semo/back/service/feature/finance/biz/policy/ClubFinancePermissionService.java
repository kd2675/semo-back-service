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
    public static final String PERMISSION_FINANCE_BILLING_ISSUE = "FINANCE_BILLING_ISSUE";
    public static final String PERMISSION_FINANCE_REQUEST_REVIEW = "FINANCE_REQUEST_REVIEW";
    public static final String PERMISSION_FINANCE_EXPENSE_CREATE = "FINANCE_EXPENSE_CREATE";
    public static final String PERMISSION_FINANCE_PAYMENT_UPDATE = "FINANCE_PAYMENT_UPDATE";
    public static final String PERMISSION_FINANCE_EXPORT = "FINANCE_EXPORT";
    public static final String PERMISSION_FINANCE_PERIOD_CLOSE = "FINANCE_PERIOD_CLOSE";

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
        return canManageBilling(access) || canReviewRequests(access) || canCreateExpenses(access);
    }

    public boolean canManageBilling(ClubAccessResolver.ClubAccess access) {
        if (access.isAdmin()) {
            return true;
        }
        return hasRolePermission(access, PERMISSION_FINANCE_BILLING_ISSUE);
    }

    public boolean canReviewRequests(ClubAccessResolver.ClubAccess access) {
        if (access.isAdmin()) {
            return true;
        }
        return hasRolePermission(access, PERMISSION_FINANCE_REQUEST_REVIEW);
    }

    public boolean canCreateExpenses(ClubAccessResolver.ClubAccess access) {
        if (access.isAdmin()) {
            return true;
        }
        return hasRolePermission(access, PERMISSION_FINANCE_EXPENSE_CREATE);
    }

    public boolean canMarkPaid(ClubAccessResolver.ClubAccess access) {
        if (access.isAdmin()) {
            return true;
        }
        return hasRolePermission(access, PERMISSION_FINANCE_PAYMENT_UPDATE);
    }

    public boolean canMarkWaived(ClubAccessResolver.ClubAccess access) {
        if (access.isAdmin()) {
            return true;
        }
        return hasRolePermission(access, PERMISSION_FINANCE_PAYMENT_UPDATE);
    }

    public boolean canUpdatePayments(ClubAccessResolver.ClubAccess access) {
        return canMarkPaid(access) || canMarkWaived(access);
    }

    public boolean canExport(ClubAccessResolver.ClubAccess access) {
        return access.isAdmin() || hasRolePermission(access, PERMISSION_FINANCE_EXPORT);
    }

    public boolean canClosePeriods(ClubAccessResolver.ClubAccess access) {
        return access.isAdmin() || hasRolePermission(access, PERMISSION_FINANCE_PERIOD_CLOSE);
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
