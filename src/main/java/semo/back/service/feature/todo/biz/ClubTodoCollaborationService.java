package semo.back.service.feature.todo.biz;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import semo.back.service.common.exception.SemoException;
import semo.back.service.database.pub.entity.ClubProfile;
import semo.back.service.database.pub.entity.TodoChecklistItem;
import semo.back.service.database.pub.entity.TodoComment;
import semo.back.service.database.pub.entity.TodoItem;
import semo.back.service.database.pub.entity.TodoItemAssignee;
import semo.back.service.database.pub.repository.ClubProfileRepository;
import semo.back.service.database.pub.repository.TodoChecklistItemRepository;
import semo.back.service.database.pub.repository.TodoCommentRepository;
import semo.back.service.database.pub.repository.TodoItemAssigneeRepository;
import semo.back.service.database.pub.repository.TodoItemRepository;
import semo.back.service.feature.activity.biz.ClubActivityContextHolder;
import semo.back.service.feature.activity.biz.RecordClubActivity;
import semo.back.service.feature.club.biz.policy.ClubAccessResolver;
import semo.back.service.feature.notification.biz.ClubNotificationPublisher;
import semo.back.service.feature.notification.biz.ClubNotificationPublisher.NotificationCommand;
import semo.back.service.feature.todo.biz.policy.ClubTodoPermissionService;
import semo.back.service.feature.todo.vo.CreateTodoChecklistItemRequest;
import semo.back.service.feature.todo.vo.CreateTodoCommentRequest;
import semo.back.service.feature.todo.vo.TodoChecklistItemResponse;
import semo.back.service.feature.todo.vo.TodoCollaborationResponse;
import semo.back.service.feature.todo.vo.TodoCommentResponse;
import semo.back.service.feature.todo.vo.UpdateTodoChecklistItemRequest;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubTodoCollaborationService {
    private static final Set<String> TERMINAL_STATUSES = Set.of("COMPLETED", "CANCELED");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final ClubAccessResolver clubAccessResolver;
    private final ClubTodoPermissionService clubTodoPermissionService;
    private final TodoItemRepository todoItemRepository;
    private final TodoItemAssigneeRepository todoItemAssigneeRepository;
    private final TodoChecklistItemRepository todoChecklistItemRepository;
    private final TodoCommentRepository todoCommentRepository;
    private final ClubProfileRepository clubProfileRepository;
    private final ClubNotificationPublisher clubNotificationPublisher;

    public TodoCollaborationResponse getCollaboration(Long clubId, Long todoItemId, String userKey) {
        ClubAccessResolver.ClubAccess access = requireAccess(clubId, userKey);
        TodoItem todoItem = requireTodo(clubId, todoItemId);
        requireCanView(access, todoItem);
        return buildResponse(access, todoItem);
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "할 일")
    public TodoChecklistItemResponse addChecklistItem(
            Long clubId,
            Long todoItemId,
            String userKey,
            CreateTodoChecklistItemRequest request
    ) {
        ClubAccessResolver.ClubAccess access = requireAccess(clubId, userKey);
        TodoItem todoItem = requireTodoForUpdate(clubId, todoItemId);
        requireCanManageChecklist(access, todoItem);
        requireActiveTodo(todoItem);
        String content = normalizeContent(request.content(), 300, "체크리스트 내용은 필수입니다.");
        int sortOrder = Math.toIntExact(todoChecklistItemRepository.countByTodoItemId(todoItemId) * 10 + 10);
        TodoChecklistItem saved = todoChecklistItemRepository.save(TodoChecklistItem.builder()
                .todoItemId(todoItemId)
                .content(content)
                .sortOrder(sortOrder)
                .completed(false)
                .build());
        ClubActivityContextHolder.setDetails(
                "'" + todoItem.getTitle() + "' 업무에 체크리스트를 추가했습니다.",
                "'" + todoItem.getTitle() + "' 업무 체크리스트 추가에 실패했습니다."
        );
        return toChecklistResponse(saved, Map.of());
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "할 일")
    public TodoChecklistItemResponse updateChecklistItem(
            Long clubId,
            Long todoItemId,
            Long todoChecklistItemId,
            String userKey,
            UpdateTodoChecklistItemRequest request
    ) {
        ClubAccessResolver.ClubAccess access = requireAccess(clubId, userKey);
        TodoItem todoItem = requireTodoForUpdate(clubId, todoItemId);
        requireCanManageChecklist(access, todoItem);
        requireActiveTodo(todoItem);
        TodoChecklistItem checklistItem = todoChecklistItemRepository
                .findForUpdate(todoItemId, todoChecklistItemId)
                .orElseThrow(() -> new SemoException.ResourceNotFoundException(
                        "TodoChecklistItem",
                        "todoChecklistItemId",
                        todoChecklistItemId
                ));
        checklistItem.update(
                normalizeContent(request.content(), 300, "체크리스트 내용은 필수입니다."),
                request.completed(),
                access.clubProfile().getClubProfileId(),
                LocalDateTime.now()
        );
        TodoChecklistItem saved = todoChecklistItemRepository.save(checklistItem);
        ClubActivityContextHolder.setDetails(
                "'" + todoItem.getTitle() + "' 업무 체크리스트를 수정했습니다.",
                "'" + todoItem.getTitle() + "' 업무 체크리스트 수정에 실패했습니다."
        );
        return toChecklistResponse(
                saved,
                resolveProfiles(Set.of(access.clubProfile().getClubProfileId()))
        );
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "할 일")
    public void deleteChecklistItem(
            Long clubId,
            Long todoItemId,
            Long todoChecklistItemId,
            String userKey
    ) {
        ClubAccessResolver.ClubAccess access = requireAccess(clubId, userKey);
        TodoItem todoItem = requireTodoForUpdate(clubId, todoItemId);
        requireCanManageChecklist(access, todoItem);
        requireActiveTodo(todoItem);
        TodoChecklistItem checklistItem = todoChecklistItemRepository
                .findForUpdate(todoItemId, todoChecklistItemId)
                .orElseThrow(() -> new SemoException.ResourceNotFoundException(
                        "TodoChecklistItem",
                        "todoChecklistItemId",
                        todoChecklistItemId
                ));
        todoChecklistItemRepository.delete(checklistItem);
        ClubActivityContextHolder.setDetails(
                "'" + todoItem.getTitle() + "' 업무 체크리스트를 삭제했습니다.",
                "'" + todoItem.getTitle() + "' 업무 체크리스트 삭제에 실패했습니다."
        );
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "할 일")
    public TodoCommentResponse addComment(
            Long clubId,
            Long todoItemId,
            String userKey,
            CreateTodoCommentRequest request
    ) {
        ClubAccessResolver.ClubAccess access = requireAccess(clubId, userKey);
        TodoItem todoItem = requireTodoForUpdate(clubId, todoItemId);
        requireCanView(access, todoItem);
        TodoComment saved = todoCommentRepository.save(TodoComment.builder()
                .todoItemId(todoItemId)
                .authorClubProfileId(access.clubProfile().getClubProfileId())
                .content(normalizeContent(request.content(), 2000, "댓글 내용은 필수입니다."))
                .deleted(false)
                .build());
        notifyCollaborators(access, todoItem, saved);
        ClubActivityContextHolder.setDetails(
                "'" + todoItem.getTitle() + "' 업무에 댓글을 남겼습니다.",
                "'" + todoItem.getTitle() + "' 업무 댓글 등록에 실패했습니다."
        );
        return toCommentResponse(
                saved,
                access,
                resolveProfiles(Set.of(access.clubProfile().getClubProfileId()))
        );
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "할 일")
    public void deleteComment(
            Long clubId,
            Long todoItemId,
            Long todoCommentId,
            String userKey
    ) {
        ClubAccessResolver.ClubAccess access = requireAccess(clubId, userKey);
        TodoItem todoItem = requireTodoForUpdate(clubId, todoItemId);
        requireCanView(access, todoItem);
        TodoComment comment = todoCommentRepository.findForUpdate(todoItemId, todoCommentId)
                .orElseThrow(() -> new SemoException.ResourceNotFoundException(
                        "TodoComment",
                        "todoCommentId",
                        todoCommentId
                ));
        boolean mine = Objects.equals(
                comment.getAuthorClubProfileId(),
                access.clubProfile().getClubProfileId()
        );
        if (!mine && !canManageTodo(access)) {
            throw new SemoException.ForbiddenException("본인 댓글 또는 운영 권한이 있는 댓글만 삭제할 수 있습니다.");
        }
        comment.softDelete(access.clubProfile().getClubProfileId(), LocalDateTime.now());
        todoCommentRepository.save(comment);
        ClubActivityContextHolder.setDetails(
                "'" + todoItem.getTitle() + "' 업무 댓글을 삭제했습니다.",
                "'" + todoItem.getTitle() + "' 업무 댓글 삭제에 실패했습니다."
        );
    }

    private TodoCollaborationResponse buildResponse(
            ClubAccessResolver.ClubAccess access,
            TodoItem todoItem
    ) {
        List<TodoChecklistItem> checklistItems = todoChecklistItemRepository
                .findByTodoItemIdOrderBySortOrderAscTodoChecklistItemIdAsc(todoItem.getTodoItemId());
        List<TodoComment> comments = todoCommentRepository
                .findByTodoItemIdAndDeletedFalseOrderByCreateDateAscTodoCommentIdAsc(todoItem.getTodoItemId());
        Set<Long> profileIds = new LinkedHashSet<>();
        checklistItems.stream()
                .map(TodoChecklistItem::getCompletedByClubProfileId)
                .filter(Objects::nonNull)
                .forEach(profileIds::add);
        comments.stream().map(TodoComment::getAuthorClubProfileId).forEach(profileIds::add);
        Map<Long, ClubProfile> profileById = resolveProfiles(profileIds);
        return new TodoCollaborationResponse(
                todoItem.getTodoItemId(),
                todoItem.getTitle(),
                canManageChecklist(access, todoItem),
                true,
                (int) checklistItems.stream().filter(TodoChecklistItem::isCompleted).count(),
                checklistItems.size(),
                checklistItems.stream()
                        .map(item -> toChecklistResponse(item, profileById))
                        .toList(),
                comments.stream()
                        .map(comment -> toCommentResponse(comment, access, profileById))
                        .toList()
        );
    }

    private TodoChecklistItemResponse toChecklistResponse(
            TodoChecklistItem item,
            Map<Long, ClubProfile> profileById
    ) {
        return new TodoChecklistItemResponse(
                item.getTodoChecklistItemId(),
                item.getContent(),
                item.getSortOrder(),
                item.isCompleted(),
                item.getCompletedByClubProfileId(),
                displayName(profileById, item.getCompletedByClubProfileId()),
                formatDateTime(item.getCompletedAt())
        );
    }

    private TodoCommentResponse toCommentResponse(
            TodoComment comment,
            ClubAccessResolver.ClubAccess access,
            Map<Long, ClubProfile> profileById
    ) {
        boolean mine = Objects.equals(
                comment.getAuthorClubProfileId(),
                access.clubProfile().getClubProfileId()
        );
        return new TodoCommentResponse(
                comment.getTodoCommentId(),
                comment.getAuthorClubProfileId(),
                displayName(profileById, comment.getAuthorClubProfileId()),
                comment.getContent(),
                formatDateTime(comment.getCreateDate()),
                mine,
                mine || canManageTodo(access)
        );
    }

    private ClubAccessResolver.ClubAccess requireAccess(Long clubId, String userKey) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        if (!clubTodoPermissionService.isTodoEnabled(clubId)) {
            throw new SemoException.ForbiddenException("할 일 기능이 활성화되지 않았습니다.");
        }
        return access;
    }

    private TodoItem requireTodo(Long clubId, Long todoItemId) {
        return todoItemRepository.findByTodoItemIdAndClubId(todoItemId, clubId)
                .orElseThrow(() -> new SemoException.ResourceNotFoundException("TodoItem", "todoItemId", todoItemId));
    }

    private TodoItem requireTodoForUpdate(Long clubId, Long todoItemId) {
        return todoItemRepository.findForUpdate(todoItemId, clubId)
                .orElseThrow(() -> new SemoException.ResourceNotFoundException("TodoItem", "todoItemId", todoItemId));
    }

    private void requireCanView(ClubAccessResolver.ClubAccess access, TodoItem todoItem) {
        boolean visible = canManageTodo(access)
                || Objects.equals(todoItem.getCreatedByClubProfileId(), access.clubProfile().getClubProfileId())
                || isAssignee(todoItem, access.clubProfile().getClubProfileId())
                || "OPEN_SUPPORT".equals(todoItem.getAssignmentMode());
        if (!visible) {
            throw new SemoException.ForbiddenException("이 업무의 협업 내용을 조회할 권한이 없습니다.");
        }
    }

    private void requireCanManageChecklist(ClubAccessResolver.ClubAccess access, TodoItem todoItem) {
        if (!canManageChecklist(access, todoItem)) {
            throw new SemoException.ForbiddenException("이 업무의 체크리스트를 관리할 권한이 없습니다.");
        }
    }

    private boolean canManageChecklist(ClubAccessResolver.ClubAccess access, TodoItem todoItem) {
        return canManageTodo(access)
                || Objects.equals(todoItem.getCreatedByClubProfileId(), access.clubProfile().getClubProfileId())
                || isAssignee(todoItem, access.clubProfile().getClubProfileId());
    }

    private boolean canManageTodo(ClubAccessResolver.ClubAccess access) {
        return clubTodoPermissionService.canCreateTodo(access)
                || clubTodoPermissionService.canAssignTodo(access)
                || clubTodoPermissionService.canManageStatus(access);
    }

    private boolean isAssignee(TodoItem todoItem, Long clubProfileId) {
        return Objects.equals(todoItem.getAssignedClubProfileId(), clubProfileId)
                || todoItemAssigneeRepository.existsByTodoItemIdAndClubProfileId(
                        todoItem.getTodoItemId(),
                        clubProfileId
                );
    }

    private void requireActiveTodo(TodoItem todoItem) {
        if (TERMINAL_STATUSES.contains(todoItem.getStatusCode())) {
            throw new SemoException.ValidationException("완료되거나 취소된 업무의 체크리스트는 변경할 수 없습니다.");
        }
    }

    private void notifyCollaborators(
            ClubAccessResolver.ClubAccess access,
            TodoItem todoItem,
            TodoComment comment
    ) {
        Set<Long> recipientIds = todoItemAssigneeRepository
                .findByTodoItemIdOrderByTodoItemAssigneeIdAsc(todoItem.getTodoItemId()).stream()
                .map(TodoItemAssignee::getClubProfileId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (todoItem.getAssignedClubProfileId() != null) {
            recipientIds.add(todoItem.getAssignedClubProfileId());
        }
        recipientIds.add(todoItem.getCreatedByClubProfileId());
        recipientIds.remove(access.clubProfile().getClubProfileId());
        recipientIds.forEach(recipientId -> clubNotificationPublisher.notifyClubProfile(
                recipientId,
                new NotificationCommand(
                        access.club().getClubId(),
                        "TODO_COMMENT",
                        "업무에 새 댓글이 등록됐습니다",
                        "'" + todoItem.getTitle() + "' · " + comment.getContent(),
                        "TODO_ITEM",
                        todoItem.getTodoItemId(),
                        "/clubs/" + access.club().getClubId() + "/more/todos",
                        "todo-comment:" + comment.getTodoCommentId()
                )
        ));
    }

    private Map<Long, ClubProfile> resolveProfiles(Set<Long> profileIds) {
        if (profileIds.isEmpty()) {
            return Map.of();
        }
        return clubProfileRepository.findAllById(profileIds).stream()
                .collect(Collectors.toMap(ClubProfile::getClubProfileId, Function.identity()));
    }

    private String displayName(Map<Long, ClubProfile> profileById, Long profileId) {
        if (profileId == null) {
            return null;
        }
        ClubProfile profile = profileById.get(profileId);
        return profile == null ? null : profile.getDisplayName();
    }

    private String normalizeContent(String content, int maxLength, String requiredMessage) {
        if (content == null || content.isBlank()) {
            throw new SemoException.ValidationException(requiredMessage);
        }
        String normalized = content.trim();
        if (normalized.length() > maxLength) {
            throw new SemoException.ValidationException("내용이 허용 길이를 초과했습니다.");
        }
        return normalized;
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? null : value.format(DATE_TIME_FORMATTER);
    }
}
