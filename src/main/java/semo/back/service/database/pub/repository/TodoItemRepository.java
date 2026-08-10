package semo.back.service.database.pub.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import semo.back.service.database.pub.entity.TodoItem;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TodoItemRepository extends JpaRepository<TodoItem, Long> {
    List<TodoItem> findByClubIdOrderByTodoItemIdDesc(Long clubId);

    List<TodoItem> findByClubIdAndAssignedClubProfileIdOrderByTodoItemIdDesc(Long clubId, Long assignedClubProfileId);

    @Query("""
            select t
            from TodoItem t
            where t.clubId = :clubId
              and (
                    t.assignedClubProfileId = :clubProfileId
                    or exists (
                        select assignee.todoItemAssigneeId
                        from TodoItemAssignee assignee
                        where assignee.todoItemId = t.todoItemId
                          and assignee.clubProfileId = :clubProfileId
                    )
                  )
            order by t.todoItemId desc
            """)
    List<TodoItem> findAssignedTodos(Long clubId, Long clubProfileId);

    List<TodoItem> findTop5ByClubIdAndCompletedByClubProfileIdOrderByTodoItemIdDesc(Long clubId, Long completedByClubProfileId);

    Optional<TodoItem> findByTodoItemIdAndClubId(Long todoItemId, Long clubId);

    Optional<TodoItem> findByRecurrenceSourceTodoItemId(Long recurrenceSourceTodoItemId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select todo
            from TodoItem todo
            where todo.todoItemId = :todoItemId
              and todo.clubId = :clubId
            """)
    Optional<TodoItem> findForUpdate(Long todoItemId, Long clubId);

    @Query("""
            select t
            from TodoItem t
            where t.clubId = :clubId
              and t.assignmentMode = 'OPEN_SUPPORT'
              and t.statusCode in ('OPEN', 'IN_PROGRESS')
              and (
                    select count(assignee.todoItemAssigneeId)
                    from TodoItemAssignee assignee
                    where assignee.todoItemId = t.todoItemId
                  ) < t.recruitmentCapacity
            order by
              case when t.dueAt is null then 1 else 0 end,
              t.dueAt asc,
              t.todoItemId desc
            """)
    List<TodoItem> findClaimableTodos(Long clubId, Pageable pageable);

    @Query("""
            select count(t)
            from TodoItem t
            where t.clubId = :clubId
              and t.assignmentMode = 'OPEN_SUPPORT'
              and t.statusCode in ('OPEN', 'IN_PROGRESS')
              and (
                    select count(assignee.todoItemAssigneeId)
                    from TodoItemAssignee assignee
                    where assignee.todoItemId = t.todoItemId
                  ) < t.recruitmentCapacity
            """)
    long countClaimableTodos(Long clubId);

    @Query("""
            select count(t)
            from TodoItem t
            where t.clubId = :clubId
              and (
                    t.assignedClubProfileId = :clubProfileId
                    or exists (
                        select assignee.todoItemAssigneeId
                        from TodoItemAssignee assignee
                        where assignee.todoItemId = t.todoItemId
                          and assignee.clubProfileId = :clubProfileId
                    )
                  )
              and t.statusCode not in ('COMPLETED', 'CANCELED')
            """)
    long countActiveAssigned(Long clubId, Long clubProfileId);

    @Query("""
            select count(t)
            from TodoItem t
            where t.clubId = :clubId
              and (
                    t.assignedClubProfileId = :clubProfileId
                    or exists (
                        select assignee.todoItemAssigneeId
                        from TodoItemAssignee assignee
                        where assignee.todoItemId = t.todoItemId
                          and assignee.clubProfileId = :clubProfileId
                    )
                  )
              and t.statusCode not in ('COMPLETED', 'CANCELED')
              and t.dueAt is not null
              and t.dueAt < :now
            """)
    long countOverdueAssigned(Long clubId, Long clubProfileId, LocalDateTime now);

    @Query("""
            select count(t)
            from TodoItem t
            where t.clubId = :clubId
              and t.statusCode not in ('COMPLETED', 'CANCELED')
            """)
    long countActiveForAdmin(Long clubId);

    @Query("""
            select count(t)
            from TodoItem t
            where t.clubId = :clubId
              and t.statusCode not in ('COMPLETED', 'CANCELED')
              and t.dueAt is not null
              and t.dueAt < :now
            """)
    long countOverdueForAdmin(Long clubId, LocalDateTime now);

    @Query("""
            select t
            from TodoItem t
            where t.clubId = :clubId
              and (:cursorTodoItemId is null or t.todoItemId < :cursorTodoItemId)
              and (
                    :statusFilter is null
                    or :statusFilter = 'ALL'
                    or (
                        :statusFilter = 'OVERDUE'
                        and t.dueAt is not null
                        and t.dueAt < :now
                        and t.statusCode not in :terminalStatuses
                    )
                    or t.statusCode = :statusFilter
                  )
              and (
                    :assignmentFilter is null
                    or :assignmentFilter = 'ALL'
                    or (
                        :assignmentFilter = 'ASSIGNED'
                        and (
                            t.assignedClubProfileId is not null
                            or exists (
                                select assignee.todoItemAssigneeId
                                from TodoItemAssignee assignee
                                where assignee.todoItemId = t.todoItemId
                            )
                        )
                    )
                    or (
                        :assignmentFilter = 'UNASSIGNED'
                        and t.assignedClubProfileId is null
                        and not exists (
                            select assignee.todoItemAssigneeId
                            from TodoItemAssignee assignee
                            where assignee.todoItemId = t.todoItemId
                        )
                    )
                    or (:assignmentFilter = 'OPEN_SUPPORT' and t.assignmentMode = 'OPEN_SUPPORT')
                    or (:assignmentFilter = 'DIRECT_ASSIGN' and t.assignmentMode = 'DIRECT_ASSIGN')
                  )
              and (
                    :applicationFilter is null
                    or :applicationFilter = 'ALL'
                    or exists (
                        select 1
                        from TodoItemApplication application
                        where application.todoItemId = t.todoItemId
                          and application.applicationStatus = :applicationFilter
                    )
                  )
            order by t.todoItemId desc
            """)
    List<TodoItem> findAdminFeed(
            Long clubId,
            String statusFilter,
            String assignmentFilter,
            String applicationFilter,
            Long cursorTodoItemId,
            LocalDateTime now,
            Collection<String> terminalStatuses,
            Pageable pageable
    );
}
