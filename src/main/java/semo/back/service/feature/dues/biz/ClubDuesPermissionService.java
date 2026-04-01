package semo.back.service.feature.dues.biz;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import semo.back.service.feature.club.biz.ClubAccessResolver;
import semo.back.service.feature.clubfeature.biz.ClubFeatureService;
import semo.back.service.feature.position.biz.ClubPositionPermissionEvaluator;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubDuesPermissionService {
    public static final String FEATURE_DUES = "DUES";
    public static final String PERMISSION_DUES_VIEW = "DUES_VIEW";
    public static final String PERMISSION_DUES_ISSUE = "DUES_ISSUE";
    public static final String PERMISSION_DUES_MARK_PAID = "DUES_MARK_PAID";
    public static final String PERMISSION_DUES_MARK_WAIVED = "DUES_MARK_WAIVED";

    private final ClubFeatureService clubFeatureService;
    private final ClubPositionPermissionEvaluator clubPositionPermissionEvaluator;

    public boolean isDuesEnabled(Long clubId) {
        return clubFeatureService.isFeatureEnabled(clubId, FEATURE_DUES);
    }

    public boolean canViewAdminDues(ClubAccessResolver.ClubAccess access) {
        if (access.isAdmin()) {
            return true;
        }
        return hasRolePermission(access, PERMISSION_DUES_VIEW);
    }

    public boolean canIssueDues(ClubAccessResolver.ClubAccess access) {
        if (access.isAdmin()) {
            return true;
        }
        return hasRolePermission(access, PERMISSION_DUES_ISSUE);
    }

    public boolean canMarkPaid(ClubAccessResolver.ClubAccess access) {
        if (access.isAdmin()) {
            return true;
        }
        return hasRolePermission(access, PERMISSION_DUES_MARK_PAID);
    }

    public boolean canMarkWaived(ClubAccessResolver.ClubAccess access) {
        if (access.isAdmin()) {
            return true;
        }
        return hasRolePermission(access, PERMISSION_DUES_MARK_WAIVED);
    }

    private boolean hasRolePermission(ClubAccessResolver.ClubAccess access, String permissionKey) {
        return clubPositionPermissionEvaluator.isRoleManagementEnabled(access.club().getClubId())
                && clubPositionPermissionEvaluator.hasPermission(access, permissionKey);
    }
}
