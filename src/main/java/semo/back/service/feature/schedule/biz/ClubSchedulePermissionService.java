package semo.back.service.feature.schedule.biz;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import semo.back.service.common.exception.SemoException;
import semo.back.service.feature.club.biz.ClubAccessResolver;
import semo.back.service.feature.position.biz.ClubPositionPermissionEvaluator;
import semo.back.service.feature.schedule.vo.ClubAdminScheduleSettingsResponse;
import semo.back.service.feature.schedule.vo.UpdateClubAdminScheduleSettingsRequest;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubSchedulePermissionService {
    private final ClubPositionPermissionEvaluator clubPositionPermissionEvaluator;

    public ClubAdminScheduleSettingsResponse getAdminSettings(Long clubId, String userKey) {
        throw new SemoException.ValidationException("일정 권한 설정 페이지는 제거되었습니다. 직책관리에서 권한을 관리해주세요.");
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    public ClubAdminScheduleSettingsResponse updateAdminSettings(
            Long clubId,
            String userKey,
            UpdateClubAdminScheduleSettingsRequest request
    ) {
        throw new SemoException.ValidationException("일정 권한 설정 페이지는 제거되었습니다. 직책관리에서 권한을 관리해주세요.");
    }

    public boolean canCreateSchedule(ClubAccessResolver.ClubAccess access) {
        if (access.isAdmin()) {
            return true;
        }
        return clubPositionPermissionEvaluator.isRoleManagementEnabled(access.club().getClubId())
                && clubPositionPermissionEvaluator.hasPermission(access, ClubPositionPermissionEvaluator.PERMISSION_SCHEDULE_CREATE);
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
