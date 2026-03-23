package semo.back.service.feature.poll.biz;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import semo.back.service.common.exception.SemoException;
import semo.back.service.feature.club.biz.ClubAccessResolver;
import semo.back.service.feature.position.biz.ClubPositionPermissionEvaluator;
import semo.back.service.feature.poll.vo.ClubAdminPollSettingsResponse;
import semo.back.service.feature.poll.vo.UpdateClubAdminPollSettingsRequest;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubPollPermissionService {
    private final ClubPositionPermissionEvaluator clubPositionPermissionEvaluator;

    public ClubAdminPollSettingsResponse getAdminSettings(Long clubId, String userKey) {
        throw new SemoException.ValidationException("투표 권한 설정 페이지는 제거되었습니다. 직책관리에서 권한을 관리해주세요.");
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    public ClubAdminPollSettingsResponse updateAdminSettings(
            Long clubId,
            String userKey,
            UpdateClubAdminPollSettingsRequest request
    ) {
        throw new SemoException.ValidationException("투표 권한 설정 페이지는 제거되었습니다. 직책관리에서 권한을 관리해주세요.");
    }

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
