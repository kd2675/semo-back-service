package semo.back.service.feature.todo.biz.support;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import semo.back.service.common.exception.SemoException;
import semo.back.service.database.pub.entity.TodoChecklistItem;
import semo.back.service.database.pub.entity.TodoItem;
import semo.back.service.database.pub.entity.TodoItemAssignee;
import semo.back.service.database.pub.repository.TodoChecklistItemRepository;
import semo.back.service.database.pub.repository.TodoItemAssigneeRepository;
import semo.back.service.database.pub.repository.TodoItemRepository;
import semo.back.service.feature.notification.biz.ClubNotificationPublisher;
import semo.back.service.feature.notification.biz.ClubNotificationPublisher.NotificationCommand;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ClubTodoRecurrenceService {
    private static final String ASSIGNMENT_MODE_DIRECT_ASSIGN = "DIRECT_ASSIGN";
    private static final String STATUS_OPEN = "OPEN";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String RECURRENCE_NONE = "NONE";
    private static final String RECURRENCE_WEEKLY = "WEEKLY";
    private static final String RECURRENCE_MONTHLY = "MONTHLY";

    private final TodoItemRepository todoItemRepository;
    private final TodoItemAssigneeRepository todoItemAssigneeRepository;
    private final TodoChecklistItemRepository todoChecklistItemRepository;
    private final ClubNotificationPublisher clubNotificationPublisher;

    public void validateRecurrence(
            String recurrenceFrequency,
            LocalDate recurrenceEndDate,
            LocalDateTime dueAt,
            LocalDateTime workStartAt
    ) {
        if (RECURRENCE_NONE.equals(recurrenceFrequency)) {
            if (recurrenceEndDate != null) {
                throw new SemoException.ValidationException("반복 종료일을 사용하려면 반복 주기를 선택해야 합니다.");
            }
            return;
        }
        LocalDate anchorDate = recurrenceAnchorDate(dueAt, workStartAt);
        if (anchorDate == null) {
            throw new SemoException.ValidationException("반복 업무는 마감일 또는 업무 시작 시간이 필요합니다.");
        }
        if (recurrenceEndDate != null && recurrenceEndDate.isBefore(anchorDate)) {
            throw new SemoException.ValidationException("반복 종료일은 첫 업무 기준일보다 빠를 수 없습니다.");
        }
    }

    public boolean hasConfigurationChanged(
            TodoItem current,
            String recurrenceFrequency,
            int recurrenceInterval,
            LocalDate recurrenceEndDate
    ) {
        return !normalizedRecurrenceFrequency(current).equals(recurrenceFrequency)
                || normalizedRecurrenceInterval(current) != recurrenceInterval
                || !Objects.equals(current.getRecurrenceEndDate(), recurrenceEndDate);
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.MANDATORY)
    public TodoItem createNextTodo(TodoItem completed, Long actorClubProfileId) {
        String recurrenceFrequency = normalizedRecurrenceFrequency(completed);
        if (!STATUS_COMPLETED.equals(completed.getStatusCode()) || RECURRENCE_NONE.equals(recurrenceFrequency)) {
            return null;
        }
        TodoItem existing = todoItemRepository.findByRecurrenceSourceTodoItemId(completed.getTodoItemId())
                .orElse(null);
        if (existing != null) {
            return existing;
        }

        int recurrenceInterval = normalizedRecurrenceInterval(completed);
        LocalDateTime nextDueAt = shiftDateTime(completed.getDueAt(), recurrenceFrequency, recurrenceInterval);
        LocalDateTime nextWorkStartAt = shiftDateTime(
                completed.getWorkStartAt(),
                recurrenceFrequency,
                recurrenceInterval
        );
        LocalDateTime nextWorkEndAt = shiftDateTime(
                completed.getWorkEndAt(),
                recurrenceFrequency,
                recurrenceInterval
        );
        LocalDate nextAnchorDate = recurrenceAnchorDate(nextDueAt, nextWorkStartAt);
        if (nextAnchorDate == null
                || completed.getRecurrenceEndDate() != null
                && nextAnchorDate.isAfter(completed.getRecurrenceEndDate())) {
            return null;
        }

        List<TodoItemAssignee> currentAssignees = todoItemAssigneeRepository
                .findByTodoItemIdOrderByTodoItemAssigneeIdAsc(completed.getTodoItemId());
        List<Long> nextAssigneeIds = ASSIGNMENT_MODE_DIRECT_ASSIGN.equals(completed.getAssignmentMode())
                ? resolveAssigneeIds(completed, currentAssignees)
                : List.of();
        Long primaryAssigneeId = nextAssigneeIds.isEmpty() ? null : nextAssigneeIds.getFirst();
        TodoItem next = todoItemRepository.save(TodoItem.builder()
                .clubId(completed.getClubId())
                .createdByClubProfileId(completed.getCreatedByClubProfileId())
                .assignedClubProfileId(primaryAssigneeId)
                .assignedByClubProfileId(primaryAssigneeId == null ? null : actorClubProfileId)
                .todoType(completed.getTodoType())
                .assignmentMode(completed.getAssignmentMode())
                .statusCode(STATUS_OPEN)
                .priorityCode(completed.getPriorityCode())
                .recruitmentCapacity(completed.getRecruitmentCapacity())
                .title(completed.getTitle())
                .description(completed.getDescription())
                .dueAt(nextDueAt)
                .workStartAt(nextWorkStartAt)
                .workEndAt(nextWorkEndAt)
                .linkedScheduleEventId(null)
                .linkedDecisionRecordId(completed.getLinkedDecisionRecordId())
                .recurrenceFrequency(recurrenceFrequency)
                .recurrenceInterval(recurrenceInterval)
                .recurrenceEndDate(completed.getRecurrenceEndDate())
                .recurrenceSourceTodoItemId(completed.getTodoItemId())
                .completedByClubProfileId(null)
                .completedAt(null)
                .build());
        replaceAssignees(next.getTodoItemId(), nextAssigneeIds, actorClubProfileId);
        cloneChecklist(completed.getTodoItemId(), next.getTodoItemId());
        notifyAssignees(next, nextAssigneeIds);
        return next;
    }

    private List<Long> resolveAssigneeIds(TodoItem todoItem, List<TodoItemAssignee> assignees) {
        List<Long> ids = assignees.stream()
                .map(TodoItemAssignee::getClubProfileId)
                .distinct()
                .toList();
        if (!ids.isEmpty()) {
            return ids;
        }
        return todoItem.getAssignedClubProfileId() == null
                ? List.of()
                : List.of(todoItem.getAssignedClubProfileId());
    }

    private void replaceAssignees(Long todoItemId, List<Long> assigneeIds, Long actorClubProfileId) {
        todoItemAssigneeRepository.deleteByTodoItemId(todoItemId);
        todoItemAssigneeRepository.flush();
        if (assigneeIds.isEmpty()) {
            return;
        }
        todoItemAssigneeRepository.saveAll(assigneeIds.stream()
                .map(profileId -> TodoItemAssignee.builder()
                        .todoItemId(todoItemId)
                        .clubProfileId(profileId)
                        .assignedByClubProfileId(actorClubProfileId)
                        .build())
                .toList());
    }

    private void cloneChecklist(Long sourceTodoItemId, Long nextTodoItemId) {
        List<TodoChecklistItem> sourceItems = todoChecklistItemRepository
                .findByTodoItemIdOrderBySortOrderAscTodoChecklistItemIdAsc(sourceTodoItemId);
        if (sourceItems.isEmpty()) {
            return;
        }
        todoChecklistItemRepository.saveAll(sourceItems.stream()
                .map(item -> TodoChecklistItem.builder()
                        .todoItemId(nextTodoItemId)
                        .content(item.getContent())
                        .sortOrder(item.getSortOrder())
                        .completed(false)
                        .completedByClubProfileId(null)
                        .completedAt(null)
                        .build())
                .toList());
    }

    private void notifyAssignees(TodoItem todoItem, List<Long> assigneeIds) {
        assigneeIds.forEach(assigneeId -> clubNotificationPublisher.notifyClubProfile(
                assigneeId,
                new NotificationCommand(
                        todoItem.getClubId(),
                        "TODO_RECURRING_CREATED",
                        "다음 반복 업무가 열렸습니다",
                        "'" + todoItem.getTitle() + "' 다음 회차 업무를 확인해주세요.",
                        "TODO_ITEM",
                        todoItem.getTodoItemId(),
                        "/clubs/" + todoItem.getClubId() + "/more/todos",
                        "todo-recurring:" + todoItem.getTodoItemId()
                )
        ));
    }

    private LocalDate recurrenceAnchorDate(LocalDateTime dueAt, LocalDateTime workStartAt) {
        if (workStartAt != null) {
            return workStartAt.toLocalDate();
        }
        return dueAt == null ? null : dueAt.toLocalDate();
    }

    private LocalDateTime shiftDateTime(
            LocalDateTime value,
            String recurrenceFrequency,
            int recurrenceInterval
    ) {
        if (value == null) {
            return null;
        }
        return switch (recurrenceFrequency) {
            case RECURRENCE_WEEKLY -> value.plusWeeks(recurrenceInterval);
            case RECURRENCE_MONTHLY -> value.plusMonths(recurrenceInterval);
            default -> value;
        };
    }

    private String normalizedRecurrenceFrequency(TodoItem todoItem) {
        return todoItem.getRecurrenceFrequency() == null || todoItem.getRecurrenceFrequency().isBlank()
                ? RECURRENCE_NONE
                : todoItem.getRecurrenceFrequency();
    }

    private int normalizedRecurrenceInterval(TodoItem todoItem) {
        return todoItem.getRecurrenceInterval() == null
                ? 1
                : Math.max(1, todoItem.getRecurrenceInterval());
    }
}
