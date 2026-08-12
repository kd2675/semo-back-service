package semo.back.service.feature.position.biz;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import semo.back.service.database.pub.repository.ClubPositionPermissionRepository;
import semo.back.service.feature.club.biz.policy.ClubAccessResolver;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubPositionPermissionEvaluator {
    public static final String FEATURE_ROLE_MANAGEMENT = "ROLE_MANAGEMENT";
    public static final ClubCapability PERMISSION_NOTICE_CREATE = ClubCapability.NOTICE_CREATE;
    public static final ClubCapability PERMISSION_NOTICE_UPDATE_SELF = ClubCapability.NOTICE_UPDATE_SELF;
    public static final ClubCapability PERMISSION_NOTICE_DELETE_SELF = ClubCapability.NOTICE_DELETE_SELF;
    public static final ClubCapability PERMISSION_SCHEDULE_CREATE = ClubCapability.SCHEDULE_CREATE;
    public static final ClubCapability PERMISSION_SCHEDULE_UPDATE_SELF = ClubCapability.SCHEDULE_UPDATE_SELF;
    public static final ClubCapability PERMISSION_SCHEDULE_DELETE_SELF = ClubCapability.SCHEDULE_DELETE_SELF;
    public static final ClubCapability PERMISSION_ATTENDANCE_MANAGE = ClubCapability.ATTENDANCE_MANAGE;
    public static final ClubCapability PERMISSION_POLL_CREATE = ClubCapability.POLL_CREATE;
    public static final ClubCapability PERMISSION_POLL_UPDATE_SELF = ClubCapability.POLL_UPDATE_SELF;
    public static final ClubCapability PERMISSION_POLL_DELETE_SELF = ClubCapability.POLL_DELETE_SELF;
    public static final ClubCapability PERMISSION_TOURNAMENT_CREATE = ClubCapability.TOURNAMENT_RECORD_CREATE;
    public static final ClubCapability PERMISSION_TOURNAMENT_UPDATE_SELF = ClubCapability.TOURNAMENT_RECORD_UPDATE_SELF;
    public static final ClubCapability PERMISSION_TOURNAMENT_PIN = ClubCapability.TOURNAMENT_RECORD_PIN;
    public static final ClubCapability PERMISSION_TOURNAMENT_REVIEW = ClubCapability.TOURNAMENT_RECORD_REVIEW;
    public static final ClubCapability PERMISSION_TOURNAMENT_DELETE_ANY = ClubCapability.TOURNAMENT_RECORD_DELETE_ANY;
    public static final ClubCapability PERMISSION_BRACKET_CREATE = ClubCapability.BRACKET_CREATE;
    public static final ClubCapability PERMISSION_BRACKET_UPDATE_SELF = ClubCapability.BRACKET_UPDATE_SELF;
    public static final ClubCapability PERMISSION_BRACKET_REVIEW = ClubCapability.BRACKET_REVIEW;
    public static final ClubCapability PERMISSION_BRACKET_DELETE_ANY = ClubCapability.BRACKET_DELETE_ANY;
    public static final ClubCapability PERMISSION_FINANCE_VIEW = ClubCapability.FINANCE_VIEW;
    public static final ClubCapability PERMISSION_FINANCE_BILLING_ISSUE = ClubCapability.FINANCE_BILLING_ISSUE;
    public static final ClubCapability PERMISSION_FINANCE_REQUEST_REVIEW = ClubCapability.FINANCE_REQUEST_REVIEW;
    public static final ClubCapability PERMISSION_FINANCE_EXPENSE_CREATE = ClubCapability.FINANCE_EXPENSE_CREATE;
    public static final ClubCapability PERMISSION_FINANCE_PAYMENT_UPDATE = ClubCapability.FINANCE_PAYMENT_UPDATE;
    public static final ClubCapability PERMISSION_FINANCE_EXPORT = ClubCapability.FINANCE_EXPORT;
    public static final ClubCapability PERMISSION_FINANCE_PERIOD_CLOSE = ClubCapability.FINANCE_PERIOD_CLOSE;
    public static final ClubCapability PERMISSION_TODO_VIEW = ClubCapability.TODO_VIEW;
    public static final ClubCapability PERMISSION_TODO_CREATE = ClubCapability.TODO_CREATE;
    public static final ClubCapability PERMISSION_TODO_ASSIGN = ClubCapability.TODO_ASSIGN;
    public static final ClubCapability PERMISSION_TODO_MANAGE_STATUS = ClubCapability.TODO_MANAGE_STATUS;
    public static final ClubCapability PERMISSION_TODO_DELETE_ANY = ClubCapability.TODO_DELETE_ANY;
    public static final ClubCapability PERMISSION_HANDOVER_VIEW = ClubCapability.HANDOVER_VIEW;
    public static final ClubCapability PERMISSION_HANDOVER_MANAGE = ClubCapability.HANDOVER_MANAGE;
    public static final ClubCapability PERMISSION_DECISION_VIEW = ClubCapability.DECISION_VIEW;
    public static final ClubCapability PERMISSION_DECISION_MANAGE = ClubCapability.DECISION_MANAGE;

    private final ClubPositionPermissionRepository clubPositionPermissionRepository;

    public boolean isRoleManagementEnabled(Long clubId) {
        return clubId != null && clubId > 0;
    }

    public boolean hasPermission(
            ClubAccessResolver.ClubAccess access,
            ClubCapability capability
    ) {
        if (access.isAdmin()) {
            return true;
        }
        return getCapabilitiesForMember(
                access.club().getClubId(),
                access.membership().getClubMemberId()
        ).contains(capability);
    }

    public boolean hasAnyPermission(
            ClubAccessResolver.ClubAccess access,
            ClubCapability... capabilities
    ) {
        if (access.isAdmin()) {
            return true;
        }
        Set<ClubCapability> granted = getCapabilitiesForMember(
                access.club().getClubId(),
                access.membership().getClubMemberId()
        );
        return Arrays.stream(capabilities).anyMatch(granted::contains);
    }

    public Set<ClubCapability> getCapabilitiesForMember(Long clubId, Long clubMemberId) {
        return clubPositionPermissionRepository.findEffectivePermissionKeys(clubId, clubMemberId).stream()
                .map(ClubCapability::fromPermissionKey)
                .flatMap(java.util.Optional::stream)
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Read-only compatibility view for response composition and migration tests.
     * Authorization decisions should use the typed capability methods above.
     */
    public Set<String> getPermissionKeysForMember(Long clubId, Long clubMemberId) {
        return getCapabilitiesForMember(clubId, clubMemberId).stream()
                .map(ClubCapability::permissionKey)
                .collect(Collectors.toUnmodifiableSet());
    }
}
