package semo.back.service.database.pub.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import semo.back.service.database.pub.entity.TodoComment;

import java.util.List;
import java.util.Optional;

public interface TodoCommentRepository extends JpaRepository<TodoComment, Long> {
    List<TodoComment> findByTodoItemIdAndDeletedFalseOrderByCreateDateAscTodoCommentIdAsc(Long todoItemId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select comment
            from TodoComment comment
            where comment.todoCommentId = :todoCommentId
              and comment.todoItemId = :todoItemId
              and comment.deleted = false
            """)
    Optional<TodoComment> findForUpdate(Long todoItemId, Long todoCommentId);
}
