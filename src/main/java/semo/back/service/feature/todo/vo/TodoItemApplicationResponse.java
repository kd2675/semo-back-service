package semo.back.service.feature.todo.vo;

public record TodoItemApplicationResponse(
        Long todoItemApplicationId,
        Long todoItemId,
        Long clubProfileId,
        String applicantDisplayName,
        String applicationStatus,
        String applicationStatusLabel,
        String applicationNote,
        String reviewNote,
        String appliedAtLabel,
        String reviewedAtLabel,
        boolean mine,
        boolean canReview
) {
}
