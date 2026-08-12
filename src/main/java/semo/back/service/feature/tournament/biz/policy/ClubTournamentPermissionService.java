package semo.back.service.feature.tournament.biz.policy;

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
public class ClubTournamentPermissionService {
    public static final String FEATURE_TOURNAMENT_RECORD = "TOURNAMENT_RECORD";

    private final ClubFeatureService clubFeatureService;
    private final ClubPositionPermissionEvaluator clubPositionPermissionEvaluator;

    public boolean isTournamentEnabled(Long clubId) {
        return clubFeatureService.isFeatureEnabled(clubId, FEATURE_TOURNAMENT_RECORD);
    }

    public boolean canCreateTournament(ClubAccessResolver.ClubAccess access) {
        if (access.isAdmin()) {
            return true;
        }
        return hasRolePermission(access, ClubCapability.TOURNAMENT_RECORD_CREATE);
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
                hasRolePermission(access, ClubCapability.TOURNAMENT_RECORD_UPDATE_SELF),
                hasRolePermission(access, ClubCapability.TOURNAMENT_RECORD_UPDATE_SELF),
                false
        );
    }

    public boolean canPinTournament(ClubAccessResolver.ClubAccess access) {
        if (access.isAdmin()) {
            return true;
        }
        return hasRolePermission(access, ClubCapability.TOURNAMENT_RECORD_PIN);
    }

    public boolean canReviewTournament(ClubAccessResolver.ClubAccess access) {
        if (access.isAdmin()) {
            return true;
        }
        return hasRolePermission(access, ClubCapability.TOURNAMENT_RECORD_REVIEW);
    }

    public boolean canDeleteTournament(ClubAccessResolver.ClubAccess access) {
        if (access.isAdmin()) {
            return true;
        }
        return hasRolePermission(access, ClubCapability.TOURNAMENT_RECORD_DELETE_ANY);
    }

    private boolean hasRolePermission(ClubAccessResolver.ClubAccess access, ClubCapability capability) {
        return clubPositionPermissionEvaluator.hasPermission(access, capability);
    }

    public record TournamentActionPermission(
            boolean canEdit,
            boolean canCancel,
            boolean canDelete
    ) {
    }
}
