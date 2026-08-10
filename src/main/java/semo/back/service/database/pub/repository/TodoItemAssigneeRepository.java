package semo.back.service.database.pub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import semo.back.service.database.pub.entity.TodoItemAssignee;

import java.util.Collection;
import java.util.List;

public interface TodoItemAssigneeRepository extends JpaRepository<TodoItemAssignee, Long> {
    List<TodoItemAssignee> findByTodoItemIdOrderByTodoItemAssigneeIdAsc(Long todoItemId);

    List<TodoItemAssignee> findByTodoItemIdInOrderByTodoItemAssigneeIdAsc(Collection<Long> todoItemIds);

    boolean existsByTodoItemIdAndClubProfileId(Long todoItemId, Long clubProfileId);

    long countByTodoItemId(Long todoItemId);

    void deleteByTodoItemId(Long todoItemId);
}
