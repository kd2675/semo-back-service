package semo.back.service.database.pub.repository;

public interface ClubGrowthEvidenceRow {
    long getAttendanceCount();

    long getVoteSelectionCount();

    long getCompletedTodoCount();

    long getSubmittedFeedbackCount();

    long getBoardReadCount();

    long getPublishedNoticeCount();

    long getAnsweredFeedbackCount();

    long getClosedVoteCount();

    long getAttendanceEventCount();

    long getConfirmedDecisionCount();

    long getDecisionResourceLinkCount();

    long getLinkedTodoCount();

    long getRecurringTodoCount();

    long getAcknowledgedHandoverCount();

    long getResolvedCarryoverCount();

    long getClosedTermCount();

    long getRecentActivityCount();
}
