package semo.back.service.feature.todo.biz.policy;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import semo.back.service.feature.club.biz.policy.ClubAccessResolver;
import semo.back.service.feature.clubfeature.biz.ClubFeatureService;
import semo.back.service.feature.position.biz.ClubCapability;
import semo.back.service.feature.position.biz.ClubPositionPermissionEvaluator;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubTodoPermissionService {
    public static final String FEATURE_TODO = "TODO";

    private final ClubFeatureService clubFeatureService;
    private final ClubPositionPermissionEvaluator clubPositionPermissionEvaluator;

    public boolean isTodoEnabled(Long clubId) {
        return clubFeatureService.isFeatureEnabled(clubId, FEATURE_TODO);
    }

    public boolean canViewAdminTodos(ClubAccessResolver.ClubAccess access) {
        if (access.isAdmin()) {
            return true;
        }
        return hasRolePermission(access, ClubPositionPermissionEvaluator.PERMISSION_TODO_VIEW);
    }

    public boolean canCreateTodo(ClubAccessResolver.ClubAccess access) {
        if (access.isAdmin()) {
            return true;
        }
        return hasRolePermission(access, ClubPositionPermissionEvaluator.PERMISSION_TODO_CREATE);
    }

    public boolean canAssignTodo(ClubAccessResolver.ClubAccess access) {
        if (access.isAdmin()) {
            return true;
        }
        return hasRolePermission(access, ClubPositionPermissionEvaluator.PERMISSION_TODO_ASSIGN);
    }

    public boolean canManageStatus(ClubAccessResolver.ClubAccess access) {
        if (access.isAdmin()) {
            return true;
        }
        return hasRolePermission(access, ClubPositionPermissionEvaluator.PERMISSION_TODO_MANAGE_STATUS);
    }

    public boolean canDeleteTodo(ClubAccessResolver.ClubAccess access) {
        if (access.isAdmin()) {
            return true;
        }
        return hasRolePermission(access, ClubPositionPermissionEvaluator.PERMISSION_TODO_DELETE_ANY);
    }

    private boolean hasRolePermission(ClubAccessResolver.ClubAccess access, ClubCapability capability) {
        return clubPositionPermissionEvaluator.hasPermission(access, capability);
    }
}
