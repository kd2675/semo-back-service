package semo.back.service.feature.poll.biz;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import semo.back.service.feature.club.biz.policy.ClubAccessResolver;
import semo.back.service.feature.position.biz.ClubPositionPermissionEvaluator;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubPollPermissionService {
    private final ClubPositionPermissionEvaluator clubPositionPermissionEvaluator;

    public boolean canCreatePoll(ClubAccessResolver.ClubAccess access) {
        if (access.isAdmin()) {
            return true;
        }
        return clubPositionPermissionEvaluator.isRoleManagementEnabled(access.club().getClubId())
                && clubPositionPermissionEvaluator.hasPermission(access, ClubPositionPermissionEvaluator.PERMISSION_POLL_CREATE);
    }

    public PollActionPermission getActionPermission(
            ClubAccessResolver.ClubAccess access,
            Long authorClubProfileId
    ) {
        if (access.isAdmin()) {
            return new PollActionPermission(true, true);
        }
        if (!access.clubProfile().getClubProfileId().equals(authorClubProfileId)) {
            return new PollActionPermission(false, false);
        }
        return new PollActionPermission(
                clubPositionPermissionEvaluator.isRoleManagementEnabled(access.club().getClubId())
                        && clubPositionPermissionEvaluator.hasPermission(access, ClubPositionPermissionEvaluator.PERMISSION_POLL_UPDATE_SELF),
                clubPositionPermissionEvaluator.isRoleManagementEnabled(access.club().getClubId())
                        && clubPositionPermissionEvaluator.hasPermission(access, ClubPositionPermissionEvaluator.PERMISSION_POLL_DELETE_SELF)
        );
    }

    public record PollActionPermission(
            boolean canEdit,
            boolean canDelete
    ) {
        public boolean canManage() {
            return canEdit || canDelete;
        }
    }
}
