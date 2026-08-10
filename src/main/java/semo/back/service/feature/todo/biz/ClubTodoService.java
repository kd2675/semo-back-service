package semo.back.service.feature.todo.biz;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import semo.back.service.common.exception.SemoException;
import semo.back.service.database.pub.entity.ClubProfile;
import semo.back.service.database.pub.entity.ClubScheduleEvent;
import semo.back.service.database.pub.entity.DecisionRecord;
import semo.back.service.database.pub.entity.TodoChecklistItem;
import semo.back.service.database.pub.entity.TodoItem;
import semo.back.service.database.pub.entity.TodoItemApplication;
import semo.back.service.database.pub.entity.TodoItemAssignee;
import semo.back.service.database.pub.repository.ClubScheduleEventRepository;
import semo.back.service.database.pub.repository.ClubProfileRepository;
import semo.back.service.database.pub.repository.DecisionRecordRepository;
import semo.back.service.database.pub.repository.TodoChecklistItemRepository;
import semo.back.service.database.pub.repository.TodoItemApplicationRepository;
import semo.back.service.database.pub.repository.TodoItemAssigneeRepository;
import semo.back.service.database.pub.repository.TodoItemRepository;
import semo.back.service.feature.activity.biz.ClubActivityContextHolder;
import semo.back.service.feature.activity.biz.RecordClubActivity;
import semo.back.service.feature.club.biz.policy.ClubAccessResolver;
import semo.back.service.feature.notification.biz.ClubNotificationPublisher;
import semo.back.service.feature.notification.biz.ClubNotificationPublisher.NotificationCommand;
import semo.back.service.feature.todo.biz.policy.ClubTodoPermissionService;
import semo.back.service.feature.todo.biz.support.ClubTodoCommandSupport;
import semo.back.service.feature.todo.biz.support.ClubTodoViewSupport;
import semo.back.service.feature.todo.vo.ClubAdminTodoResponse;
import semo.back.service.feature.todo.vo.ClubTodoResponse;
import semo.back.service.feature.todo.vo.CreateClubTodoRequest;
import semo.back.service.feature.todo.vo.CreateTodoApplicationRequest;
import semo.back.service.feature.todo.vo.ReviewTodoItemApplicationRequest;
import semo.back.service.feature.todo.vo.TodoActionResponse;
import semo.back.service.feature.todo.vo.TodoAssigneeResponse;
import semo.back.service.feature.todo.vo.TodoDecisionOptionResponse;
import semo.back.service.feature.todo.vo.TodoItemApplicationResponse;
import semo.back.service.feature.todo.vo.TodoItemApplicationsResponse;
import semo.back.service.feature.todo.vo.TodoMemberOptionResponse;
import semo.back.service.feature.todo.vo.TodoScheduleOptionResponse;
import semo.back.service.feature.todo.vo.TodoSummaryResponse;
import semo.back.service.feature.todo.vo.UpdateClubTodoRequest;
import semo.back.service.feature.todo.vo.UpdateTodoStatusRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
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
    private static final String ASSIGNMENT_MODE_DIRECT_ASSIGN = "DIRECT_ASSIGN";
    private static final String ASSIGNMENT_MODE_OPEN_SUPPORT = "OPEN_SUPPORT";

    private static final String STATUS_OPEN = "OPEN";
    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_CANCELED = "CANCELED";
    private static final String STATUS_REOPEN = "REOPEN";

    private static final String APPLICATION_STATUS_APPLIED = "APPLIED";
    private static final String APPLICATION_STATUS_SELECTED = "SELECTED";
    private static final String APPLICATION_STATUS_REJECTED = "REJECTED";
    private static final String APPLICATION_STATUS_WITHDRAWN = "WITHDRAWN";
    private static final String RECURRENCE_NONE = "NONE";
    private static final String RECURRENCE_WEEKLY = "WEEKLY";
    private static final String RECURRENCE_MONTHLY = "MONTHLY";

    private static final int CLAIMABLE_PAGE_SIZE = 8;
    private static final int MAX_CLAIMABLE_PAGE_SIZE = 50;
    private static final DateTimeFormatter SCHEDULE_LABEL_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");

    private final ClubAccessResolver clubAccessResolver;
    private final ClubTodoPermissionService clubTodoPermissionService;
    private final TodoItemRepository todoItemRepository;
    private final TodoItemApplicationRepository todoItemApplicationRepository;
    private final TodoItemAssigneeRepository todoItemAssigneeRepository;
    private final TodoChecklistItemRepository todoChecklistItemRepository;
    private final ClubProfileRepository clubProfileRepository;
    private final ClubScheduleEventRepository clubScheduleEventRepository;
    private final DecisionRecordRepository decisionRecordRepository;
    private final ClubTodoCommandSupport clubTodoCommandSupport;
    private final ClubTodoViewSupport clubTodoViewSupport;
    private final ClubNotificationPublisher clubNotificationPublisher;

    public ClubTodoResponse getTodos(Long clubId, String userKey) {
        return getTodos(clubId, userKey, CLAIMABLE_PAGE_SIZE);
    }

    public ClubTodoResponse getTodos(Long clubId, String userKey, Integer claimableSize) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        requireTodoFeature(clubId);

        int normalizedClaimableSize = claimableSize == null
                ? CLAIMABLE_PAGE_SIZE
                : Math.max(1, Math.min(claimableSize, MAX_CLAIMABLE_PAGE_SIZE));

        List<TodoItem> myItems = todoItemRepository.findAssignedTodos(
                clubId,
                access.clubProfile().getClubProfileId()
        );
        long claimableOpenCount = todoItemRepository.countClaimableTodos(clubId);
        List<TodoItem> claimableItems = todoItemRepository.findClaimableTodos(
                clubId,
                PageRequest.of(0, normalizedClaimableSize)
        );
        List<TodoItem> recentCompletedItems = todoItemRepository.findTop5ByClubIdAndCompletedByClubProfileIdOrderByTodoItemIdDesc(
                clubId,
                access.clubProfile().getClubProfileId()
        );
        List<TodoItemApplication> myAllApplications = todoItemApplicationRepository
                .findByClubProfileId(access.clubProfile().getClubProfileId());
        List<Long> myAppliedTodoIds = myAllApplications.stream()
                .map(TodoItemApplication::getTodoItemId)
                .distinct()
                .toList();
        Map<Long, TodoItem> appliedTodoById = myAppliedTodoIds.isEmpty()
                ? Map.of()
                : todoItemRepository.findAllById(myAppliedTodoIds).stream()
                        .filter(item -> Objects.equals(item.getClubId(), clubId))
                        .collect(Collectors.toMap(TodoItem::getTodoItemId, Function.identity()));
        List<TodoItem> prioritizedClaimableItems = Stream.concat(
                        myAllApplications.stream()
                                .map(application -> appliedTodoById.get(application.getTodoItemId()))
                                .filter(Objects::nonNull)
                                .filter(item -> ASSIGNMENT_MODE_OPEN_SUPPORT.equals(item.getAssignmentMode()))
                                .filter(this::isRecruitmentAvailable),
                        claimableItems.stream()
                )
                .distinct()
                .sorted(clubTodoViewSupport.todoPriorityComparator())
                .limit(normalizedClaimableSize)
                .toList();
        List<Long> visibleTodoIds = Stream.of(myItems, prioritizedClaimableItems, recentCompletedItems)
                .flatMap(Collection::stream)
                .map(TodoItem::getTodoItemId)
                .distinct()
                .toList();
        List<TodoItemApplication> visibleApplications = visibleTodoIds.isEmpty()
                ? List.of()
                : todoItemApplicationRepository.findByTodoItemIdIn(visibleTodoIds);
        List<TodoItemApplication> myVisibleApplications = visibleTodoIds.isEmpty()
                ? List.of()
                : todoItemApplicationRepository.findByTodoItemIdInAndClubProfileId(
                        visibleTodoIds,
                        access.clubProfile().getClubProfileId()
                );
        List<TodoItemAssignee> visibleAssignees = visibleTodoIds.isEmpty()
                ? List.of()
                : todoItemAssigneeRepository.findByTodoItemIdInOrderByTodoItemAssigneeIdAsc(visibleTodoIds);
        Map<Long, List<Long>> assigneeIdsByTodoItemId = resolveAssigneeIdsByTodoItemId(
                List.of(myItems, prioritizedClaimableItems, recentCompletedItems),
                visibleAssignees
        );
        Map<Long, ClubProfile> profileById = clubTodoViewSupport.resolveClubProfiles(
                List.of(myItems, prioritizedClaimableItems, recentCompletedItems),
                List.of(visibleApplications),
                List.of(visibleAssignees)
        );
        Map<Long, ClubScheduleEvent> scheduleById = resolveScheduleById(
                List.of(myItems, prioritizedClaimableItems, recentCompletedItems)
        );
        Map<Long, DecisionRecord> decisionById = resolveDecisionById(
                List.of(myItems, prioritizedClaimableItems, recentCompletedItems)
        );
        Map<Long, Integer> applicationCountByTodoItemId =
                clubTodoViewSupport.resolveApplicationCountByTodoItemId(visibleApplications);
        Map<Long, TodoItemApplication> myApplicationByTodoItemId = myVisibleApplications.stream()
                .collect(Collectors.toMap(
                        TodoItemApplication::getTodoItemId,
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        List<TodoSummaryResponse> myTodos = myItems.stream()
                .filter(item -> !STATUS_COMPLETED.equals(item.getStatusCode()) && !STATUS_CANCELED.equals(item.getStatusCode()))
                .sorted(clubTodoViewSupport.todoPriorityComparator())
                .map(item -> clubTodoViewSupport.toSummaryResponse(
                        item,
                        profileById,
                        access,
                        false,
                        applicationCountByTodoItemId,
                        myApplicationByTodoItemId,
                        assigneeIdsByTodoItemId,
                        scheduleById,
                        decisionById
                ))
                .toList();
        List<TodoSummaryResponse> prioritizedClaimableTodos = prioritizedClaimableItems.stream()
                .map(item -> clubTodoViewSupport.toSummaryResponse(
                        item,
                        profileById,
                        access,
                        false,
                        applicationCountByTodoItemId,
                        myApplicationByTodoItemId,
                        assigneeIdsByTodoItemId,
                        scheduleById,
                        decisionById
                ))
                .toList();
        List<TodoSummaryResponse> recentCompletedTodos = recentCompletedItems.stream()
                .map(item -> clubTodoViewSupport.toSummaryResponse(
                        item,
                        profileById,
                        access,
                        false,
                        applicationCountByTodoItemId,
                        myApplicationByTodoItemId,
                        assigneeIdsByTodoItemId,
                        scheduleById,
                        decisionById
                ))
                .toList();

        return new ClubTodoResponse(
                access.club().getClubId(),
                access.club().getName(),
                access.isAdmin(),
                myTodos.size(),
                (int) myItems.stream().filter(item -> STATUS_COMPLETED.equals(item.getStatusCode())).count(),
                (int) myAllApplications.stream()
                        .filter(application -> {
                            TodoItem item = appliedTodoById.get(application.getTodoItemId());
                            return item != null
                                    && ASSIGNMENT_MODE_OPEN_SUPPORT.equals(item.getAssignmentMode())
                                    && isRecruitmentAvailable(item);
                        })
                        .filter(application -> APPLICATION_STATUS_APPLIED.equals(application.getApplicationStatus()))
                        .count(),
                Math.toIntExact(Math.min(claimableOpenCount, Integer.MAX_VALUE)),
                (int) myItems.stream().filter(clubTodoViewSupport::isOverdue).count(),
                claimableOpenCount > prioritizedClaimableTodos.size(),
                myTodos,
                prioritizedClaimableTodos,
                recentCompletedTodos
        );
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "할 일")
    public TodoActionResponse claimTodo(Long clubId, Long todoItemId, String userKey) {
        applyTodo(clubId, todoItemId, userKey, null);
        TodoItem current = requireTodoItem(clubId, todoItemId);
        Map<Long, ClubProfile> profileById = clubTodoViewSupport.resolveClubProfiles(
                List.of(List.of(current)),
                List.of()
        );
        return clubTodoViewSupport.toActionResponse(current, profileById);
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "할 일")
    public TodoItemApplicationResponse applyTodo(
            Long clubId,
            Long todoItemId,
            String userKey,
            CreateTodoApplicationRequest request
    ) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        requireTodoFeature(clubId);
        TodoItem current = requireTodoItem(clubId, todoItemId);
        validateOpenSupportApplicationAvailable(current);

        String applicationNote = clubTodoCommandSupport.trimToNull(
                request == null ? null : request.applicationNote()
        );
        TodoItemApplication existing = todoItemApplicationRepository
                .findByTodoItemIdAndClubProfileId(todoItemId, access.clubProfile().getClubProfileId())
                .orElse(null);

        TodoItemApplication saved;
        if (existing == null) {
            saved = todoItemApplicationRepository.save(TodoItemApplication.builder()
                    .todoItemId(todoItemId)
                    .clubProfileId(access.clubProfile().getClubProfileId())
                    .applicationStatus(APPLICATION_STATUS_APPLIED)
                    .applicationNote(applicationNote)
                    .reviewNote(null)
                    .reviewedByClubProfileId(null)
                    .reviewedAt(null)
                    .build());
        } else {
            if (APPLICATION_STATUS_APPLIED.equals(existing.getApplicationStatus())) {
                throw new SemoException.ConflictException("이미 신청한 업무입니다.");
            }
            if (APPLICATION_STATUS_SELECTED.equals(existing.getApplicationStatus())) {
                throw new SemoException.ConflictException("이미 선정된 업무입니다.");
            }
            existing.markApplied(applicationNote);
            saved = todoItemApplicationRepository.save(existing);
        }

        ClubActivityContextHolder.setDetails(
                "'" + current.getTitle() + "' 업무에 신청했습니다.",
                "'" + current.getTitle() + "' 업무 신청에 실패했습니다."
        );
        Map<Long, ClubProfile> profileById = clubTodoViewSupport.resolveClubProfiles(
                List.of(List.of(current)),
                List.of(List.of(saved))
        );
        return clubTodoViewSupport.toApplicationResponse(
                saved,
                profileById,
                access.clubProfile().getClubProfileId(),
                false
        );
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "할 일")
    public TodoItemApplicationResponse cancelMyTodoApplication(Long clubId, Long todoItemId, String userKey) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        requireTodoFeature(clubId);
        TodoItem current = requireTodoItem(clubId, todoItemId);
        if (!ASSIGNMENT_MODE_OPEN_SUPPORT.equals(current.getAssignmentMode())) {
            throw new SemoException.ValidationException("신청형 업무만 취소할 수 있습니다.");
        }
        TodoItemApplication application = todoItemApplicationRepository
                .findByTodoItemIdAndClubProfileId(todoItemId, access.clubProfile().getClubProfileId())
                .orElseThrow(() -> new SemoException.ResourceNotFoundException(
                        "TodoItemApplication",
                        "todoItemId",
                        todoItemId
                ));
        if (!APPLICATION_STATUS_APPLIED.equals(application.getApplicationStatus())) {
            throw new SemoException.ValidationException("현재 취소할 수 없는 신청 상태입니다.");
        }

        application.withdraw();
        TodoItemApplication saved = todoItemApplicationRepository.save(application);

        ClubActivityContextHolder.setDetails(
                "'" + current.getTitle() + "' 업무 신청을 취소했습니다.",
                "'" + current.getTitle() + "' 업무 신청 취소에 실패했습니다."
        );
        Map<Long, ClubProfile> profileById = clubTodoViewSupport.resolveClubProfiles(
                List.of(List.of(current)),
                List.of(List.of(saved))
        );
        return clubTodoViewSupport.toApplicationResponse(
                saved,
                profileById,
                access.clubProfile().getClubProfileId(),
                false
        );
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "할 일")
    public TodoActionResponse completeTodo(Long clubId, Long todoItemId, String userKey) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        requireTodoFeature(clubId);
        TodoItem current = requireTodoItemForUpdate(clubId, todoItemId);
        if (!isAssignee(current, access.clubProfile().getClubProfileId())) {
            throw new SemoException.ForbiddenException("본인에게 배정된 업무만 완료 처리할 수 있습니다.");
        }
        if (STATUS_COMPLETED.equals(current.getStatusCode()) || STATUS_CANCELED.equals(current.getStatusCode())) {
            throw new SemoException.ValidationException("이미 종료된 업무입니다.");
        }
        if (ASSIGNMENT_MODE_OPEN_SUPPORT.equals(current.getAssignmentMode())
                && !STATUS_IN_PROGRESS.equals(current.getStatusCode())) {
            throw new SemoException.ForbiddenException("신청형 업무는 운영진 선정 이후에만 완료 처리할 수 있습니다.");
        }

        TodoItem updated = saveWithStatus(current, STATUS_COMPLETED, access.clubProfile().getClubProfileId());
        createNextRecurringTodo(updated, access.clubProfile().getClubProfileId());
        ClubActivityContextHolder.setDetails(
                "'" + current.getTitle() + "' 업무를 완료했습니다.",
                "'" + current.getTitle() + "' 업무를 완료하지 못했습니다."
        );
        Map<Long, ClubProfile> profileById = clubTodoViewSupport.resolveClubProfiles(
                List.of(List.of(updated)),
                List.of()
        );
        return clubTodoViewSupport.toActionResponse(updated, profileById);
    }

    public ClubAdminTodoResponse getAdminTodos(
            Long clubId,
            String userKey,
            String statusFilter,
            String assignmentFilter,
            String applicationFilter,
            Long cursorTodoItemId,
            Integer size
    ) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        requireTodoFeature(clubId);
        requireAdminTodoView(access);

        List<TodoItem> allItems = todoItemRepository.findByClubIdOrderByTodoItemIdDesc(clubId);
        List<Long> allTodoIds = allItems.stream().map(TodoItem::getTodoItemId).toList();
        List<TodoItemApplication> allApplications = allTodoIds.isEmpty()
                ? List.of()
                : todoItemApplicationRepository.findByTodoItemIdIn(allTodoIds);
        List<TodoItemAssignee> allAssignees = allTodoIds.isEmpty()
                ? List.of()
                : todoItemAssigneeRepository.findByTodoItemIdInOrderByTodoItemAssigneeIdAsc(allTodoIds);
        List<ClubAccessResolver.ClubMemberSnapshot> activeMembers = clubAccessResolver.getActiveMemberSnapshots(clubId);
        List<TodoMemberOptionResponse> availableMembers = activeMembers.stream()
                .map(snapshot -> new TodoMemberOptionResponse(
                        snapshot.clubProfile().getClubProfileId(),
                        snapshot.clubProfile().getDisplayName(),
                        snapshot.membership().getRoleCode()
                ))
                .toList();

        int pageSize = clubTodoCommandSupport.normalizeAdminPageSize(size);
        String normalizedStatusFilter = clubTodoCommandSupport.normalizeStatusFilter(statusFilter);
        String normalizedAssignmentFilter = clubTodoCommandSupport.normalizeAssignmentFilter(assignmentFilter);
        String normalizedApplicationFilter = clubTodoCommandSupport.normalizeApplicationFilter(applicationFilter);
        LocalDateTime now = LocalDateTime.now();
        List<TodoItem> feed = todoItemRepository.findAdminFeed(
                clubId,
                normalizedStatusFilter,
                normalizedAssignmentFilter,
                normalizedApplicationFilter,
                cursorTodoItemId,
                now,
                Set.of(STATUS_COMPLETED, STATUS_CANCELED),
                PageRequest.of(0, pageSize + 1)
        );
        boolean hasNext = feed.size() > pageSize;
        List<TodoItem> pageItems = hasNext ? feed.subList(0, pageSize) : feed;
        TodoItem lastItem = pageItems.isEmpty() ? null : pageItems.get(pageItems.size() - 1);
        Map<Long, List<Long>> assigneeIdsByTodoItemId = resolveAssigneeIdsByTodoItemId(
                List.of(allItems),
                allAssignees
        );
        Map<Long, ClubProfile> profileById = clubTodoViewSupport.resolveClubProfiles(
                List.of(allItems, pageItems),
                List.of(allApplications),
                List.of(allAssignees)
        );
        Map<Long, ClubScheduleEvent> scheduleById = resolveScheduleById(List.of(allItems));
        Map<Long, DecisionRecord> decisionById = resolveDecisionById(List.of(allItems));
        Map<Long, Integer> applicationCountByTodoItemId =
                clubTodoViewSupport.resolveApplicationCountByTodoItemId(allApplications);

        return new ClubAdminTodoResponse(
                access.club().getClubId(),
                access.club().getName(),
                access.isAdmin(),
                clubTodoPermissionService.canCreateTodo(access),
                clubTodoPermissionService.canAssignTodo(access),
                clubTodoPermissionService.canManageStatus(access),
                clubTodoPermissionService.canDeleteTodo(access),
                activeMembers.size(),
                (int) allItems.stream().filter(item -> STATUS_OPEN.equals(item.getStatusCode())).count(),
                (int) allItems.stream().filter(item -> STATUS_IN_PROGRESS.equals(item.getStatusCode())).count(),
                (int) allItems.stream().filter(item -> STATUS_COMPLETED.equals(item.getStatusCode())).count(),
                (int) allApplications.stream()
                        .filter(application -> APPLICATION_STATUS_APPLIED.equals(application.getApplicationStatus()))
                        .count(),
                (int) allItems.stream().filter(clubTodoViewSupport::isOverdue).count(),
                availableMembers,
                buildScheduleOptions(clubId),
                buildDecisionOptions(clubId),
                pageItems.stream()
                        .map(item -> clubTodoViewSupport.toSummaryResponse(
                                item,
                                profileById,
                                access,
                                true,
                                applicationCountByTodoItemId,
                                Map.of(),
                                assigneeIdsByTodoItemId,
                                scheduleById,
                                decisionById
                        ))
                        .toList(),
                lastItem == null ? null : lastItem.getTodoItemId(),
                hasNext
        );
    }

    public TodoItemApplicationsResponse getAdminTodoApplications(Long clubId, Long todoItemId, String userKey) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        requireTodoFeature(clubId);
        requireAdminTodoView(access);

        TodoItem todoItem = requireTodoItem(clubId, todoItemId);
        List<TodoItemApplication> applications = todoItemApplicationRepository
                .findByTodoItemIdOrderByCreateDateAscTodoItemApplicationIdAsc(todoItemId);
        List<TodoItemAssignee> assigneeEntities = todoItemAssigneeRepository
                .findByTodoItemIdOrderByTodoItemAssigneeIdAsc(todoItemId);
        Map<Long, ClubProfile> profileById = clubTodoViewSupport.resolveClubProfiles(
                List.of(List.of(todoItem)),
                List.of(applications),
                List.of(assigneeEntities)
        );
        List<Long> assigneeIds = resolveAssigneeIds(todoItem, assigneeEntities);
        List<TodoAssigneeResponse> assignees = assigneeIds.stream()
                .map(profileId -> new TodoAssigneeResponse(
                        profileId,
                        clubTodoViewSupport.resolveDisplayName(profileById, profileId)
                ))
                .toList();
        boolean canReview = clubTodoPermissionService.canAssignTodo(access)
                && ASSIGNMENT_MODE_OPEN_SUPPORT.equals(todoItem.getAssignmentMode());

        return new TodoItemApplicationsResponse(
                todoItem.getTodoItemId(),
                todoItem.getTitle(),
                todoItem.getAssignmentMode(),
                clubTodoViewSupport.toAssignmentModeLabel(todoItem.getAssignmentMode()),
                todoItem.getStatusCode(),
                clubTodoViewSupport.toStatusLabel(todoItem.getStatusCode()),
                assigneeIds.isEmpty() ? null : assigneeIds.getFirst(),
                assignees.stream()
                        .map(TodoAssigneeResponse::displayName)
                        .filter(Objects::nonNull)
                        .collect(Collectors.joining(", ")),
                assignees,
                normalizedRecruitmentCapacity(todoItem),
                applications.size(),
                (int) applications.stream()
                        .filter(application -> APPLICATION_STATUS_APPLIED.equals(application.getApplicationStatus()))
                        .count(),
                canReview,
                applications.stream()
                        .map(application -> clubTodoViewSupport.toApplicationResponse(
                                application,
                                profileById,
                                access.clubProfile().getClubProfileId(),
                                canReview
                        ))
                        .toList()
        );
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "할 일관리")
    public TodoItemApplicationResponse reviewTodoApplication(
            Long clubId,
            Long todoItemId,
            Long todoItemApplicationId,
            String userKey,
            ReviewTodoItemApplicationRequest request
    ) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        requireTodoFeature(clubId);
        if (!clubTodoPermissionService.canAssignTodo(access)) {
            throw new SemoException.ForbiddenException("업무 신청을 검토할 권한이 없습니다.");
        }

        TodoItem todoItem = requireTodoItemForUpdate(clubId, todoItemId);
        if (!ASSIGNMENT_MODE_OPEN_SUPPORT.equals(todoItem.getAssignmentMode())) {
            throw new SemoException.ValidationException("신청형 업무만 신청을 검토할 수 있습니다.");
        }

        TodoItemApplication application = requireTodoItemApplicationForUpdate(todoItemId, todoItemApplicationId);
        if (!APPLICATION_STATUS_APPLIED.equals(application.getApplicationStatus())) {
            throw new SemoException.ValidationException("대기 중인 신청만 검토할 수 있습니다.");
        }

        String nextStatus = clubTodoCommandSupport.normalizeApplicationReviewStatus(
                request.applicationStatus()
        );
        String reviewNote = clubTodoCommandSupport.trimToNull(request.reviewNote());
        LocalDateTime reviewedAt = LocalDateTime.now();

        List<TodoItemApplication> automaticallyRejectedApplications = List.of();
        if (APPLICATION_STATUS_SELECTED.equals(nextStatus)) {
            if (!isRecruitmentAvailable(todoItem)) {
                throw new SemoException.ValidationException("현재 선정할 수 없는 업무 상태입니다.");
            }
            todoItemAssigneeRepository.save(TodoItemAssignee.builder()
                    .todoItemId(todoItem.getTodoItemId())
                    .clubProfileId(application.getClubProfileId())
                    .assignedByClubProfileId(access.clubProfile().getClubProfileId())
                    .build());
            TodoItem updatedTodo = todoItemRepository.save(TodoItem.builder()
                    .todoItemId(todoItem.getTodoItemId())
                    .clubId(todoItem.getClubId())
                    .createdByClubProfileId(todoItem.getCreatedByClubProfileId())
                    .assignedClubProfileId(todoItem.getAssignedClubProfileId() == null
                            ? application.getClubProfileId()
                            : todoItem.getAssignedClubProfileId())
                    .assignedByClubProfileId(access.clubProfile().getClubProfileId())
                    .todoType(todoItem.getTodoType())
                    .assignmentMode(todoItem.getAssignmentMode())
                    .statusCode(STATUS_IN_PROGRESS)
                    .priorityCode(todoItem.getPriorityCode())
                    .recruitmentCapacity(todoItem.getRecruitmentCapacity())
                    .title(todoItem.getTitle())
                    .description(todoItem.getDescription())
                    .dueAt(todoItem.getDueAt())
                    .workStartAt(todoItem.getWorkStartAt())
                    .workEndAt(todoItem.getWorkEndAt())
                    .linkedScheduleEventId(todoItem.getLinkedScheduleEventId())
                    .linkedDecisionRecordId(todoItem.getLinkedDecisionRecordId())
                    .recurrenceFrequency(todoItem.getRecurrenceFrequency())
                    .recurrenceInterval(todoItem.getRecurrenceInterval())
                    .recurrenceEndDate(todoItem.getRecurrenceEndDate())
                    .recurrenceSourceTodoItemId(todoItem.getRecurrenceSourceTodoItemId())
                    .completedByClubProfileId(null)
                    .completedAt(null)
                    .build());
            long selectedCount = todoItemAssigneeRepository.countByTodoItemId(todoItemId);
            if (selectedCount >= normalizedRecruitmentCapacity(updatedTodo)) {
                automaticallyRejectedApplications = rejectOtherPendingApplications(
                        todoItemId,
                        todoItemApplicationId,
                        access.clubProfile().getClubProfileId(),
                        "모집 인원이 모두 선정되었습니다."
                );
            }
        }

        application.review(nextStatus, reviewNote, access.clubProfile().getClubProfileId(), reviewedAt);
        TodoItemApplication saved = todoItemApplicationRepository.save(application);
        notifyTodoApplicationReview(clubId, todoItem, saved, nextStatus, reviewNote);
        automaticallyRejectedApplications.forEach(rejectedApplication -> notifyTodoApplicationReview(
                clubId,
                todoItem,
                rejectedApplication,
                APPLICATION_STATUS_REJECTED,
                rejectedApplication.getReviewNote()
        ));

        ClubActivityContextHolder.setDetails(
                "'" + todoItem.getTitle() + "' 업무 신청을 처리했습니다.",
                "'" + todoItem.getTitle() + "' 업무 신청 처리에 실패했습니다."
        );
        Map<Long, ClubProfile> profileById = clubTodoViewSupport.resolveClubProfiles(
                List.of(List.of(todoItem)),
                List.of(List.of(saved))
        );
        return clubTodoViewSupport.toApplicationResponse(
                saved,
                profileById,
                access.clubProfile().getClubProfileId(),
                true
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

        String title = clubTodoCommandSupport.normalizeTitle(request.title());
        String description = clubTodoCommandSupport.trimToNull(request.description());
        String todoType = clubTodoCommandSupport.normalizeTodoType(request.todoType());
        String assignmentMode = clubTodoCommandSupport.normalizeAssignmentMode(request.assignmentMode());
        LocalDateTime dueAt = clubTodoCommandSupport.parseDateTime(
                request.dueAt(),
                "마감일 형식이 잘못되었습니다."
        );
        String priorityCode = clubTodoCommandSupport.normalizePriorityCode(request.priorityCode());
        LocalDateTime workStartAt = clubTodoCommandSupport.parseDateTime(
                request.workStartAt(),
                "업무 시작 시간 형식이 잘못되었습니다."
        );
        LocalDateTime workEndAt = clubTodoCommandSupport.parseDateTime(
                request.workEndAt(),
                "업무 종료 시간 형식이 잘못되었습니다."
        );
        clubTodoCommandSupport.validateWorkWindow(workStartAt, workEndAt);
        String recurrenceFrequency = clubTodoCommandSupport.normalizeRecurrenceFrequency(
                request.recurrenceFrequency()
        );
        int recurrenceInterval = clubTodoCommandSupport.normalizeRecurrenceInterval(
                recurrenceFrequency,
                request.recurrenceInterval()
        );
        LocalDate recurrenceEndDate = clubTodoCommandSupport.parseDate(
                request.recurrenceEndDate(),
                "반복 종료일 형식이 잘못되었습니다."
        );
        validateRecurrence(
                recurrenceFrequency,
                recurrenceEndDate,
                dueAt,
                workStartAt
        );
        validateLinkedScheduleEvent(clubId, request.linkedScheduleEventId());
        validateLinkedDecisionRecord(clubId, request.linkedDecisionRecordId());
        Map<Long, ClubAccessResolver.ClubMemberSnapshot> activeMemberByProfileId = resolveActiveMembersByProfileId(clubId);
        List<Long> assignedClubProfileIds = resolveAssignedClubProfileIds(
                assignmentMode,
                request.assignedClubProfileId(),
                request.assignedClubProfileIds(),
                activeMemberByProfileId
        );
        if (!assignedClubProfileIds.isEmpty() && !canAssign) {
            throw new SemoException.ForbiddenException("담당자를 배정할 권한이 없습니다.");
        }
        int recruitmentCapacity = ASSIGNMENT_MODE_DIRECT_ASSIGN.equals(assignmentMode)
                ? Math.max(1, assignedClubProfileIds.size())
                : clubTodoCommandSupport.normalizeRecruitmentCapacity(request.recruitmentCapacity());
        Long primaryAssigneeId = assignedClubProfileIds.isEmpty() ? null : assignedClubProfileIds.getFirst();

        TodoItem saved = todoItemRepository.save(TodoItem.builder()
                .clubId(clubId)
                .createdByClubProfileId(access.clubProfile().getClubProfileId())
                .assignedClubProfileId(primaryAssigneeId)
                .assignedByClubProfileId(primaryAssigneeId == null ? null : access.clubProfile().getClubProfileId())
                .todoType(todoType)
                .assignmentMode(assignmentMode)
                .statusCode(STATUS_OPEN)
                .priorityCode(priorityCode)
                .recruitmentCapacity(recruitmentCapacity)
                .title(title)
                .description(description)
                .dueAt(dueAt)
                .workStartAt(workStartAt)
                .workEndAt(workEndAt)
                .linkedScheduleEventId(request.linkedScheduleEventId())
                .linkedDecisionRecordId(request.linkedDecisionRecordId())
                .recurrenceFrequency(recurrenceFrequency)
                .recurrenceInterval(recurrenceInterval)
                .recurrenceEndDate(recurrenceEndDate)
                .recurrenceSourceTodoItemId(null)
                .completedByClubProfileId(null)
                .completedAt(null)
                .build());
        List<TodoItemAssignee> assignees = replaceAssignees(
                saved.getTodoItemId(),
                assignedClubProfileIds,
                access.clubProfile().getClubProfileId()
        );

        ClubActivityContextHolder.setDetails(
                "'" + title + "' 할 일을 등록했습니다.",
                "'" + title + "' 할 일을 등록하지 못했습니다."
        );
        Map<Long, ClubProfile> profileById = clubTodoViewSupport.resolveClubProfiles(
                List.of(List.of(saved)),
                List.of(),
                List.of(assignees)
        );
        return clubTodoViewSupport.toSummaryResponse(
                saved,
                profileById,
                access,
                true,
                Map.of(),
                Map.of(),
                resolveAssigneeIdsByTodoItemId(List.of(List.of(saved)), assignees),
                resolveScheduleById(List.of(List.of(saved))),
                resolveDecisionById(List.of(List.of(saved)))
        );
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
        if (STATUS_COMPLETED.equals(current.getStatusCode()) || STATUS_CANCELED.equals(current.getStatusCode())) {
            throw new SemoException.ValidationException("완료되거나 취소된 업무는 다시 열어야 수정할 수 있습니다.");
        }

        String title = clubTodoCommandSupport.normalizeTitle(request.title());
        String description = clubTodoCommandSupport.trimToNull(request.description());
        String todoType = clubTodoCommandSupport.normalizeTodoType(request.todoType());
        String assignmentMode = clubTodoCommandSupport.normalizeAssignmentMode(request.assignmentMode());
        LocalDateTime dueAt = clubTodoCommandSupport.parseDateTime(
                request.dueAt(),
                "마감일 형식이 잘못되었습니다."
        );
        String priorityCode = clubTodoCommandSupport.normalizePriorityCode(request.priorityCode());
        LocalDateTime workStartAt = clubTodoCommandSupport.parseDateTime(
                request.workStartAt(),
                "업무 시작 시간 형식이 잘못되었습니다."
        );
        LocalDateTime workEndAt = clubTodoCommandSupport.parseDateTime(
                request.workEndAt(),
                "업무 종료 시간 형식이 잘못되었습니다."
        );
        clubTodoCommandSupport.validateWorkWindow(workStartAt, workEndAt);
        String recurrenceFrequency = clubTodoCommandSupport.normalizeRecurrenceFrequency(
                request.recurrenceFrequency()
        );
        int recurrenceInterval = clubTodoCommandSupport.normalizeRecurrenceInterval(
                recurrenceFrequency,
                request.recurrenceInterval()
        );
        LocalDate recurrenceEndDate = clubTodoCommandSupport.parseDate(
                request.recurrenceEndDate(),
                "반복 종료일 형식이 잘못되었습니다."
        );
        validateRecurrence(
                recurrenceFrequency,
                recurrenceEndDate,
                dueAt,
                workStartAt
        );
        validateLinkedScheduleEvent(clubId, request.linkedScheduleEventId());
        validateLinkedDecisionRecord(clubId, request.linkedDecisionRecordId());
        Map<Long, ClubAccessResolver.ClubMemberSnapshot> activeMemberByProfileId = resolveActiveMembersByProfileId(clubId);
        List<TodoItemAssignee> currentAssignees = todoItemAssigneeRepository
                .findByTodoItemIdOrderByTodoItemAssigneeIdAsc(todoItemId);
        List<Long> currentAssigneeIds = resolveAssigneeIds(current, currentAssignees);
        List<Long> assignedClubProfileIds = resolveUpdatedAssignedClubProfileIds(
                current,
                assignmentMode,
                request.assignedClubProfileId(),
                request.assignedClubProfileIds(),
                currentAssigneeIds,
                activeMemberByProfileId
        );
        int recruitmentCapacity = ASSIGNMENT_MODE_DIRECT_ASSIGN.equals(assignmentMode)
                ? Math.max(1, assignedClubProfileIds.size())
                : clubTodoCommandSupport.normalizeRecruitmentCapacity(request.recruitmentCapacity());
        if (recruitmentCapacity < assignedClubProfileIds.size()) {
            throw new SemoException.ValidationException("모집 인원은 현재 선정 인원보다 적게 줄일 수 없습니다.");
        }
        Long primaryAssigneeId = assignedClubProfileIds.isEmpty() ? null : assignedClubProfileIds.getFirst();
        boolean metadataChanged = !Objects.equals(current.getTitle(), title)
                || !Objects.equals(current.getDescription(), description)
                || !Objects.equals(current.getTodoType(), todoType)
                || !Objects.equals(current.getDueAt(), dueAt)
                || !Objects.equals(current.getPriorityCode(), priorityCode)
                || !Objects.equals(current.getWorkStartAt(), workStartAt)
                || !Objects.equals(current.getWorkEndAt(), workEndAt)
                || !Objects.equals(current.getLinkedScheduleEventId(), request.linkedScheduleEventId())
                || !Objects.equals(current.getLinkedDecisionRecordId(), request.linkedDecisionRecordId())
                || !Objects.equals(normalizedRecurrenceFrequency(current), recurrenceFrequency)
                || normalizedRecurrenceInterval(current) != recurrenceInterval
                || !Objects.equals(current.getRecurrenceEndDate(), recurrenceEndDate);
        boolean assignmentChanged = !Objects.equals(current.getAssignmentMode(), assignmentMode)
                || !Objects.equals(currentAssigneeIds, assignedClubProfileIds)
                || normalizedRecruitmentCapacity(current) != recruitmentCapacity;
        if (metadataChanged && !canCreate) {
            throw new SemoException.ForbiddenException("업무 기본 정보를 수정할 권한이 없습니다.");
        }
        if (assignmentChanged && !canAssign) {
            throw new SemoException.ForbiddenException("담당자를 조정할 권한이 없습니다.");
        }
        String nextStatus = current.getStatusCode();
        if (ASSIGNMENT_MODE_OPEN_SUPPORT.equals(assignmentMode) && assignedClubProfileIds.isEmpty()) {
            nextStatus = STATUS_OPEN;
        }

        Long assignedByClubProfileId = primaryAssigneeId == null
                ? null
                : assignmentChanged
                ? access.clubProfile().getClubProfileId()
                : current.getAssignedByClubProfileId();

        TodoItem updated = todoItemRepository.save(TodoItem.builder()
                .todoItemId(current.getTodoItemId())
                .clubId(current.getClubId())
                .createdByClubProfileId(current.getCreatedByClubProfileId())
                .assignedClubProfileId(primaryAssigneeId)
                .assignedByClubProfileId(assignedByClubProfileId)
                .todoType(todoType)
                .assignmentMode(assignmentMode)
                .statusCode(nextStatus)
                .priorityCode(priorityCode)
                .recruitmentCapacity(recruitmentCapacity)
                .title(title)
                .description(description)
                .dueAt(dueAt)
                .workStartAt(workStartAt)
                .workEndAt(workEndAt)
                .linkedScheduleEventId(request.linkedScheduleEventId())
                .linkedDecisionRecordId(request.linkedDecisionRecordId())
                .recurrenceFrequency(recurrenceFrequency)
                .recurrenceInterval(recurrenceInterval)
                .recurrenceEndDate(recurrenceEndDate)
                .recurrenceSourceTodoItemId(current.getRecurrenceSourceTodoItemId())
                .completedByClubProfileId(null)
                .completedAt(null)
                .build());
        List<TodoItemAssignee> updatedAssignees = assignmentChanged
                ? replaceAssignees(
                        updated.getTodoItemId(),
                        assignedClubProfileIds,
                        access.clubProfile().getClubProfileId()
                )
                : currentAssignees;

        if (ASSIGNMENT_MODE_OPEN_SUPPORT.equals(current.getAssignmentMode())
                && ASSIGNMENT_MODE_DIRECT_ASSIGN.equals(assignmentMode)) {
            rejectActiveApplications(
                    current.getTodoItemId(),
                    access.clubProfile().getClubProfileId(),
                    "직접 배정으로 전환되었습니다."
            );
        }

        ClubActivityContextHolder.setDetails(
                "'" + title + "' 할 일을 수정했습니다.",
                "'" + title + "' 할 일을 수정하지 못했습니다."
        );
        Map<Long, ClubProfile> profileById = clubTodoViewSupport.resolveClubProfiles(
                List.of(List.of(updated)),
                List.of(),
                List.of(updatedAssignees)
        );
        Map<Long, Integer> applicationCountByTodoItemId = clubTodoViewSupport.resolveApplicationCountByTodoItemId(
                todoItemApplicationRepository.findByTodoItemIdIn(List.of(updated.getTodoItemId()))
        );
        return clubTodoViewSupport.toSummaryResponse(
                updated,
                profileById,
                access,
                true,
                applicationCountByTodoItemId,
                Map.of(),
                resolveAssigneeIdsByTodoItemId(List.of(List.of(updated)), updatedAssignees),
                resolveScheduleById(List.of(List.of(updated))),
                resolveDecisionById(List.of(List.of(updated)))
        );
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

        TodoItem current = requireTodoItemForUpdate(clubId, todoItemId);
        String nextStatus = clubTodoCommandSupport.normalizeStatusCode(request.statusCode());
        clubTodoCommandSupport.validateStatusTransition(current, nextStatus);

        if (STATUS_REOPEN.equals(nextStatus)
                && todoItemRepository.findByRecurrenceSourceTodoItemId(current.getTodoItemId()).isPresent()) {
            throw new SemoException.ValidationException("이미 다음 반복 업무가 생성되어 이전 회차를 다시 열 수 없습니다.");
        }

        TodoItem updated;
        if (STATUS_REOPEN.equals(nextStatus)) {
            updated = saveAsOpen(current);
            if (ASSIGNMENT_MODE_OPEN_SUPPORT.equals(current.getAssignmentMode())) {
                rejectSelectedApplications(
                        current.getTodoItemId(),
                        access.clubProfile().getClubProfileId(),
                        "업무가 다시 모집 상태로 돌아갔습니다."
                );
            }
            ClubActivityContextHolder.setDetails(
                    "'" + current.getTitle() + "' 업무를 다시 열었습니다.",
                    "'" + current.getTitle() + "' 업무를 다시 열지 못했습니다."
            );
        } else if (STATUS_OPEN.equals(nextStatus)) {
            updated = saveAsOpen(current);
            if (ASSIGNMENT_MODE_OPEN_SUPPORT.equals(current.getAssignmentMode())) {
                rejectSelectedApplications(
                        current.getTodoItemId(),
                        access.clubProfile().getClubProfileId(),
                        "업무가 다시 모집 상태로 돌아갔습니다."
                );
            }
            ClubActivityContextHolder.setDetails(
                    "'" + current.getTitle() + "' 업무를 열림 상태로 되돌렸습니다.",
                    "'" + current.getTitle() + "' 업무를 열림 상태로 되돌리지 못했습니다."
            );
        } else {
            updated = saveWithStatus(current, nextStatus, access.clubProfile().getClubProfileId());
            if (STATUS_COMPLETED.equals(nextStatus)) {
                createNextRecurringTodo(updated, access.clubProfile().getClubProfileId());
            }
            ClubActivityContextHolder.setDetails(
                    "'" + current.getTitle() + "' 상태를 "
                            + clubTodoViewSupport.toStatusLabel(nextStatus)
                            + "(으)로 변경했습니다.",
                    "'" + current.getTitle() + "' 상태를 변경하지 못했습니다."
            );
        }
        Map<Long, ClubProfile> profileById = clubTodoViewSupport.resolveClubProfiles(
                List.of(List.of(updated)),
                List.of()
        );
        return clubTodoViewSupport.toActionResponse(updated, profileById);
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "할 일관리")
    public void deleteTodo(Long clubId, Long todoItemId, String userKey) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        requireTodoFeature(clubId);
        if (!clubTodoPermissionService.canDeleteTodo(access)) {
            throw new SemoException.ForbiddenException("할 일을 삭제할 권한이 없습니다.");
        }

        TodoItem current = requireTodoItem(clubId, todoItemId);
        saveWithStatus(current, STATUS_CANCELED, access.clubProfile().getClubProfileId());
        rejectActiveApplications(
                current.getTodoItemId(),
                access.clubProfile().getClubProfileId(),
                "업무가 보관되었습니다."
        );
        ClubActivityContextHolder.setDetails(
                "'" + current.getTitle() + "' 할 일을 보관했습니다.",
                "'" + current.getTitle() + "' 할 일 보관에 실패했습니다."
        );
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

    private TodoItem requireTodoItemForUpdate(Long clubId, Long todoItemId) {
        return todoItemRepository.findForUpdate(todoItemId, clubId)
                .orElseThrow(() -> new SemoException.ResourceNotFoundException("TodoItem", "todoItemId", todoItemId));
    }

    private TodoItemApplication requireTodoItemApplication(Long todoItemId, Long todoItemApplicationId) {
        return todoItemApplicationRepository.findById(todoItemApplicationId)
                .filter(application -> Objects.equals(application.getTodoItemId(), todoItemId))
                .orElseThrow(() -> new SemoException.ResourceNotFoundException(
                        "TodoItemApplication",
                        "todoItemApplicationId",
                        todoItemApplicationId
                ));
    }

    private TodoItemApplication requireTodoItemApplicationForUpdate(Long todoItemId, Long todoItemApplicationId) {
        return todoItemApplicationRepository.findForUpdate(todoItemId, todoItemApplicationId)
                .orElseThrow(() -> new SemoException.ResourceNotFoundException(
                        "TodoItemApplication",
                        "todoItemApplicationId",
                        todoItemApplicationId
                ));
    }

    private void validateOpenSupportApplicationAvailable(TodoItem current) {
        if (!ASSIGNMENT_MODE_OPEN_SUPPORT.equals(current.getAssignmentMode())) {
            throw new SemoException.ValidationException("신청 가능한 업무만 지원할 수 있습니다.");
        }
        if (!isRecruitmentAvailable(current)) {
            throw new SemoException.ValidationException("현재 신청할 수 없는 상태의 업무입니다.");
        }
    }

    private List<TodoItemApplication> rejectOtherPendingApplications(
            Long todoItemId,
            Long selectedApplicationId,
            Long actorClubProfileId,
            String reviewNote
    ) {
        List<TodoItemApplication> pendingApplications = todoItemApplicationRepository
                .findByTodoItemIdAndApplicationStatusOrderByCreateDateAscTodoItemApplicationIdAsc(
                        todoItemId,
                        APPLICATION_STATUS_APPLIED
                );
        List<TodoItemApplication> rejectedApplications = pendingApplications.stream()
                .filter(application -> !Objects.equals(application.getTodoItemApplicationId(), selectedApplicationId))
                .toList();
        LocalDateTime reviewedAt = LocalDateTime.now();
        rejectedApplications.forEach(application -> application.review(
                APPLICATION_STATUS_REJECTED,
                reviewNote,
                actorClubProfileId,
                reviewedAt
        ));
        if (!rejectedApplications.isEmpty()) {
            todoItemApplicationRepository.saveAll(rejectedApplications);
        }
        return rejectedApplications;
    }

    private void notifyTodoApplicationReview(
            Long clubId,
            TodoItem todoItem,
            TodoItemApplication application,
            String status,
            String reviewNote
    ) {
        boolean selected = APPLICATION_STATUS_SELECTED.equals(status);
        String resultLabel = selected ? "선정" : "미선정";
        String message = "'" + todoItem.getTitle() + "' 업무 신청 결과: " + resultLabel;
        if (reviewNote != null && !reviewNote.isBlank()) {
            message += " · " + reviewNote;
        }
        clubNotificationPublisher.notifyClubProfile(
                application.getClubProfileId(),
                new NotificationCommand(
                        clubId,
                        "TODO_APPLICATION_REVIEW",
                        "업무 신청 결과가 도착했습니다",
                        message,
                        "TODO_APPLICATION",
                        application.getTodoItemApplicationId(),
                        "/clubs/" + clubId + "/more/todos",
                        "todo-application:" + application.getTodoItemApplicationId() + ":" + status
                )
        );
    }

    private void rejectActiveApplications(Long todoItemId, Long actorClubProfileId, String reviewNote) {
        List<TodoItemApplication> applications = todoItemApplicationRepository
                .findByTodoItemIdOrderByCreateDateAscTodoItemApplicationIdAsc(todoItemId);
        applications.stream()
                .filter(application -> APPLICATION_STATUS_APPLIED.equals(application.getApplicationStatus())
                        || APPLICATION_STATUS_SELECTED.equals(application.getApplicationStatus()))
                .forEach(application -> application.review(
                        APPLICATION_STATUS_REJECTED,
                        reviewNote,
                        actorClubProfileId,
                        LocalDateTime.now()
                ));
        if (!applications.isEmpty()) {
            todoItemApplicationRepository.saveAll(applications);
        }
    }

    private void rejectSelectedApplications(Long todoItemId, Long actorClubProfileId, String reviewNote) {
        List<TodoItemApplication> applications = todoItemApplicationRepository
                .findByTodoItemIdOrderByCreateDateAscTodoItemApplicationIdAsc(todoItemId);
        applications.stream()
                .filter(application -> APPLICATION_STATUS_SELECTED.equals(application.getApplicationStatus()))
                .forEach(application -> application.review(
                        APPLICATION_STATUS_REJECTED,
                        reviewNote,
                        actorClubProfileId,
                        LocalDateTime.now()
                ));
        if (!applications.isEmpty()) {
            todoItemApplicationRepository.saveAll(applications);
        }
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

    private List<Long> resolveAssignedClubProfileIds(
            String assignmentMode,
            Long requestedAssignedClubProfileId,
            List<Long> requestedAssignedClubProfileIds,
            Map<Long, ClubAccessResolver.ClubMemberSnapshot> activeMemberByProfileId
    ) {
        List<Long> requestedIds = normalizeRequestedAssigneeIds(
                requestedAssignedClubProfileId,
                requestedAssignedClubProfileIds
        );
        if (ASSIGNMENT_MODE_DIRECT_ASSIGN.equals(assignmentMode)) {
            if (requestedIds.isEmpty()) {
                throw new SemoException.ValidationException("직접 배정 업무는 담당자를 선택해야 합니다.");
            }
            validateActiveAssignees(requestedIds, activeMemberByProfileId);
            return requestedIds;
        }

        if (!requestedIds.isEmpty()) {
            throw new SemoException.ValidationException("신청형 업무는 담당자를 비워둬야 합니다.");
        }
        return List.of();
    }

    private List<Long> resolveUpdatedAssignedClubProfileIds(
            TodoItem current,
            String assignmentMode,
            Long requestedAssignedClubProfileId,
            List<Long> requestedAssignedClubProfileIds,
            List<Long> currentAssigneeIds,
            Map<Long, ClubAccessResolver.ClubMemberSnapshot> activeMemberByProfileId
    ) {
        if (ASSIGNMENT_MODE_OPEN_SUPPORT.equals(assignmentMode)) {
            List<Long> requestedIds = normalizeRequestedAssigneeIds(
                    requestedAssignedClubProfileId,
                    requestedAssignedClubProfileIds
            );
            if (!requestedIds.isEmpty()) {
                throw new SemoException.ValidationException("신청형 업무의 담당자는 신청 검토에서 선정해야 합니다.");
            }
            return ASSIGNMENT_MODE_OPEN_SUPPORT.equals(current.getAssignmentMode())
                    ? currentAssigneeIds
                    : List.of();
        }
        List<Long> requestedIds = normalizeRequestedAssigneeIds(
                requestedAssignedClubProfileId,
                requestedAssignedClubProfileIds
        );
        if (ASSIGNMENT_MODE_DIRECT_ASSIGN.equals(current.getAssignmentMode())
                && Objects.equals(currentAssigneeIds, requestedIds)) {
            return currentAssigneeIds;
        }
        return resolveAssignedClubProfileIds(
                assignmentMode,
                requestedAssignedClubProfileId,
                requestedAssignedClubProfileIds,
                activeMemberByProfileId
        );
    }

    private List<Long> normalizeRequestedAssigneeIds(
            Long requestedAssignedClubProfileId,
            List<Long> requestedAssignedClubProfileIds
    ) {
        Stream<Long> requestedStream = requestedAssignedClubProfileIds == null
                ? Stream.empty()
                : requestedAssignedClubProfileIds.stream();
        return Stream.concat(
                        requestedStream,
                        requestedAssignedClubProfileId == null
                                ? Stream.empty()
                                : Stream.of(requestedAssignedClubProfileId)
                )
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private void validateActiveAssignees(
            List<Long> assignedClubProfileIds,
            Map<Long, ClubAccessResolver.ClubMemberSnapshot> activeMemberByProfileId
    ) {
        if (assignedClubProfileIds.size() > 100) {
            throw new SemoException.ValidationException("업무 담당자는 최대 100명까지 지정할 수 있습니다.");
        }
        if (assignedClubProfileIds.stream().anyMatch(id -> !activeMemberByProfileId.containsKey(id))) {
            throw new SemoException.ValidationException("현재 활성 멤버에게만 업무를 배정할 수 있습니다.");
        }
    }

    private List<TodoItemAssignee> replaceAssignees(
            Long todoItemId,
            List<Long> assignedClubProfileIds,
            Long actorClubProfileId
    ) {
        todoItemAssigneeRepository.deleteByTodoItemId(todoItemId);
        todoItemAssigneeRepository.flush();
        if (assignedClubProfileIds.isEmpty()) {
            return List.of();
        }
        return todoItemAssigneeRepository.saveAll(assignedClubProfileIds.stream()
                .map(profileId -> TodoItemAssignee.builder()
                        .todoItemId(todoItemId)
                        .clubProfileId(profileId)
                        .assignedByClubProfileId(actorClubProfileId)
                        .build())
                .toList());
    }

    private boolean isAssignee(TodoItem todoItem, Long clubProfileId) {
        return Objects.equals(todoItem.getAssignedClubProfileId(), clubProfileId)
                || todoItemAssigneeRepository.existsByTodoItemIdAndClubProfileId(
                        todoItem.getTodoItemId(),
                        clubProfileId
                );
    }

    private boolean isRecruitmentAvailable(TodoItem todoItem) {
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

    private int normalizedRecruitmentCapacity(TodoItem todoItem) {
        return todoItem.getRecruitmentCapacity() == null
                ? 1
                : Math.max(1, todoItem.getRecruitmentCapacity());
    }

    private List<Long> resolveAssigneeIds(
            TodoItem todoItem,
            List<TodoItemAssignee> assignees
    ) {
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

    private Map<Long, List<Long>> resolveAssigneeIdsByTodoItemId(
            List<List<TodoItem>> itemGroups,
            List<TodoItemAssignee> assignees
    ) {
        Map<Long, List<Long>> assigneeIdsByTodoItemId = assignees.stream()
                .collect(Collectors.groupingBy(
                        TodoItemAssignee::getTodoItemId,
                        LinkedHashMap::new,
                        Collectors.mapping(TodoItemAssignee::getClubProfileId, Collectors.toList())
                ));
        itemGroups.stream()
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .filter(item -> item.getAssignedClubProfileId() != null)
                .forEach(item -> assigneeIdsByTodoItemId.putIfAbsent(
                        item.getTodoItemId(),
                        List.of(item.getAssignedClubProfileId())
                ));
        return assigneeIdsByTodoItemId;
    }

    private Map<Long, ClubScheduleEvent> resolveScheduleById(List<List<TodoItem>> itemGroups) {
        List<Long> eventIds = itemGroups.stream()
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .map(TodoItem::getLinkedScheduleEventId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (eventIds.isEmpty()) {
            return Map.of();
        }
        return clubScheduleEventRepository.findAllByEventIdIn(eventIds).stream()
                .collect(Collectors.toMap(ClubScheduleEvent::getEventId, Function.identity()));
    }

    private Map<Long, DecisionRecord> resolveDecisionById(List<List<TodoItem>> itemGroups) {
        List<Long> decisionRecordIds = itemGroups.stream()
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .map(TodoItem::getLinkedDecisionRecordId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (decisionRecordIds.isEmpty()) {
            return Map.of();
        }
        return decisionRecordRepository.findAllById(decisionRecordIds).stream()
                .filter(decision -> !decision.isDeleted())
                .collect(Collectors.toMap(DecisionRecord::getDecisionRecordId, Function.identity()));
    }

    private void validateLinkedScheduleEvent(Long clubId, Long eventId) {
        if (eventId == null) {
            return;
        }
        ClubScheduleEvent event = clubScheduleEventRepository.findByEventIdAndClubId(eventId, clubId)
                .orElseThrow(() -> new SemoException.ValidationException("같은 클럽의 일정만 업무에 연결할 수 있습니다."));
        if ("CANCELLED".equals(event.getEventStatus())) {
            throw new SemoException.ValidationException("취소된 일정은 업무에 연결할 수 없습니다.");
        }
    }

    private void validateLinkedDecisionRecord(Long clubId, Long decisionRecordId) {
        if (decisionRecordId == null) {
            return;
        }
        DecisionRecord decision = decisionRecordRepository
                .findByDecisionRecordIdAndClubIdAndDeletedFalse(decisionRecordId, clubId)
                .orElseThrow(() -> new SemoException.ValidationException(
                        "같은 클럽의 공개 확정 결정만 업무에 연결할 수 있습니다."
                ));
        if (!"MEMBERS".equals(decision.getVisibilityScope())
                || (!"CONFIRMED".equals(decision.getStatusCode())
                && !"SUPERSEDED".equals(decision.getStatusCode()))) {
            throw new SemoException.ValidationException("멤버 공개 상태로 확정된 결정만 업무에 연결할 수 있습니다.");
        }
    }

    private void validateRecurrence(
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

    private List<TodoScheduleOptionResponse> buildScheduleOptions(Long clubId) {
        LocalDateTime lowerBound = LocalDateTime.now().minusDays(30);
        return clubScheduleEventRepository.findAllActiveEvents(clubId).stream()
                .filter(event -> event.getEndAt() == null
                        ? !event.getStartAt().isBefore(lowerBound)
                        : !event.getEndAt().isBefore(lowerBound))
                .limit(40)
                .map(event -> new TodoScheduleOptionResponse(
                        event.getEventId(),
                        event.getTitle(),
                        event.getStartAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                        event.getStartAt().format(SCHEDULE_LABEL_FORMATTER)
                ))
                .toList();
    }

    private List<TodoDecisionOptionResponse> buildDecisionOptions(Long clubId) {
        return decisionRecordRepository.findTodoLinkOptions(clubId, PageRequest.of(0, 40)).stream()
                .map(decision -> new TodoDecisionOptionResponse(
                        decision.getDecisionRecordId(),
                        decision.getTitle(),
                        decision.getStatusCode(),
                        decision.getConfirmedAt() == null
                                ? null
                                : decision.getConfirmedAt().format(SCHEDULE_LABEL_FORMATTER)
                ))
                .toList();
    }

    private TodoItem createNextRecurringTodo(TodoItem completed, Long actorClubProfileId) {
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
        LocalDateTime nextDueAt = shiftRecurringDateTime(
                completed.getDueAt(),
                recurrenceFrequency,
                recurrenceInterval
        );
        LocalDateTime nextWorkStartAt = shiftRecurringDateTime(
                completed.getWorkStartAt(),
                recurrenceFrequency,
                recurrenceInterval
        );
        LocalDateTime nextWorkEndAt = shiftRecurringDateTime(
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
        cloneChecklistForRecurringTodo(completed.getTodoItemId(), next.getTodoItemId());
        notifyRecurringTodoAssignees(next, nextAssigneeIds);
        return next;
    }

    private void cloneChecklistForRecurringTodo(Long sourceTodoItemId, Long nextTodoItemId) {
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

    private void notifyRecurringTodoAssignees(TodoItem todoItem, List<Long> assigneeIds) {
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

    private LocalDateTime shiftRecurringDateTime(
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
                .priorityCode(current.getPriorityCode())
                .recruitmentCapacity(current.getRecruitmentCapacity())
                .title(current.getTitle())
                .description(current.getDescription())
                .dueAt(current.getDueAt())
                .workStartAt(current.getWorkStartAt())
                .workEndAt(current.getWorkEndAt())
                .linkedScheduleEventId(current.getLinkedScheduleEventId())
                .linkedDecisionRecordId(current.getLinkedDecisionRecordId())
                .recurrenceFrequency(current.getRecurrenceFrequency())
                .recurrenceInterval(current.getRecurrenceInterval())
                .recurrenceEndDate(current.getRecurrenceEndDate())
                .recurrenceSourceTodoItemId(current.getRecurrenceSourceTodoItemId())
                .completedByClubProfileId(completedByClubProfileId)
                .completedAt(completedAt)
                .build());
    }

    private TodoItem saveAsOpen(TodoItem current) {
        boolean keepAssignment = ASSIGNMENT_MODE_DIRECT_ASSIGN.equals(current.getAssignmentMode());
        if (!keepAssignment) {
            todoItemAssigneeRepository.deleteByTodoItemId(current.getTodoItemId());
        }
        return todoItemRepository.save(TodoItem.builder()
                .todoItemId(current.getTodoItemId())
                .clubId(current.getClubId())
                .createdByClubProfileId(current.getCreatedByClubProfileId())
                .assignedClubProfileId(keepAssignment ? current.getAssignedClubProfileId() : null)
                .assignedByClubProfileId(keepAssignment ? current.getAssignedByClubProfileId() : null)
                .todoType(current.getTodoType())
                .assignmentMode(current.getAssignmentMode())
                .statusCode(STATUS_OPEN)
                .priorityCode(current.getPriorityCode())
                .recruitmentCapacity(current.getRecruitmentCapacity())
                .title(current.getTitle())
                .description(current.getDescription())
                .dueAt(current.getDueAt())
                .workStartAt(current.getWorkStartAt())
                .workEndAt(current.getWorkEndAt())
                .linkedScheduleEventId(current.getLinkedScheduleEventId())
                .linkedDecisionRecordId(current.getLinkedDecisionRecordId())
                .recurrenceFrequency(current.getRecurrenceFrequency())
                .recurrenceInterval(current.getRecurrenceInterval())
                .recurrenceEndDate(current.getRecurrenceEndDate())
                .recurrenceSourceTodoItemId(current.getRecurrenceSourceTodoItemId())
                .completedByClubProfileId(null)
                .completedAt(null)
                .build());
    }

}
