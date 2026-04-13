package semo.back.service.feature.tournament.biz.policy;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import semo.back.service.feature.club.biz.policy.ClubAccessResolver;
import semo.back.service.feature.clubfeature.biz.ClubFeatureService;
import semo.back.service.feature.position.biz.ClubPositionPermissionEvaluator;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubTournamentPermissionService {
    public static final String FEATURE_TOURNAMENT_RECORD = "TOURNAMENT_RECORD";
    public static final String PERMISSION_TOURNAMENT_CREATE = "TOURNAMENT_RECORD_CREATE";
    public static final String PERMISSION_TOURNAMENT_UPDATE_SELF = "TOURNAMENT_RECORD_UPDATE_SELF";
    public static final String PERMISSION_TOURNAMENT_PIN = "TOURNAMENT_RECORD_PIN";
    public static final String PERMISSION_TOURNAMENT_REVIEW = "TOURNAMENT_RECORD_REVIEW";
    public static final String PERMISSION_TOURNAMENT_DELETE_ANY = "TOURNAMENT_RECORD_DELETE_ANY";

    private final ClubFeatureService clubFeatureService;
    private final ClubPositionPermissionEvaluator clubPositionPermissionEvaluator;

    public boolean isTournamentEnabled(Long clubId) {
        return clubFeatureService.isFeatureEnabled(clubId, FEATURE_TOURNAMENT_RECORD);
    }

    public boolean canCreateTournament(ClubAccessResolver.ClubAccess access) {
        if (access.isAdmin()) {
            return true;
        }
        return hasRolePermission(access, PERMISSION_TOURNAMENT_CREATE);
    }

    public TournamentActionPermission getActionPermission(
            ClubAccessResolver.ClubAccess access,
            Long authorClubProfileId
    ) {
        if (access.isAdmin()) {
            return new TournamentActionPermission(true, true, true);
        }
        boolean isAuthor = access.clubProfile().getClubProfileId().equals(authorClubProfileId);
        if (!isAuthor) {
            return new TournamentActionPermission(false, false, false);
        }
        return new TournamentActionPermission(
                hasRolePermission(access, PERMISSION_TOURNAMENT_UPDATE_SELF),
                hasRolePermission(access, PERMISSION_TOURNAMENT_UPDATE_SELF),
                false
        );
    }

    public boolean canPinTournament(ClubAccessResolver.ClubAccess access) {
        if (access.isAdmin()) {
            return true;
        }
        return hasRolePermission(access, PERMISSION_TOURNAMENT_PIN);
    }

    public boolean canReviewTournament(ClubAccessResolver.ClubAccess access) {
        if (access.isAdmin()) {
            return true;
        }
        return hasRolePermission(access, PERMISSION_TOURNAMENT_REVIEW);
    }

    public boolean canDeleteTournament(ClubAccessResolver.ClubAccess access) {
        if (access.isAdmin()) {
            return true;
        }
        return hasRolePermission(access, PERMISSION_TOURNAMENT_DELETE_ANY);
    }

    private boolean hasRolePermission(ClubAccessResolver.ClubAccess access, String permissionKey) {
        return clubPositionPermissionEvaluator.isRoleManagementEnabled(access.club().getClubId())
                && clubPositionPermissionEvaluator.hasPermission(access, permissionKey);
    }

    public record TournamentActionPermission(
            boolean canEdit,
            boolean canCancel,
            boolean canDelete
    ) {
    }
}
