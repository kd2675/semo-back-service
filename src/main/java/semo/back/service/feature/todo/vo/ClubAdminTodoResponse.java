package semo.back.service.feature.todo.vo;

import java.util.List;

public record ClubAdminTodoResponse(
        Long clubId,
        String clubName,
        boolean admin,
        boolean canCreate,
        boolean canAssign,
        boolean canManageStatus,
        boolean canDelete,
        int activeMemberCount,
        int openCount,
        int inProgressCount,
        int completedCount,
        int pendingApplicationCount,
        int overdueCount,
        List<TodoMemberOptionResponse> availableMembers,
        List<TodoScheduleOptionResponse> scheduleOptions,
        List<TodoSummaryResponse> items,
        Long nextCursorTodoItemId,
        boolean hasNext
) {
}
