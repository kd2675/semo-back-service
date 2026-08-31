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
import semo.back.service.database.pub.entity.TodoItem;
import semo.back.service.database.pub.entity.TodoItemApplication;
import semo.back.service.database.pub.entity.TodoItemAssignee;
import semo.back.service.database.pub.repository.ClubScheduleEventRepository;
import semo.back.service.database.pub.repository.ClubProfileRepository;
import semo.back.service.database.pub.repository.DecisionRecordRepository;
import semo.back.service.database.pub.repository.TodoItemApplicationRepository;
import semo.back.service.database.pub.repository.TodoItemAssigneeRepository;
import semo.back.service.database.pub.repository.TodoItemRepository;
import semo.back.service.feature.activity.biz.ClubActivityContextHolder;
import semo.back.service.feature.activity.biz.RecordClubActivity;
import semo.back.service.feature.club.biz.policy.ClubAccessResolver;
import semo.back.service.feature.clubfeature.biz.ClubFeatureService;
import semo.back.service.feature.todo.biz.policy.ClubTodoPermissionService;
import semo.back.service.feature.todo.biz.policy.ClubTodoAssignmentPolicy;
import semo.back.service.feature.todo.biz.support.ClubTodoCommandSupport;
import semo.back.service.feature.todo.biz.support.ClubTodoRecurrenceService;
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
    private static final int CLAIMABLE_PAGE_SIZE = 8;
    private static final int MAX_CLAIMABLE_PAGE_SIZE = 50;
    private static final DateTimeFormatter SCHEDULE_LABEL_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");

    private final ClubAccessResolver clubAccessResolver;
    private final ClubFeatureService clubFeatureService;
    private final ClubTodoPermissionService clubTodoPermissionService;
    private final ClubTodoAssignmentPolicy clubTodoAssignmentPolicy;
    private final ClubTodoApplicationService clubTodoApplicationService;
    private final TodoItemRepository todoItemRepository;
    private final TodoItemApplicationRepository todoItemApplicationRepository;
    private final TodoItemAssigneeRepository todoItemAssigneeRepository;
    private final ClubProfileRepository clubProfileRepository;
    private final ClubScheduleEventRepository clubScheduleEventRepository;
    private final DecisionRecordRepository decisionRecordRepository;
    private final ClubTodoCommandSupport clubTodoCommandSupport;
    private final ClubTodoRecurrenceService clubTodoRecurrenceService;
    private final ClubTodoViewSupport clubTodoViewSupport;

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
                                .filter(clubTodoAssignmentPolicy::isRecruitmentAvailable),
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
        Map<Long, ClubScheduleEvent> scheduleById = resolveScheduleById(clubId,
                List.of(myItems, prioritizedClaimableItems, recentCompletedItems)
        );
        Map<Long, DecisionRecord> decisionById = resolveDecisionById(clubId,
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
                                    && clubTodoAssignmentPolicy.isRecruitmentAvailable(item);
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
        return clubTodoApplicationService.apply(clubId, todoItemId, userKey, request);
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "할 일")
    public TodoItemApplicationResponse cancelMyTodoApplication(Long clubId, Long todoItemId, String userKey) {
        return clubTodoApplicationService.cancel(clubId, todoItemId, userKey);
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "할 일")
    public TodoActionResponse completeTodo(Long clubId, Long todoItemId, String userKey) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        requireTodoFeature(clubId);
        TodoItem current = requireTodoItemForUpdate(clubId, todoItemId);
        if (!clubTodoAssignmentPolicy.isAssignee(current, access.clubProfile().getClubProfileId())) {
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
        clubTodoRecurrenceService.createNextTodo(updated, access.clubProfile().getClubProfileId());
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
        List<Long> pageTodoIds = pageItems.stream().map(TodoItem::getTodoItemId).toList();
        List<TodoItemApplication> pageApplications = pageTodoIds.isEmpty()
                ? List.of()
                : todoItemApplicationRepository.findByTodoItemIdIn(pageTodoIds);
        List<TodoItemAssignee> pageAssignees = pageTodoIds.isEmpty()
                ? List.of()
                : todoItemAssigneeRepository.findByTodoItemIdInOrderByTodoItemAssigneeIdAsc(pageTodoIds);
        Map<Long, List<Long>> assigneeIdsByTodoItemId = resolveAssigneeIdsByTodoItemId(
                List.of(pageItems),
                pageAssignees
        );
        Map<Long, ClubProfile> profileById = clubTodoViewSupport.resolveClubProfiles(
                List.of(pageItems),
                List.of(pageApplications),
                List.of(pageAssignees)
        );
        Map<Long, ClubScheduleEvent> scheduleById = resolveScheduleById(clubId, List.of(pageItems));
        Map<Long, DecisionRecord> decisionById = resolveDecisionById(clubId, List.of(pageItems));
        Map<Long, Integer> applicationCountByTodoItemId =
                clubTodoViewSupport.resolveApplicationCountByTodoItemId(pageApplications);

        return new ClubAdminTodoResponse(
                access.club().getClubId(),
                access.club().getName(),
                access.isAdmin(),
                clubTodoPermissionService.canCreateTodo(access),
                clubTodoPermissionService.canAssignTodo(access),
                clubTodoPermissionService.canManageStatus(access),
                clubTodoPermissionService.canDeleteTodo(access),
                activeMembers.size(),
                Math.toIntExact(todoItemRepository.countByClubIdAndStatusCode(clubId, STATUS_OPEN)),
                Math.toIntExact(todoItemRepository.countByClubIdAndStatusCode(clubId, STATUS_IN_PROGRESS)),
                Math.toIntExact(todoItemRepository.countByClubIdAndStatusCode(clubId, STATUS_COMPLETED)),
                Math.toIntExact(todoItemApplicationRepository.countPendingApplicationsForClub(clubId)),
                Math.toIntExact(todoItemRepository.countOverdueForAdmin(clubId, now)),
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
        List<Long> assigneeIds = clubTodoAssignmentPolicy.resolveAssigneeIds(todoItem, assigneeEntities);
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
                clubTodoAssignmentPolicy.normalizedRecruitmentCapacity(todoItem),
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
        return clubTodoApplicationService.review(
                clubId,
                todoItemId,
                todoItemApplicationId,
                userKey,
                request
        );
    }

    @Transactional(transactionManager = "pubTransactionManager")
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
        clubTodoRecurrenceService.validateRecurrence(
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
                resolveScheduleById(clubId, List.of(List.of(saved))),
                resolveDecisionById(clubId, List.of(List.of(saved)))
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
        clubTodoRecurrenceService.validateRecurrence(
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
        List<Long> currentAssigneeIds = clubTodoAssignmentPolicy.resolveAssigneeIds(current, currentAssignees);
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
                || clubTodoRecurrenceService.hasConfigurationChanged(
                        current,
                        recurrenceFrequency,
                        recurrenceInterval,
                        recurrenceEndDate
                );
        boolean assignmentChanged = !Objects.equals(current.getAssignmentMode(), assignmentMode)
                || !Objects.equals(currentAssigneeIds, assignedClubProfileIds)
                || clubTodoAssignmentPolicy.normalizedRecruitmentCapacity(current) != recruitmentCapacity;
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
            clubTodoApplicationService.rejectActive(
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
                resolveScheduleById(clubId, List.of(List.of(updated))),
                resolveDecisionById(clubId, List.of(List.of(updated)))
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
                clubTodoApplicationService.rejectSelected(
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
                clubTodoApplicationService.rejectSelected(
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
                clubTodoRecurrenceService.createNextTodo(updated, access.clubProfile().getClubProfileId());
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
        clubTodoApplicationService.rejectActive(
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

    private Map<Long, ClubScheduleEvent> resolveScheduleById(Long clubId, List<List<TodoItem>> itemGroups) {
        if (!clubFeatureService.isFeatureEnabled(clubId, "SCHEDULE_MANAGE")) {
            return Map.of();
        }
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

    private Map<Long, DecisionRecord> resolveDecisionById(Long clubId, List<List<TodoItem>> itemGroups) {
        if (!clubFeatureService.isFeatureEnabled(clubId, "DECISION_LOG")) {
            return Map.of();
        }
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
        if (!clubFeatureService.isFeatureEnabled(clubId, "SCHEDULE_MANAGE")) {
            throw new SemoException.ValidationException("일정 기능이 비활성화되어 업무에 일정을 연결할 수 없습니다.");
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
        if (!clubFeatureService.isFeatureEnabled(clubId, "DECISION_LOG")) {
            throw new SemoException.ValidationException("회의록·결정 기능이 비활성화되어 업무에 결정을 연결할 수 없습니다.");
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

    private List<TodoScheduleOptionResponse> buildScheduleOptions(Long clubId) {
        if (!clubFeatureService.isFeatureEnabled(clubId, "SCHEDULE_MANAGE")) {
            return List.of();
        }
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
        if (!clubFeatureService.isFeatureEnabled(clubId, "DECISION_LOG")) {
            return List.of();
        }
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
