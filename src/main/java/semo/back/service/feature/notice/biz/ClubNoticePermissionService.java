package semo.back.service.feature.notice.biz;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import semo.back.service.common.exception.SemoException;
import semo.back.service.feature.club.biz.ClubAccessResolver;
import semo.back.service.feature.notice.vo.ClubAdminNoticeSettingsResponse;
import semo.back.service.feature.notice.vo.UpdateClubAdminNoticeSettingsRequest;
import semo.back.service.feature.position.biz.ClubPositionPermissionEvaluator;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubNoticePermissionService {
    private final ClubPositionPermissionEvaluator clubPositionPermissionEvaluator;

    public ClubAdminNoticeSettingsResponse getAdminSettings(Long clubId, String userKey) {
        throw new SemoException.ValidationException("공지 권한 설정 페이지는 제거되었습니다. 직책관리에서 권한을 관리해주세요.");
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    public ClubAdminNoticeSettingsResponse updateAdminSettings(
            Long clubId,
            String userKey,
            UpdateClubAdminNoticeSettingsRequest request
    ) {
        throw new SemoException.ValidationException("공지 권한 설정 페이지는 제거되었습니다. 직책관리에서 권한을 관리해주세요.");
    }

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
