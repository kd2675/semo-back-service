package semo.back.service.feature.todo.biz;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import semo.back.service.common.exception.SemoException;
import semo.back.service.database.pub.entity.ClubProfile;
import semo.back.service.database.pub.entity.TodoItem;
import semo.back.service.database.pub.repository.ClubProfileRepository;
import semo.back.service.database.pub.repository.TodoItemRepository;
import semo.back.service.feature.activity.biz.ClubActivityContextHolder;
import semo.back.service.feature.activity.biz.RecordClubActivity;
import semo.back.service.feature.club.biz.ClubAccessResolver;
import semo.back.service.feature.todo.vo.ClubAdminTodoResponse;
import semo.back.service.feature.todo.vo.ClubTodoResponse;
import semo.back.service.feature.todo.vo.CreateClubTodoRequest;
import semo.back.service.feature.todo.vo.TodoActionResponse;
import semo.back.service.feature.todo.vo.TodoMemberOptionResponse;
import semo.back.service.feature.todo.vo.TodoSummaryResponse;
import semo.back.service.feature.todo.vo.UpdateClubTodoRequest;
import semo.back.service.feature.todo.vo.UpdateTodoStatusRequest;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubTodoService {
    private static final String TODO_TYPE_VOLUNTEER = "VOLUNTEER";
    private static final String TODO_TYPE_OPERATIONS = "OPERATIONS";
    private static final Set<String> ALLOWED_TODO_TYPES = Set.of(
            TODO_TYPE_VOLUNTEER,
            TODO_TYPE_OPERATIONS
    );

    private static final String ASSIGNMENT_MODE_DIRECT_ASSIGN = "DIRECT_ASSIGN";
    private static final String ASSIGNMENT_MODE_OPEN_SUPPORT = "OPEN_SUPPORT";
    private static final Set<String> ALLOWED_ASSIGNMENT_MODES = Set.of(
            ASSIGNMENT_MODE_DIRECT_ASSIGN,
            ASSIGNMENT_MODE_OPEN_SUPPORT
    );

    private static final String STATUS_OPEN = "OPEN";
    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_CANCELED = "CANCELED";
    private static final Set<String> TERMINAL_STATUSES = Set.of(STATUS_COMPLETED, STATUS_CANCELED);
    private static final Set<String> ADMIN_FILTERABLE_STATUSES = Set.of(
            STATUS_OPEN,
            STATUS_IN_PROGRESS,
            STATUS_COMPLETED,
            "OVERDUE"
    );
    private static final Set<String> DIRECT_FILTER_STATUSES = Set.of(
            STATUS_OPEN,
            STATUS_IN_PROGRESS,
            STATUS_COMPLETED,
            STATUS_CANCELED
    );
    private static final Set<String> ALLOWED_ASSIGNMENT_FILTERS = Set.of(
            "ALL",
            "ASSIGNED",
            "UNASSIGNED",
            "OPEN_SUPPORT"
    );

    private static final int CLAIMABLE_PAGE_SIZE = 8;
    private static final int DEFAULT_ADMIN_PAGE_SIZE = 12;
    private static final int MAX_ADMIN_PAGE_SIZE = 30;
    private static final DateTimeFormatter REQUEST_DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter DATE_TIME_LABEL_FORMATTER = DateTimeFormatter.ofPattern(
            "yyyy.MM.dd HH:mm",
            Locale.KOREAN
    );

    private final ClubAccessResolver clubAccessResolver;
    private final ClubTodoPermissionService clubTodoPermissionService;
    private final TodoItemRepository todoItemRepository;
    private final ClubProfileRepository clubProfileRepository;

    public ClubTodoResponse getTodos(Long clubId, String userKey) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        requireTodoFeature(clubId);

        List<TodoItem> myItems = todoItemRepository.findByClubIdAndAssignedClubProfileIdOrderByTodoItemIdDesc(
                clubId,
                access.clubProfile().getClubProfileId()
        );
        List<TodoItem> claimableItems = todoItemRepository.findClaimableTodos(clubId, PageRequest.of(0, CLAIMABLE_PAGE_SIZE));
        List<TodoItem> recentCompletedItems = todoItemRepository.findTop5ByClubIdAndCompletedByClubProfileIdOrderByTodoItemIdDesc(
                clubId,
                access.clubProfile().getClubProfileId()
        );
        Map<Long, ClubProfile> profileById = resolveClubProfiles(List.of(myItems, claimableItems, recentCompletedItems));

        List<TodoSummaryResponse> myTodos = myItems.stream()
                .filter(item -> !TERMINAL_STATUSES.contains(item.getStatusCode()))
                .sorted(todoPriorityComparator())
                .map(item -> toSummaryResponse(item, profileById, access, false))
                .toList();
        List<TodoSummaryResponse> claimableTodos = claimableItems.stream()
                .map(item -> toSummaryResponse(item, profileById, access, false))
                .toList();
        List<TodoSummaryResponse> recentCompletedTodos = recentCompletedItems.stream()
                .map(item -> toSummaryResponse(item, profileById, access, false))
                .toList();

        return new ClubTodoResponse(
                access.club().getClubId(),
                access.club().getName(),
                access.isAdmin(),
                myTodos.size(),
                (int) myItems.stream().filter(item -> STATUS_COMPLETED.equals(item.getStatusCode())).count(),
                claimableTodos.size(),
                (int) myItems.stream().filter(this::isOverdue).count(),
                myTodos,
                claimableTodos,
                recentCompletedTodos
        );
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "할 일")
    public TodoActionResponse claimTodo(Long clubId, Long todoItemId, String userKey) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        requireTodoFeature(clubId);
        TodoItem current = requireTodoItem(clubId, todoItemId);
        if (!ASSIGNMENT_MODE_OPEN_SUPPORT.equals(current.getAssignmentMode())) {
            throw new SemoException.ValidationException("지원 가능한 업무만 맡을 수 있습니다.");
        }
        if (current.getAssignedClubProfileId() != null) {
            throw new SemoException.ConflictException("이미 다른 멤버가 맡은 업무입니다.");
        }
        if (!STATUS_OPEN.equals(current.getStatusCode())) {
            throw new SemoException.ValidationException("현재 맡을 수 없는 상태의 업무입니다.");
        }

        TodoItem updated = todoItemRepository.save(TodoItem.builder()
                .todoItemId(current.getTodoItemId())
                .clubId(current.getClubId())
                .createdByClubProfileId(current.getCreatedByClubProfileId())
                .assignedClubProfileId(access.clubProfile().getClubProfileId())
                .assignedByClubProfileId(access.clubProfile().getClubProfileId())
                .todoType(current.getTodoType())
                .assignmentMode(current.getAssignmentMode())
                .statusCode(STATUS_IN_PROGRESS)
                .title(current.getTitle())
                .description(current.getDescription())
                .dueAt(current.getDueAt())
                .completedByClubProfileId(null)
                .completedAt(null)
                .build());

        ClubActivityContextHolder.setDetails(
                "'" + current.getTitle() + "' 업무를 맡았습니다.",
                "'" + current.getTitle() + "' 업무를 맡지 못했습니다."
        );
        Map<Long, ClubProfile> profileById = resolveClubProfiles(List.of(List.of(updated)));
        return toActionResponse(updated, profileById);
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "할 일")
    public TodoActionResponse completeTodo(Long clubId, Long todoItemId, String userKey) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        requireTodoFeature(clubId);
        TodoItem current = requireTodoItem(clubId, todoItemId);
        if (!Objects.equals(current.getAssignedClubProfileId(), access.clubProfile().getClubProfileId())) {
            throw new SemoException.ForbiddenException("본인에게 배정된 업무만 완료 처리할 수 있습니다.");
        }
        if (TERMINAL_STATUSES.contains(current.getStatusCode())) {
            throw new SemoException.ValidationException("이미 종료된 업무입니다.");
        }
        if (ASSIGNMENT_MODE_OPEN_SUPPORT.equals(current.getAssignmentMode())
                && !STATUS_IN_PROGRESS.equals(current.getStatusCode())) {
            throw new SemoException.ForbiddenException("지원 업무는 먼저 맡은 뒤 완료 처리해야 합니다.");
        }

        TodoItem updated = saveWithStatus(current, STATUS_COMPLETED, access.clubProfile().getClubProfileId());
        ClubActivityContextHolder.setDetails(
                "'" + current.getTitle() + "' 업무를 완료했습니다.",
                "'" + current.getTitle() + "' 업무를 완료하지 못했습니다."
        );
        Map<Long, ClubProfile> profileById = resolveClubProfiles(List.of(List.of(updated)));
        return toActionResponse(updated, profileById);
    }

    public ClubAdminTodoResponse getAdminTodos(
            Long clubId,
            String userKey,
            String statusFilter,
            String assignmentFilter,
            Long cursorTodoItemId,
            Integer size
    ) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        requireTodoFeature(clubId);
        requireAdminTodoView(access);

        List<TodoItem> allItems = todoItemRepository.findByClubIdOrderByTodoItemIdDesc(clubId);
        List<ClubAccessResolver.ClubMemberSnapshot> activeMembers = clubAccessResolver.getActiveMemberSnapshots(clubId);
        List<TodoMemberOptionResponse> availableMembers = activeMembers.stream()
                .map(snapshot -> new TodoMemberOptionResponse(
                        snapshot.clubProfile().getClubProfileId(),
                        snapshot.clubProfile().getDisplayName(),
                        snapshot.membership().getRoleCode()
                ))
                .toList();

        int pageSize = normalizeAdminPageSize(size);
        String normalizedStatusFilter = normalizeStatusFilter(statusFilter);
        String normalizedAssignmentFilter = normalizeAssignmentFilter(assignmentFilter);
        LocalDateTime now = LocalDateTime.now();
        List<TodoItem> feed = todoItemRepository.findAdminFeed(
                clubId,
                normalizedStatusFilter,
                normalizedAssignmentFilter,
                cursorTodoItemId,
                now,
                TERMINAL_STATUSES,
                PageRequest.of(0, pageSize + 1)
        );
        boolean hasNext = feed.size() > pageSize;
        List<TodoItem> pageItems = hasNext ? feed.subList(0, pageSize) : feed;
        TodoItem lastItem = pageItems.isEmpty() ? null : pageItems.get(pageItems.size() - 1);
        Map<Long, ClubProfile> profileById = resolveClubProfiles(List.of(allItems, pageItems));

        return new ClubAdminTodoResponse(
                access.club().getClubId(),
                access.club().getName(),
                access.isAdmin(),
                clubTodoPermissionService.canCreateTodo(access),
                clubTodoPermissionService.canAssignTodo(access),
                clubTodoPermissionService.canManageStatus(access),
                activeMembers.size(),
                (int) allItems.stream().filter(item -> STATUS_OPEN.equals(item.getStatusCode())).count(),
                (int) allItems.stream().filter(item -> STATUS_IN_PROGRESS.equals(item.getStatusCode())).count(),
                (int) allItems.stream().filter(item -> STATUS_COMPLETED.equals(item.getStatusCode())).count(),
                (int) allItems.stream().filter(this::isOverdue).count(),
                availableMembers,
                pageItems.stream()
                        .map(item -> toSummaryResponse(item, profileById, access, true))
                        .toList(),
                lastItem == null ? null : lastItem.getTodoItemId(),
                hasNext
        );
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "할 일관리")
    public TodoSummaryResponse createTodo(Long clubId, String userKey, CreateClubTodoRequest request) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        requireTodoFeature(clubId);
        boolean canCreate = clubTodoPermissionService.canCreateTodo(access);
        boolean canAssign = clubTodoPermissionService.canAssignTodo(access);
        if (!canCreate) {
            throw new SemoException.ForbiddenException("할 일을 등록할 권한이 없습니다.");
        }

        String title = normalizeTitle(request.title());
        String description = trimToNull(request.description());
        String todoType = normalizeTodoType(request.todoType());
        String assignmentMode = normalizeAssignmentMode(request.assignmentMode());
        LocalDateTime dueAt = parseDateTime(request.dueAt(), "마감일 형식이 잘못되었습니다.");
        Map<Long, ClubAccessResolver.ClubMemberSnapshot> activeMemberByProfileId = resolveActiveMembersByProfileId(clubId);
        Long assignedClubProfileId = resolveAssignedClubProfileId(
                assignmentMode,
                request.assignedClubProfileId(),
                activeMemberByProfileId
        );
        if (assignedClubProfileId != null && !canAssign) {
            throw new SemoException.ForbiddenException("담당자를 배정할 권한이 없습니다.");
        }

        TodoItem saved = todoItemRepository.save(TodoItem.builder()
                .clubId(clubId)
                .createdByClubProfileId(access.clubProfile().getClubProfileId())
                .assignedClubProfileId(assignedClubProfileId)
                .assignedByClubProfileId(assignedClubProfileId == null ? null : access.clubProfile().getClubProfileId())
                .todoType(todoType)
                .assignmentMode(assignmentMode)
                .statusCode(STATUS_OPEN)
                .title(title)
                .description(description)
                .dueAt(dueAt)
                .completedByClubProfileId(null)
                .completedAt(null)
                .build());

        ClubActivityContextHolder.setDetails(
                "'" + title + "' 할 일을 등록했습니다.",
                "'" + title + "' 할 일을 등록하지 못했습니다."
        );
        Map<Long, ClubProfile> profileById = resolveClubProfiles(List.of(List.of(saved)));
        return toSummaryResponse(saved, profileById, access, true);
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "할 일관리")
    public TodoSummaryResponse updateTodo(Long clubId, Long todoItemId, String userKey, UpdateClubTodoRequest request) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        requireTodoFeature(clubId);
        TodoItem current = requireTodoItem(clubId, todoItemId);
        boolean canCreate = clubTodoPermissionService.canCreateTodo(access);
        boolean canAssign = clubTodoPermissionService.canAssignTodo(access);
        if (!canCreate && !canAssign) {
            throw new SemoException.ForbiddenException("할 일을 수정할 권한이 없습니다.");
        }
        if (TERMINAL_STATUSES.contains(current.getStatusCode())) {
            throw new SemoException.ValidationException("완료되거나 취소된 업무는 다시 열어야 수정할 수 있습니다.");
        }

        String title = normalizeTitle(request.title());
        String description = trimToNull(request.description());
        String todoType = normalizeTodoType(request.todoType());
        String assignmentMode = normalizeAssignmentMode(request.assignmentMode());
        LocalDateTime dueAt = parseDateTime(request.dueAt(), "마감일 형식이 잘못되었습니다.");
        Map<Long, ClubAccessResolver.ClubMemberSnapshot> activeMemberByProfileId = resolveActiveMembersByProfileId(clubId);
        Long assignedClubProfileId = resolveUpdatedAssignedClubProfileId(
                current,
                assignmentMode,
                request.assignedClubProfileId(),
                activeMemberByProfileId
        );
        boolean metadataChanged = !Objects.equals(current.getTitle(), title)
                || !Objects.equals(current.getDescription(), description)
                || !Objects.equals(current.getTodoType(), todoType)
                || !Objects.equals(current.getDueAt(), dueAt);
        boolean assignmentChanged = !Objects.equals(current.getAssignmentMode(), assignmentMode)
                || !Objects.equals(current.getAssignedClubProfileId(), assignedClubProfileId);
        if (metadataChanged && !canCreate) {
            throw new SemoException.ForbiddenException("업무 기본 정보를 수정할 권한이 없습니다.");
        }
        if (assignmentChanged && !canAssign) {
            throw new SemoException.ForbiddenException("담당자를 조정할 권한이 없습니다.");
        }
        String nextStatus = current.getStatusCode();
        if (ASSIGNMENT_MODE_OPEN_SUPPORT.equals(assignmentMode) && assignedClubProfileId == null) {
            nextStatus = STATUS_OPEN;
        }

        Long assignedByClubProfileId = assignedClubProfileId == null
                ? null
                : assignmentChanged
                ? access.clubProfile().getClubProfileId()
                : current.getAssignedByClubProfileId();

        TodoItem updated = todoItemRepository.save(TodoItem.builder()
                .todoItemId(current.getTodoItemId())
                .clubId(current.getClubId())
                .createdByClubProfileId(current.getCreatedByClubProfileId())
                .assignedClubProfileId(assignedClubProfileId)
                .assignedByClubProfileId(assignedByClubProfileId)
                .todoType(todoType)
                .assignmentMode(assignmentMode)
                .statusCode(nextStatus)
                .title(title)
                .description(description)
                .dueAt(dueAt)
                .completedByClubProfileId(null)
                .completedAt(null)
                .build());

        ClubActivityContextHolder.setDetails(
                "'" + title + "' 할 일을 수정했습니다.",
                "'" + title + "' 할 일을 수정하지 못했습니다."
        );
        Map<Long, ClubProfile> profileById = resolveClubProfiles(List.of(List.of(updated)));
        return toSummaryResponse(updated, profileById, access, true);
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "할 일관리")
    public TodoActionResponse updateTodoStatus(
            Long clubId,
            Long todoItemId,
            String userKey,
            UpdateTodoStatusRequest request
    ) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        requireTodoFeature(clubId);
        if (!clubTodoPermissionService.canManageStatus(access)) {
            throw new SemoException.ForbiddenException("할 일 상태를 변경할 권한이 없습니다.");
        }

        TodoItem current = requireTodoItem(clubId, todoItemId);
        String nextStatus = normalizeStatusCode(request.statusCode());
        validateStatusTransition(current, nextStatus);

        TodoItem updated = saveWithStatus(current, nextStatus, access.clubProfile().getClubProfileId());
        ClubActivityContextHolder.setDetails(
                "'" + current.getTitle() + "' 상태를 " + toStatusLabel(nextStatus) + "(으)로 변경했습니다.",
                "'" + current.getTitle() + "' 상태를 변경하지 못했습니다."
        );
        Map<Long, ClubProfile> profileById = resolveClubProfiles(List.of(List.of(updated)));
        return toActionResponse(updated, profileById);
    }

    private void requireTodoFeature(Long clubId) {
        if (!clubTodoPermissionService.isTodoEnabled(clubId)) {
            throw new SemoException.ForbiddenException("할 일 기능이 활성화되지 않았습니다.");
        }
    }

    private void requireAdminTodoView(ClubAccessResolver.ClubAccess access) {
        if (!clubTodoPermissionService.canViewAdminTodos(access)) {
            throw new SemoException.ForbiddenException("할 일 운영 화면을 조회할 권한이 없습니다.");
        }
    }

    private TodoItem requireTodoItem(Long clubId, Long todoItemId) {
        return todoItemRepository.findByTodoItemIdAndClubId(todoItemId, clubId)
                .orElseThrow(() -> new SemoException.ResourceNotFoundException("TodoItem", "todoItemId", todoItemId));
    }

    private Map<Long, ClubAccessResolver.ClubMemberSnapshot> resolveActiveMembersByProfileId(Long clubId) {
        return clubAccessResolver.getActiveMemberSnapshots(clubId).stream()
                .collect(Collectors.toMap(
                        snapshot -> snapshot.clubProfile().getClubProfileId(),
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
    }

    private Long resolveAssignedClubProfileId(
            String assignmentMode,
            Long requestedAssignedClubProfileId,
            Map<Long, ClubAccessResolver.ClubMemberSnapshot> activeMemberByProfileId
    ) {
        if (ASSIGNMENT_MODE_DIRECT_ASSIGN.equals(assignmentMode)) {
            if (requestedAssignedClubProfileId == null) {
                throw new SemoException.ValidationException("직접 배정 업무는 담당자를 선택해야 합니다.");
            }
            if (!activeMemberByProfileId.containsKey(requestedAssignedClubProfileId)) {
                throw new SemoException.ValidationException("현재 활성 멤버에게만 업무를 배정할 수 있습니다.");
            }
            return requestedAssignedClubProfileId;
        }

        if (requestedAssignedClubProfileId != null) {
            throw new SemoException.ValidationException("지원 가능 업무는 담당자를 비워둬야 합니다.");
        }
        return null;
    }

    private Long resolveUpdatedAssignedClubProfileId(
            TodoItem current,
            String assignmentMode,
            Long requestedAssignedClubProfileId,
            Map<Long, ClubAccessResolver.ClubMemberSnapshot> activeMemberByProfileId
    ) {
        if (ASSIGNMENT_MODE_DIRECT_ASSIGN.equals(assignmentMode)
                && ASSIGNMENT_MODE_DIRECT_ASSIGN.equals(current.getAssignmentMode())
                && Objects.equals(current.getAssignedClubProfileId(), requestedAssignedClubProfileId)) {
            if (requestedAssignedClubProfileId == null) {
                throw new SemoException.ValidationException("직접 배정 업무는 담당자를 선택해야 합니다.");
            }
            return requestedAssignedClubProfileId;
        }
        return resolveAssignedClubProfileId(assignmentMode, requestedAssignedClubProfileId, activeMemberByProfileId);
    }

    private String normalizeTitle(String title) {
        String normalized = trimToNull(title);
        if (normalized == null) {
            throw new SemoException.ValidationException("업무 이름은 필수입니다.");
        }
        if (normalized.length() > 150) {
            throw new SemoException.ValidationException("업무 이름은 150자 이하여야 합니다.");
        }
        return normalized;
    }

    private String normalizeTodoType(String todoType) {
        String normalized = trimToNull(todoType);
        if (normalized == null) {
            throw new SemoException.ValidationException("업무 유형은 필수입니다.");
        }
        normalized = normalized.toUpperCase(Locale.ROOT);
        if (!ALLOWED_TODO_TYPES.contains(normalized)) {
            throw new SemoException.ValidationException("지원하지 않는 업무 유형입니다.");
        }
        return normalized;
    }

    private String normalizeAssignmentMode(String assignmentMode) {
        String normalized = trimToNull(assignmentMode);
        if (normalized == null) {
            throw new SemoException.ValidationException("배정 방식은 필수입니다.");
        }
        normalized = normalized.toUpperCase(Locale.ROOT);
        if (!ALLOWED_ASSIGNMENT_MODES.contains(normalized)) {
            throw new SemoException.ValidationException("지원하지 않는 배정 방식입니다.");
        }
        return normalized;
    }

    private String normalizeStatusCode(String statusCode) {
        String normalized = trimToNull(statusCode);
        if (normalized == null) {
            throw new SemoException.ValidationException("상태 코드는 필수입니다.");
        }
        normalized = normalized.toUpperCase(Locale.ROOT);
        if (!DIRECT_FILTER_STATUSES.contains(normalized)) {
            throw new SemoException.ValidationException("지원하지 않는 상태 코드입니다.");
        }
        return normalized;
    }

    private String normalizeStatusFilter(String statusFilter) {
        String normalized = trimToNull(statusFilter);
        if (normalized == null) {
            return "ALL";
        }
        normalized = normalized.toUpperCase(Locale.ROOT);
        if ("ALL".equals(normalized)) {
            return normalized;
        }
        if (!ADMIN_FILTERABLE_STATUSES.contains(normalized)) {
            throw new SemoException.ValidationException("지원하지 않는 상태 필터입니다.");
        }
        return normalized;
    }

    private String normalizeAssignmentFilter(String assignmentFilter) {
        String normalized = trimToNull(assignmentFilter);
        if (normalized == null) {
            return "ALL";
        }
        normalized = normalized.toUpperCase(Locale.ROOT);
        if (!ALLOWED_ASSIGNMENT_FILTERS.contains(normalized)) {
            throw new SemoException.ValidationException("지원하지 않는 배정 필터입니다.");
        }
        return normalized;
    }

    private int normalizeAdminPageSize(Integer size) {
        if (size == null) {
            return DEFAULT_ADMIN_PAGE_SIZE;
        }
        if (size < 1 || size > MAX_ADMIN_PAGE_SIZE) {
            throw new SemoException.ValidationException("페이지 크기는 1~30 범위여야 합니다.");
        }
        return size;
    }

    private LocalDateTime parseDateTime(String value, String errorMessage) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }
        try {
            return LocalDateTime.parse(normalized, REQUEST_DATE_TIME_FORMATTER);
        } catch (RuntimeException exception) {
            throw new SemoException.ValidationException(errorMessage);
        }
    }

    private void validateStatusTransition(TodoItem current, String nextStatus) {
        if (STATUS_OPEN.equals(nextStatus)) {
            return;
        }
        if (STATUS_IN_PROGRESS.equals(nextStatus)) {
            if (current.getAssignedClubProfileId() == null) {
                throw new SemoException.ValidationException("담당자가 없는 업무는 진행중으로 바꿀 수 없습니다.");
            }
            if (STATUS_COMPLETED.equals(current.getStatusCode()) || STATUS_CANCELED.equals(current.getStatusCode())) {
                throw new SemoException.ValidationException("종료된 업무는 먼저 다시 열어야 합니다.");
            }
            return;
        }
        if (STATUS_COMPLETED.equals(nextStatus)) {
            if (current.getAssignedClubProfileId() == null) {
                throw new SemoException.ValidationException("담당자가 있는 업무만 완료 처리할 수 있습니다.");
            }
            if (STATUS_CANCELED.equals(current.getStatusCode())) {
                throw new SemoException.ValidationException("취소된 업무는 먼저 다시 열어야 합니다.");
            }
            return;
        }
        if (STATUS_CANCELED.equals(nextStatus)) {
            if (STATUS_COMPLETED.equals(current.getStatusCode())) {
                throw new SemoException.ValidationException("완료된 업무는 먼저 다시 열어야 합니다.");
            }
            return;
        }
        throw new SemoException.ValidationException("지원하지 않는 상태 전이입니다.");
    }

    private TodoItem saveWithStatus(TodoItem current, String nextStatus, Long actorClubProfileId) {
        LocalDateTime completedAt = STATUS_COMPLETED.equals(nextStatus) ? LocalDateTime.now() : null;
        Long completedByClubProfileId = STATUS_COMPLETED.equals(nextStatus) ? actorClubProfileId : null;
        return todoItemRepository.save(TodoItem.builder()
                .todoItemId(current.getTodoItemId())
                .clubId(current.getClubId())
                .createdByClubProfileId(current.getCreatedByClubProfileId())
                .assignedClubProfileId(current.getAssignedClubProfileId())
                .assignedByClubProfileId(current.getAssignedByClubProfileId())
                .todoType(current.getTodoType())
                .assignmentMode(current.getAssignmentMode())
                .statusCode(nextStatus)
                .title(current.getTitle())
                .description(current.getDescription())
                .dueAt(current.getDueAt())
                .completedByClubProfileId(completedByClubProfileId)
                .completedAt(completedAt)
                .build());
    }

    private Map<Long, ClubProfile> resolveClubProfiles(List<List<TodoItem>> groups) {
        List<Long> ids = groups.stream()
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .flatMap(item -> Stream.of(
                        item.getCreatedByClubProfileId(),
                        item.getAssignedClubProfileId(),
                        item.getCompletedByClubProfileId()
                ))
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return clubProfileRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(ClubProfile::getClubProfileId, Function.identity()));
    }

    private Comparator<TodoItem> todoPriorityComparator() {
        return Comparator
                .comparing((TodoItem item) -> !isOverdue(item))
                .thenComparing(item -> item.getStatusCode(), Comparator.comparingInt(this::statusSortOrder))
                .thenComparing(item -> item.getDueAt() == null)
                .thenComparing(TodoItem::getDueAt, Comparator.nullsLast(LocalDateTime::compareTo))
                .thenComparing(TodoItem::getTodoItemId, Comparator.reverseOrder());
    }

    private int statusSortOrder(String statusCode) {
        return switch (statusCode) {
            case STATUS_OPEN -> 0;
            case STATUS_IN_PROGRESS -> 1;
            case STATUS_COMPLETED -> 2;
            default -> 3;
        };
    }

    private boolean isOverdue(TodoItem item) {
        return item.getDueAt() != null
                && item.getDueAt().isBefore(LocalDateTime.now())
                && !TERMINAL_STATUSES.contains(item.getStatusCode());
    }

    private TodoSummaryResponse toSummaryResponse(
            TodoItem item,
            Map<Long, ClubProfile> profileById,
            ClubAccessResolver.ClubAccess access,
            boolean adminView
    ) {
        boolean assignedToViewer = Objects.equals(item.getAssignedClubProfileId(), access.clubProfile().getClubProfileId());
        boolean canClaim = ASSIGNMENT_MODE_OPEN_SUPPORT.equals(item.getAssignmentMode())
                && item.getAssignedClubProfileId() == null
                && STATUS_OPEN.equals(item.getStatusCode());
        boolean canComplete = assignedToViewer
                && !TERMINAL_STATUSES.contains(item.getStatusCode())
                && (!ASSIGNMENT_MODE_OPEN_SUPPORT.equals(item.getAssignmentMode())
                    || STATUS_IN_PROGRESS.equals(item.getStatusCode()));
        boolean canEdit = adminView && clubTodoPermissionService.canCreateTodo(access);
        boolean canManageStatus = adminView && clubTodoPermissionService.canManageStatus(access);

        return new TodoSummaryResponse(
                item.getTodoItemId(),
                item.getTitle(),
                item.getDescription(),
                item.getTodoType(),
                toTodoTypeLabel(item.getTodoType()),
                item.getAssignmentMode(),
                toAssignmentModeLabel(item.getAssignmentMode()),
                item.getStatusCode(),
                toStatusLabel(item.getStatusCode()),
                formatDateTime(item.getDueAt()),
                formatDateTimeLabel(item.getDueAt()),
                isOverdue(item),
                item.getAssignedClubProfileId(),
                resolveDisplayName(profileById, item.getAssignedClubProfileId()),
                resolveDisplayName(profileById, item.getCreatedByClubProfileId()),
                resolveDisplayName(profileById, item.getCompletedByClubProfileId()),
                formatDateTime(item.getCompletedAt()),
                formatDateTimeLabel(item.getCompletedAt()),
                canClaim,
                canComplete,
                canEdit,
                canManageStatus
        );
    }

    private TodoActionResponse toActionResponse(TodoItem item, Map<Long, ClubProfile> profileById) {
        return new TodoActionResponse(
                item.getTodoItemId(),
                item.getStatusCode(),
                toStatusLabel(item.getStatusCode()),
                item.getAssignedClubProfileId(),
                resolveDisplayName(profileById, item.getAssignedClubProfileId()),
                item.getCompletedByClubProfileId(),
                resolveDisplayName(profileById, item.getCompletedByClubProfileId()),
                formatDateTime(item.getCompletedAt()),
                formatDateTimeLabel(item.getCompletedAt())
        );
    }

    private String resolveDisplayName(Map<Long, ClubProfile> profileById, Long clubProfileId) {
        if (clubProfileId == null) {
            return null;
        }
        ClubProfile clubProfile = profileById.get(clubProfileId);
        return clubProfile == null ? null : clubProfile.getDisplayName();
    }

    private String toTodoTypeLabel(String todoType) {
        return switch (todoType) {
            case TODO_TYPE_VOLUNTEER -> "봉사";
            case TODO_TYPE_OPERATIONS -> "운영";
            default -> todoType;
        };
    }

    private String toAssignmentModeLabel(String assignmentMode) {
        return switch (assignmentMode) {
            case ASSIGNMENT_MODE_DIRECT_ASSIGN -> "직접 배정";
            case ASSIGNMENT_MODE_OPEN_SUPPORT -> "지원 가능";
            default -> assignmentMode;
        };
    }

    private String toStatusLabel(String statusCode) {
        return switch (statusCode) {
            case STATUS_OPEN -> "열림";
            case STATUS_IN_PROGRESS -> "진행중";
            case STATUS_COMPLETED -> "완료";
            case STATUS_CANCELED -> "취소";
            default -> statusCode;
        };
    }

    private String formatDateTime(LocalDateTime value) {
        if (value == null) {
            return null;
        }
        return value.format(REQUEST_DATE_TIME_FORMATTER);
    }

    private String formatDateTimeLabel(LocalDateTime value) {
        if (value == null) {
            return null;
        }
        return value.format(DATE_TIME_LABEL_FORMATTER);
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
