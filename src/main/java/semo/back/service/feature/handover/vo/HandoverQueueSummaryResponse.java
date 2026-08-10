package semo.back.service.feature.handover.vo;

public record HandoverQueueSummaryResponse(
        int openTodoCount,
        int overdueTodoCount,
        int unpaidPaymentCount,
        int pendingFinanceRequestCount,
        int upcomingScheduleCount,
        int openFeedbackCount,
        int pendingJoinRequestCount,
        int openHandoverNoteCount,
        int openCarryoverCount
) {
}
