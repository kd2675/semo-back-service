package semo.back.service.database.pub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import semo.back.service.database.pub.entity.TodoItemApplication;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TodoItemApplicationRepository extends JpaRepository<TodoItemApplication, Long> {
    List<TodoItemApplication> findByTodoItemIdOrderByCreateDateAscTodoItemApplicationIdAsc(Long todoItemId);

    Optional<TodoItemApplication> findByTodoItemIdAndClubProfileId(Long todoItemId, Long clubProfileId);

    List<TodoItemApplication> findByTodoItemIdIn(Collection<Long> todoItemIds);

    List<TodoItemApplication> findByTodoItemIdInAndClubProfileId(Collection<Long> todoItemIds, Long clubProfileId);

    List<TodoItemApplication> findByTodoItemIdAndApplicationStatusOrderByCreateDateAscTodoItemApplicationIdAsc(
            Long todoItemId,
            String applicationStatus
    );

    List<TodoItemApplication> findByClubProfileId(Long clubProfileId);

    void deleteByTodoItemId(Long todoItemId);
}
