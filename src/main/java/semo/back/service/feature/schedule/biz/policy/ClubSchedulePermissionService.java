package semo.back.service.feature.schedule.biz.policy;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import semo.back.service.feature.club.biz.policy.ClubAccessResolver;
import semo.back.service.feature.position.biz.ClubPositionPermissionEvaluator;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubSchedulePermissionService {
    private final ClubPositionPermissionEvaluator clubPositionPermissionEvaluator;

    public boolean canCreateSchedule(ClubAccessResolver.ClubAccess access) {
        if (access.isAdmin()) {
            return true;
        }
        return clubPositionPermissionEvaluator.isRoleManagementEnabled(access.club().getClubId())
                && clubPositionPermissionEvaluator.hasPermission(access, ClubPositionPermissionEvaluator.PERMISSION_SCHEDULE_CREATE);
    }

    public boolean canManageAttendance(ClubAccessResolver.ClubAccess access) {
        return access.isAdmin()
                || clubPositionPermissionEvaluator.hasPermission(
                        access,
                        ClubPositionPermissionEvaluator.PERMISSION_ATTENDANCE_MANAGE
                );
    }

    public ScheduleEventActionPermission getActionPermission(
            ClubAccessResolver.ClubAccess access,
            Long authorClubProfileId
    ) {
        if (access.isAdmin()) {
            return new ScheduleEventActionPermission(true, true);
        }
        if (!access.clubProfile().getClubProfileId().equals(authorClubProfileId)) {
            return new ScheduleEventActionPermission(false, false);
        }
        return new ScheduleEventActionPermission(
                clubPositionPermissionEvaluator.isRoleManagementEnabled(access.club().getClubId())
                        && clubPositionPermissionEvaluator.hasPermission(access, ClubPositionPermissionEvaluator.PERMISSION_SCHEDULE_UPDATE_SELF),
                clubPositionPermissionEvaluator.isRoleManagementEnabled(access.club().getClubId())
                        && clubPositionPermissionEvaluator.hasPermission(access, ClubPositionPermissionEvaluator.PERMISSION_SCHEDULE_DELETE_SELF)
        );
    }

    public boolean canManageSchedule(ClubAccessResolver.ClubAccess access, Long authorClubProfileId) {
        return getActionPermission(access, authorClubProfileId).canManage();
    }

    public record ScheduleEventActionPermission(
            boolean canEdit,
            boolean canDelete
    ) {
        public boolean canManage() {
            return canEdit || canDelete;
        }
    }
}
