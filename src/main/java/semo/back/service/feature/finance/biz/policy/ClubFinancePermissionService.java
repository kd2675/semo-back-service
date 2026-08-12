package semo.back.service.feature.finance.biz.policy;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import semo.back.service.feature.club.biz.policy.ClubAccessResolver;
import semo.back.service.feature.clubfeature.biz.ClubFeatureService;
import semo.back.service.feature.position.biz.ClubCapability;
import semo.back.service.feature.position.biz.ClubPositionPermissionEvaluator;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubFinancePermissionService {
    public static final String FEATURE_FINANCE = "FINANCE";
    public static final ClubCapability PERMISSION_FINANCE_BILLING_ISSUE = ClubCapability.FINANCE_BILLING_ISSUE;
    public static final ClubCapability PERMISSION_FINANCE_REQUEST_REVIEW = ClubCapability.FINANCE_REQUEST_REVIEW;
    public static final ClubCapability PERMISSION_FINANCE_EXPENSE_CREATE = ClubCapability.FINANCE_EXPENSE_CREATE;
    public static final ClubCapability PERMISSION_FINANCE_PAYMENT_UPDATE = ClubCapability.FINANCE_PAYMENT_UPDATE;

    private final ClubFeatureService clubFeatureService;
    private final ClubPositionPermissionEvaluator clubPositionPermissionEvaluator;

    public boolean isFinanceEnabled(Long clubId) {
        return clubFeatureService.isFeatureEnabled(clubId, FEATURE_FINANCE);
    }

    public boolean canViewAdminFinance(ClubAccessResolver.ClubAccess access) {
        if (access.isAdmin()) {
            return true;
        }
        return hasRolePermission(access, ClubCapability.FINANCE_VIEW);
    }

    public boolean canIssueFinance(ClubAccessResolver.ClubAccess access) {
        return access.isAdmin() || hasRolePermission(
                access,
                ClubCapability.FINANCE_BILLING_ISSUE,
                ClubCapability.FINANCE_REQUEST_REVIEW,
                ClubCapability.FINANCE_EXPENSE_CREATE
        );
    }

    public boolean canManageBilling(ClubAccessResolver.ClubAccess access) {
        if (access.isAdmin()) {
            return true;
        }
        return hasRolePermission(access, ClubCapability.FINANCE_BILLING_ISSUE);
    }

    public boolean canReviewRequests(ClubAccessResolver.ClubAccess access) {
        if (access.isAdmin()) {
            return true;
        }
        return hasRolePermission(access, ClubCapability.FINANCE_REQUEST_REVIEW);
    }

    public boolean canCreateExpenses(ClubAccessResolver.ClubAccess access) {
        if (access.isAdmin()) {
            return true;
        }
        return hasRolePermission(access, ClubCapability.FINANCE_EXPENSE_CREATE);
    }

    public boolean canMarkPaid(ClubAccessResolver.ClubAccess access) {
        if (access.isAdmin()) {
            return true;
        }
        return hasRolePermission(access, ClubCapability.FINANCE_PAYMENT_UPDATE);
    }

    public boolean canMarkWaived(ClubAccessResolver.ClubAccess access) {
        if (access.isAdmin()) {
            return true;
        }
        return hasRolePermission(access, ClubCapability.FINANCE_PAYMENT_UPDATE);
    }

    public boolean canUpdatePayments(ClubAccessResolver.ClubAccess access) {
        return access.isAdmin() || hasRolePermission(access, ClubCapability.FINANCE_PAYMENT_UPDATE);
    }

    public boolean canExport(ClubAccessResolver.ClubAccess access) {
        return access.isAdmin() || hasRolePermission(access, ClubCapability.FINANCE_EXPORT);
    }

    public boolean canClosePeriods(ClubAccessResolver.ClubAccess access) {
        return access.isAdmin() || hasRolePermission(access, ClubCapability.FINANCE_PERIOD_CLOSE);
    }

    private boolean hasRolePermission(ClubAccessResolver.ClubAccess access, ClubCapability... capabilities) {
        return clubPositionPermissionEvaluator.hasAnyPermission(access, capabilities);
    }
}
