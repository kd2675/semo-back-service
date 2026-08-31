package semo.back.service.feature.todo.biz;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import semo.back.service.common.exception.SemoException;
import semo.back.service.database.pub.entity.ClubProfile;
import semo.back.service.database.pub.entity.TodoItem;
import semo.back.service.database.pub.entity.TodoItemApplication;
import semo.back.service.database.pub.entity.TodoItemAssignee;
import semo.back.service.database.pub.repository.TodoItemApplicationRepository;
import semo.back.service.database.pub.repository.TodoItemAssigneeRepository;
import semo.back.service.database.pub.repository.TodoItemRepository;
import semo.back.service.feature.activity.biz.ClubActivityContextHolder;
import semo.back.service.feature.club.biz.policy.ClubAccessResolver;
import semo.back.service.feature.notification.biz.ClubNotificationPublisher;
import semo.back.service.feature.notification.biz.ClubNotificationPublisher.NotificationCommand;
import semo.back.service.feature.todo.biz.policy.ClubTodoAssignmentPolicy;
import semo.back.service.feature.todo.biz.policy.ClubTodoPermissionService;
import semo.back.service.feature.todo.biz.support.ClubTodoCommandSupport;
import semo.back.service.feature.todo.biz.support.ClubTodoViewSupport;
import semo.back.service.feature.todo.vo.CreateTodoApplicationRequest;
import semo.back.service.feature.todo.vo.ReviewTodoItemApplicationRequest;
import semo.back.service.feature.todo.vo.TodoItemApplicationResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ClubTodoApplicationService {
    private static final String ASSIGNMENT_MODE_OPEN_SUPPORT = "OPEN_SUPPORT";
    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String APPLICATION_STATUS_APPLIED = "APPLIED";
    private static final String APPLICATION_STATUS_SELECTED = "SELECTED";
    private static final String APPLICATION_STATUS_REJECTED = "REJECTED";

    private final ClubAccessResolver clubAccessResolver;
    private final ClubTodoPermissionService clubTodoPermissionService;
    private final ClubTodoAssignmentPolicy clubTodoAssignmentPolicy;
    private final TodoItemRepository todoItemRepository;
    private final TodoItemApplicationRepository todoItemApplicationRepository;
    private final TodoItemAssigneeRepository todoItemAssigneeRepository;
    private final ClubTodoCommandSupport clubTodoCommandSupport;
    private final ClubTodoViewSupport clubTodoViewSupport;
    private final ClubNotificationPublisher clubNotificationPublisher;

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.MANDATORY)
    public TodoItemApplicationResponse apply(
            Long clubId,
            Long todoItemId,
            String userKey,
            CreateTodoApplicationRequest request
    ) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        requireTodoFeature(clubId);
        TodoItem current = requireTodoItem(clubId, todoItemId);
        validateApplicationAvailable(current);

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

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.MANDATORY)
    public TodoItemApplicationResponse cancel(Long clubId, Long todoItemId, String userKey) {
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

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.MANDATORY)
    public TodoItemApplicationResponse review(
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

        TodoItemApplication application = todoItemApplicationRepository
                .findForUpdate(todoItemId, todoItemApplicationId)
                .orElseThrow(() -> new SemoException.ResourceNotFoundException(
                        "TodoItemApplication",
                        "todoItemApplicationId",
                        todoItemApplicationId
                ));
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
            if (!clubTodoAssignmentPolicy.isRecruitmentAvailable(todoItem)) {
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
            if (selectedCount >= clubTodoAssignmentPolicy.normalizedRecruitmentCapacity(updatedTodo)) {
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
        notifyReview(clubId, todoItem, saved, nextStatus, reviewNote);
        automaticallyRejectedApplications.forEach(rejectedApplication -> notifyReview(
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

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.MANDATORY)
    public void rejectActive(Long todoItemId, Long actorClubProfileId, String reviewNote) {
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

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.MANDATORY)
    public void rejectSelected(Long todoItemId, Long actorClubProfileId, String reviewNote) {
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

    private void requireTodoFeature(Long clubId) {
        if (!clubTodoPermissionService.isTodoEnabled(clubId)) {
            throw new SemoException.ForbiddenException("할 일 기능이 활성화되지 않았습니다.");
        }
    }

    private TodoItem requireTodoItem(Long clubId, Long todoItemId) {
        return todoItemRepository.findByTodoItemIdAndClubId(todoItemId, clubId)
                .orElseThrow(() -> new SemoException.ResourceNotFoundException(
                        "TodoItem",
                        "todoItemId",
                        todoItemId
                ));
    }

    private TodoItem requireTodoItemForUpdate(Long clubId, Long todoItemId) {
        return todoItemRepository.findForUpdate(todoItemId, clubId)
                .orElseThrow(() -> new SemoException.ResourceNotFoundException(
                        "TodoItem",
                        "todoItemId",
                        todoItemId
                ));
    }

    private void validateApplicationAvailable(TodoItem current) {
        if (!ASSIGNMENT_MODE_OPEN_SUPPORT.equals(current.getAssignmentMode())) {
            throw new SemoException.ValidationException("신청 가능한 업무만 지원할 수 있습니다.");
        }
        if (!clubTodoAssignmentPolicy.isRecruitmentAvailable(current)) {
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
                .filter(application -> !Objects.equals(
                        application.getTodoItemApplicationId(),
                        selectedApplicationId
                ))
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

    private void notifyReview(
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
}
