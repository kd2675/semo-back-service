package semo.back.service.feature.notice.biz;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import semo.back.service.feature.club.biz.policy.ClubAccessResolver;
import semo.back.service.feature.position.biz.ClubPositionPermissionEvaluator;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubNoticePermissionService {
    private final ClubPositionPermissionEvaluator clubPositionPermissionEvaluator;

    public boolean canCreateNotice(ClubAccessResolver.ClubAccess access) {
        if (access.isAdmin()) {
            return true;
        }
        return clubPositionPermissionEvaluator.isRoleManagementEnabled(access.club().getClubId())
                && clubPositionPermissionEvaluator.hasPermission(access, ClubPositionPermissionEvaluator.PERMISSION_NOTICE_CREATE);
    }

    public NoticeActionPermission getActionPermission(
            ClubAccessResolver.ClubAccess access,
            Long authorClubProfileId
    ) {
        if (access.isAdmin()) {
            return new NoticeActionPermission(true, true);
        }
        if (!access.clubProfile().getClubProfileId().equals(authorClubProfileId)) {
            return new NoticeActionPermission(false, false);
        }
        return new NoticeActionPermission(
                clubPositionPermissionEvaluator.isRoleManagementEnabled(access.club().getClubId())
                        && clubPositionPermissionEvaluator.hasPermission(access, ClubPositionPermissionEvaluator.PERMISSION_NOTICE_UPDATE_SELF),
                clubPositionPermissionEvaluator.isRoleManagementEnabled(access.club().getClubId())
                        && clubPositionPermissionEvaluator.hasPermission(access, ClubPositionPermissionEvaluator.PERMISSION_NOTICE_DELETE_SELF)
        );
    }

    public boolean canManageNotice(ClubAccessResolver.ClubAccess access, Long authorClubProfileId) {
        return getActionPermission(access, authorClubProfileId).canManage();
    }

    public record NoticeActionPermission(
            boolean canEdit,
            boolean canDelete
    ) {
        public boolean canManage() {
            return canEdit || canDelete;
        }
    }
}
