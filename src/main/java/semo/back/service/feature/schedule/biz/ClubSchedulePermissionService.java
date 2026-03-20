package semo.back.service.feature.schedule.biz;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import semo.back.service.common.exception.SemoException;
import semo.back.service.database.pub.entity.SchedulePermissionPolicy;
import semo.back.service.database.pub.repository.SchedulePermissionPolicyRepository;
import semo.back.service.feature.club.biz.ClubAccessResolver;
import semo.back.service.feature.clubfeature.biz.ClubFeatureService;
import semo.back.service.feature.schedule.vo.ClubAdminScheduleSettingsResponse;
import semo.back.service.feature.schedule.vo.UpdateClubAdminScheduleSettingsRequest;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubSchedulePermissionService {
    private static final String FEATURE_SCHEDULE = "SCHEDULE_MANAGE";
    private static final SchedulePolicy DEFAULT_POLICY = new SchedulePolicy(false, true, true);

    private final ClubAccessResolver clubAccessResolver;
    private final ClubFeatureService clubFeatureService;
    private final SchedulePermissionPolicyRepository schedulePermissionPolicyRepository;

    public ClubAdminScheduleSettingsResponse getAdminSettings(Long clubId, String userKey) {
        requireScheduleFeature(clubId);
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireAdmin(clubId, userKey);
        return toAdminSettingsResponse(access, getPolicy(clubId));
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    public ClubAdminScheduleSettingsResponse updateAdminSettings(
            Long clubId,
            String userKey,
            UpdateClubAdminScheduleSettingsRequest request
    ) {
        requireScheduleFeature(clubId);
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireAdmin(clubId, userKey);
        SchedulePermissionPolicy current = schedulePermissionPolicyRepository.findByClubId(clubId).orElse(null);
        schedulePermissionPolicyRepository.save(SchedulePermissionPolicy.builder()
                .schedulePermissionPolicyId(current == null ? null : current.getSchedulePermissionPolicyId())
                .clubId(clubId)
                .allowMemberCreate(Boolean.TRUE.equals(request.allowMemberCreate()))
                .allowMemberUpdate(Boolean.TRUE.equals(request.allowMemberUpdate()))
                .allowMemberDelete(Boolean.TRUE.equals(request.allowMemberDelete()))
                .build());
        return toAdminSettingsResponse(access, getPolicy(clubId));
    }

    public boolean canCreateSchedule(ClubAccessResolver.ClubAccess access) {
        if (access.isAdmin()) {
            return true;
        }
        return getPolicy(access.club().getClubId()).allowMemberCreate();
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
        SchedulePolicy policy = getPolicy(access.club().getClubId());
        return new ScheduleEventActionPermission(
                policy.allowMemberUpdate(),
                policy.allowMemberDelete()
        );
    }

    public boolean canManageSchedule(ClubAccessResolver.ClubAccess access, Long authorClubProfileId) {
        return getActionPermission(access, authorClubProfileId).canManage();
    }

    public SchedulePolicy getPolicy(Long clubId) {
        return schedulePermissionPolicyRepository.findByClubId(clubId)
                .map(policy -> new SchedulePolicy(
                        policy.isAllowMemberCreate(),
                        policy.isAllowMemberUpdate(),
                        policy.isAllowMemberDelete()
                ))
                .orElse(DEFAULT_POLICY);
    }

    private ClubAdminScheduleSettingsResponse toAdminSettingsResponse(
            ClubAccessResolver.ClubAccess access,
            SchedulePolicy policy
    ) {
        return new ClubAdminScheduleSettingsResponse(
                access.club().getClubId(),
                access.club().getName(),
                true,
                policy.allowMemberCreate(),
                policy.allowMemberUpdate(),
                policy.allowMemberDelete()
        );
    }

    private void requireScheduleFeature(Long clubId) {
        if (!clubFeatureService.isFeatureEnabled(clubId, FEATURE_SCHEDULE)) {
            throw new SemoException.ValidationException("일정 관리 기능이 활성화되지 않았습니다.");
        }
    }

    public record SchedulePolicy(
            boolean allowMemberCreate,
            boolean allowMemberUpdate,
            boolean allowMemberDelete
    ) {
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
