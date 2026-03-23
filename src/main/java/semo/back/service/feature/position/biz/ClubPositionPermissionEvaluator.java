package semo.back.service.feature.position.biz;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import semo.back.service.database.pub.entity.ClubPosition;
import semo.back.service.database.pub.entity.ClubPositionPermission;
import semo.back.service.database.pub.repository.ClubMemberPositionRepository;
import semo.back.service.database.pub.repository.ClubPositionPermissionRepository;
import semo.back.service.database.pub.repository.ClubPositionRepository;
import semo.back.service.feature.club.biz.ClubAccessResolver;
import semo.back.service.feature.clubfeature.biz.ClubFeatureService;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubPositionPermissionEvaluator {
    public static final String FEATURE_ROLE_MANAGEMENT = "ROLE_MANAGEMENT";
    public static final String PERMISSION_NOTICE_CREATE = "NOTICE_CREATE";
    public static final String PERMISSION_NOTICE_UPDATE_SELF = "NOTICE_UPDATE_SELF";
    public static final String PERMISSION_NOTICE_DELETE_SELF = "NOTICE_DELETE_SELF";
    public static final String PERMISSION_SCHEDULE_CREATE = "SCHEDULE_CREATE";
    public static final String PERMISSION_SCHEDULE_UPDATE_SELF = "SCHEDULE_UPDATE_SELF";
    public static final String PERMISSION_SCHEDULE_DELETE_SELF = "SCHEDULE_DELETE_SELF";
    public static final String PERMISSION_POLL_CREATE = "POLL_CREATE";
    public static final String PERMISSION_POLL_UPDATE_SELF = "POLL_UPDATE_SELF";
    public static final String PERMISSION_POLL_DELETE_SELF = "POLL_DELETE_SELF";
    public static final String PERMISSION_ATTENDANCE_SESSION_CREATE = "ATTENDANCE_SESSION_CREATE";
    public static final String PERMISSION_ATTENDANCE_SESSION_CLOSE = "ATTENDANCE_SESSION_CLOSE";
    public static final String PERMISSION_ROLE_MANAGEMENT_VIEW = "ROLE_MANAGEMENT_VIEW";
    public static final String PERMISSION_ROLE_MANAGEMENT_CREATE = "ROLE_MANAGEMENT_CREATE";
    public static final String PERMISSION_ROLE_MANAGEMENT_UPDATE = "ROLE_MANAGEMENT_UPDATE";
    public static final String PERMISSION_ROLE_MANAGEMENT_DELETE = "ROLE_MANAGEMENT_DELETE";
    public static final String PERMISSION_ROLE_MANAGEMENT_ASSIGN = "ROLE_MANAGEMENT_ASSIGN";

    private final ClubFeatureService clubFeatureService;
    private final ClubPositionRepository clubPositionRepository;
    private final ClubPositionPermissionRepository clubPositionPermissionRepository;
    private final ClubMemberPositionRepository clubMemberPositionRepository;

    public boolean isRoleManagementEnabled(Long clubId) {
        return clubFeatureService.isFeatureEnabled(clubId, FEATURE_ROLE_MANAGEMENT);
    }

    public boolean hasPermission(ClubAccessResolver.ClubAccess access, String permissionKey) {
        if (access.isAdmin()) {
            return true;
        }
        if (!isRoleManagementEnabled(access.club().getClubId())) {
            return false;
        }
        Set<String> permissionKeys = getPermissionKeysForMember(
                access.club().getClubId(),
                access.membership().getClubMemberId()
        );
        return permissionKeys.contains(permissionKey);
    }

    public Set<String> getPermissionKeysForMember(Long clubId, Long clubMemberId) {
        if (!isRoleManagementEnabled(clubId)) {
            return Set.of();
        }

        List<Long> positionIds = clubMemberPositionRepository.findByClubMemberId(clubMemberId).stream()
                .map(item -> item.getClubPositionId())
                .distinct()
                .toList();
        if (positionIds.isEmpty()) {
            return Set.of();
        }

        Set<Long> activePositionIds = clubPositionRepository.findAllById(positionIds).stream()
                .filter(position -> position.getClubId().equals(clubId))
                .filter(ClubPosition::isActive)
                .map(ClubPosition::getClubPositionId)
                .collect(Collectors.toSet());
        if (activePositionIds.isEmpty()) {
            return Set.of();
        }

        return clubPositionPermissionRepository.findByClubPositionIdIn(activePositionIds.stream().toList()).stream()
                .map(ClubPositionPermission::getPermissionKey)
                .collect(Collectors.toSet());
    }
}
