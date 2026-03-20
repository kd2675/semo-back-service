package semo.back.service.feature.poll.biz;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import semo.back.service.common.exception.SemoException;
import semo.back.service.database.pub.entity.PollPermissionPolicy;
import semo.back.service.database.pub.repository.PollPermissionPolicyRepository;
import semo.back.service.feature.club.biz.ClubAccessResolver;
import semo.back.service.feature.clubfeature.biz.ClubFeatureService;
import semo.back.service.feature.poll.vo.ClubAdminPollSettingsResponse;
import semo.back.service.feature.poll.vo.UpdateClubAdminPollSettingsRequest;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubPollPermissionService {
    private static final String FEATURE_POLL = "POLL";
    private static final PollPolicy DEFAULT_POLICY = new PollPolicy(false, true, true);

    private final ClubAccessResolver clubAccessResolver;
    private final ClubFeatureService clubFeatureService;
    private final PollPermissionPolicyRepository pollPermissionPolicyRepository;

    public ClubAdminPollSettingsResponse getAdminSettings(Long clubId, String userKey) {
        requirePollFeature(clubId);
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireAdmin(clubId, userKey);
        return toAdminSettingsResponse(access, getPolicy(clubId));
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    public ClubAdminPollSettingsResponse updateAdminSettings(
            Long clubId,
            String userKey,
            UpdateClubAdminPollSettingsRequest request
    ) {
        requirePollFeature(clubId);
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireAdmin(clubId, userKey);
        PollPermissionPolicy current = pollPermissionPolicyRepository.findByClubId(clubId).orElse(null);
        pollPermissionPolicyRepository.save(PollPermissionPolicy.builder()
                .pollPermissionPolicyId(current == null ? null : current.getPollPermissionPolicyId())
                .clubId(clubId)
                .allowMemberCreate(Boolean.TRUE.equals(request.allowMemberCreate()))
                .allowMemberUpdate(Boolean.TRUE.equals(request.allowMemberUpdate()))
                .allowMemberDelete(Boolean.TRUE.equals(request.allowMemberDelete()))
                .build());
        return toAdminSettingsResponse(access, getPolicy(clubId));
    }

    public boolean canCreatePoll(ClubAccessResolver.ClubAccess access) {
        if (access.isAdmin()) {
            return true;
        }
        return getPolicy(access.club().getClubId()).allowMemberCreate();
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
        PollPolicy policy = getPolicy(access.club().getClubId());
        return new PollActionPermission(
                policy.allowMemberUpdate(),
                policy.allowMemberDelete()
        );
    }

    public PollPolicy getPolicy(Long clubId) {
        return pollPermissionPolicyRepository.findByClubId(clubId)
                .map(policy -> new PollPolicy(
                        policy.isAllowMemberCreate(),
                        policy.isAllowMemberUpdate(),
                        policy.isAllowMemberDelete()
                ))
                .orElse(DEFAULT_POLICY);
    }

    private ClubAdminPollSettingsResponse toAdminSettingsResponse(
            ClubAccessResolver.ClubAccess access,
            PollPolicy policy
    ) {
        return new ClubAdminPollSettingsResponse(
                access.club().getClubId(),
                access.club().getName(),
                true,
                policy.allowMemberCreate(),
                policy.allowMemberUpdate(),
                policy.allowMemberDelete()
        );
    }

    private void requirePollFeature(Long clubId) {
        if (!clubFeatureService.isFeatureEnabled(clubId, FEATURE_POLL)) {
            throw new SemoException.ValidationException("투표 기능이 활성화되지 않았습니다.");
        }
    }

    public record PollPolicy(
            boolean allowMemberCreate,
            boolean allowMemberUpdate,
            boolean allowMemberDelete
    ) {
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
