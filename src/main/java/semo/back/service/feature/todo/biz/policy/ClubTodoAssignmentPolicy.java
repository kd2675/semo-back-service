package semo.back.service.feature.todo.biz.policy;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import semo.back.service.database.pub.entity.TodoItem;
import semo.back.service.database.pub.entity.TodoItemAssignee;
import semo.back.service.database.pub.repository.TodoItemAssigneeRepository;

import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class ClubTodoAssignmentPolicy {
    private static final String ASSIGNMENT_MODE_OPEN_SUPPORT = "OPEN_SUPPORT";
    private static final String STATUS_OPEN = "OPEN";
    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";

    private final TodoItemAssigneeRepository todoItemAssigneeRepository;

    public boolean isAssignee(TodoItem todoItem, Long clubProfileId) {
        return Objects.equals(todoItem.getAssignedClubProfileId(), clubProfileId)
                || todoItemAssigneeRepository.existsByTodoItemIdAndClubProfileId(
                        todoItem.getTodoItemId(),
                        clubProfileId
                );
    }

    public boolean isRecruitmentAvailable(TodoItem todoItem) {
        if (!ASSIGNMENT_MODE_OPEN_SUPPORT.equals(todoItem.getAssignmentMode())) {
            return false;
        }
        if (!STATUS_OPEN.equals(todoItem.getStatusCode())
                && !STATUS_IN_PROGRESS.equals(todoItem.getStatusCode())) {
            return false;
        }
        long assigneeCount = todoItemAssigneeRepository.countByTodoItemId(todoItem.getTodoItemId());
        if (assigneeCount == 0 && todoItem.getAssignedClubProfileId() != null) {
            assigneeCount = 1;
        }
        return assigneeCount < normalizedRecruitmentCapacity(todoItem);
    }

    public int normalizedRecruitmentCapacity(TodoItem todoItem) {
        return todoItem.getRecruitmentCapacity() == null
                ? 1
                : Math.max(1, todoItem.getRecruitmentCapacity());
    }

    public List<Long> resolveAssigneeIds(TodoItem todoItem, List<TodoItemAssignee> assignees) {
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
}
