package semo.back.service.feature.todo.vo;

import java.util.List;

public record ClubAdminTodoResponse(
        Long clubId,
        String clubName,
        boolean admin,
        boolean canCreate,
        boolean canAssign,
        boolean canManageStatus,
        int activeMemberCount,
        int openCount,
        int inProgressCount,
        int completedCount,
        int overdueCount,
        List<TodoMemberOptionResponse> availableMembers,
        List<TodoSummaryResponse> items,
        Long nextCursorTodoItemId,
        boolean hasNext
) {
}
