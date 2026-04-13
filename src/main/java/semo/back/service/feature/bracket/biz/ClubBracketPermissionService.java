package semo.back.service.feature.bracket.biz;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import semo.back.service.feature.club.biz.policy.ClubAccessResolver;
import semo.back.service.feature.clubfeature.biz.ClubFeatureService;
import semo.back.service.feature.position.biz.ClubPositionPermissionEvaluator;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubBracketPermissionService {
    public static final String FEATURE_BRACKET = "BRACKET";
    public static final String PERMISSION_BRACKET_CREATE = "BRACKET_CREATE";
    public static final String PERMISSION_BRACKET_UPDATE_SELF = "BRACKET_UPDATE_SELF";
    public static final String PERMISSION_BRACKET_REVIEW = "BRACKET_REVIEW";
    public static final String PERMISSION_BRACKET_DELETE_ANY = "BRACKET_DELETE_ANY";

    private final ClubFeatureService clubFeatureService;
    private final ClubPositionPermissionEvaluator clubPositionPermissionEvaluator;

    public boolean isBracketEnabled(Long clubId) {
        return clubFeatureService.isFeatureEnabled(clubId, FEATURE_BRACKET);
    }

    public boolean canCreateBracket(ClubAccessResolver.ClubAccess access) {
        if (access.isAdmin()) {
            return true;
        }
        return hasRolePermission(access, PERMISSION_BRACKET_CREATE);
    }

    public boolean canEditOwnBracket(ClubAccessResolver.ClubAccess access, Long authorClubProfileId) {
        if (!access.clubProfile().getClubProfileId().equals(authorClubProfileId)) {
            return false;
        }
        if (access.isAdmin()) {
            return true;
        }
        return hasRolePermission(access, PERMISSION_BRACKET_UPDATE_SELF);
    }

    public boolean canReviewBracket(ClubAccessResolver.ClubAccess access) {
        if (access.isAdmin()) {
            return true;
        }
        return hasRolePermission(access, PERMISSION_BRACKET_REVIEW);
    }

    public boolean canDeleteBracket(ClubAccessResolver.ClubAccess access) {
        if (access.isAdmin()) {
            return true;
        }
        return hasRolePermission(access, PERMISSION_BRACKET_DELETE_ANY);
    }

    private boolean hasRolePermission(ClubAccessResolver.ClubAccess access, String permissionKey) {
        return clubPositionPermissionEvaluator.isRoleManagementEnabled(access.club().getClubId())
                && clubPositionPermissionEvaluator.hasPermission(access, permissionKey);
    }
}
