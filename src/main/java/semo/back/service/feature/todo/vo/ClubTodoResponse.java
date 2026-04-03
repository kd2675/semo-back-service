package semo.back.service.feature.todo.vo;

import java.util.List;

public record ClubTodoResponse(
        Long clubId,
        String clubName,
        boolean admin,
        int myOpenCount,
        int myCompletedCount,
        int myApplyingCount,
        int claimableOpenCount,
        int overdueCount,
        List<TodoSummaryResponse> myTodos,
        List<TodoSummaryResponse> claimableTodos,
        List<TodoSummaryResponse> recentCompletedTodos
) {
}
