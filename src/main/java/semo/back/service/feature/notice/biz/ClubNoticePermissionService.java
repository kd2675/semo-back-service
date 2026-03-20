package semo.back.service.feature.notice.biz;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import semo.back.service.common.exception.SemoException;
import semo.back.service.database.pub.entity.NoticePermissionPolicy;
import semo.back.service.database.pub.repository.NoticePermissionPolicyRepository;
import semo.back.service.feature.club.biz.ClubAccessResolver;
import semo.back.service.feature.clubfeature.biz.ClubFeatureService;
import semo.back.service.feature.notice.vo.ClubAdminNoticeSettingsResponse;
import semo.back.service.feature.notice.vo.UpdateClubAdminNoticeSettingsRequest;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubNoticePermissionService {
    private static final String FEATURE_NOTICE = "NOTICE";
    private static final NoticePolicy DEFAULT_POLICY = new NoticePolicy(false, true, true);

    private final ClubAccessResolver clubAccessResolver;
    private final ClubFeatureService clubFeatureService;
    private final NoticePermissionPolicyRepository noticePermissionPolicyRepository;

    public ClubAdminNoticeSettingsResponse getAdminSettings(Long clubId, String userKey) {
        requireNoticeFeature(clubId);
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireAdmin(clubId, userKey);
        NoticePolicy policy = getPolicy(clubId);
        return toAdminSettingsResponse(access, policy);
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    public ClubAdminNoticeSettingsResponse updateAdminSettings(
            Long clubId,
            String userKey,
            UpdateClubAdminNoticeSettingsRequest request
    ) {
        requireNoticeFeature(clubId);
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireAdmin(clubId, userKey);
        NoticePermissionPolicy current = noticePermissionPolicyRepository.findByClubId(clubId).orElse(null);
        noticePermissionPolicyRepository.save(NoticePermissionPolicy.builder()
                .noticePermissionPolicyId(current == null ? null : current.getNoticePermissionPolicyId())
                .clubId(clubId)
                .allowMemberCreate(Boolean.TRUE.equals(request.allowMemberCreate()))
                .allowMemberUpdate(Boolean.TRUE.equals(request.allowMemberUpdate()))
                .allowMemberDelete(Boolean.TRUE.equals(request.allowMemberDelete()))
                .build());
        return toAdminSettingsResponse(access, getPolicy(clubId));
    }

    public boolean canCreateNotice(ClubAccessResolver.ClubAccess access) {
        if (access.isAdmin()) {
            return true;
        }
        return getPolicy(access.club().getClubId()).allowMemberCreate();
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
        NoticePolicy policy = getPolicy(access.club().getClubId());
        return new NoticeActionPermission(
                policy.allowMemberUpdate(),
                policy.allowMemberDelete()
        );
    }

    public boolean canManageNotice(ClubAccessResolver.ClubAccess access, Long authorClubProfileId) {
        return getActionPermission(access, authorClubProfileId).canManage();
    }

    public NoticePolicy getPolicy(Long clubId) {
        return noticePermissionPolicyRepository.findByClubId(clubId)
                .map(policy -> new NoticePolicy(
                        policy.isAllowMemberCreate(),
                        policy.isAllowMemberUpdate(),
                        policy.isAllowMemberDelete()
                ))
                .orElse(DEFAULT_POLICY);
    }

    private ClubAdminNoticeSettingsResponse toAdminSettingsResponse(
            ClubAccessResolver.ClubAccess access,
            NoticePolicy policy
    ) {
        return new ClubAdminNoticeSettingsResponse(
                access.club().getClubId(),
                access.club().getName(),
                true,
                policy.allowMemberCreate(),
                policy.allowMemberUpdate(),
                policy.allowMemberDelete()
        );
    }

    private void requireNoticeFeature(Long clubId) {
        if (!clubFeatureService.isFeatureEnabled(clubId, FEATURE_NOTICE)) {
            throw new SemoException.ValidationException("공지 기능이 활성화되지 않았습니다.");
        }
    }

    public record NoticePolicy(
            boolean allowMemberCreate,
            boolean allowMemberUpdate,
            boolean allowMemberDelete
    ) {
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
