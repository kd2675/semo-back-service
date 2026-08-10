package semo.back.service.database.pub.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import semo.back.service.database.pub.entity.TodoChecklistItem;

import java.util.List;
import java.util.Optional;

public interface TodoChecklistItemRepository extends JpaRepository<TodoChecklistItem, Long> {
    List<TodoChecklistItem> findByTodoItemIdOrderBySortOrderAscTodoChecklistItemIdAsc(Long todoItemId);

    long countByTodoItemId(Long todoItemId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select item
            from TodoChecklistItem item
            where item.todoChecklistItemId = :todoChecklistItemId
              and item.todoItemId = :todoItemId
            """)
    Optional<TodoChecklistItem> findForUpdate(Long todoItemId, Long todoChecklistItemId);

    void deleteByTodoItemId(Long todoItemId);
}
