package semo.back.service.feature.todo.biz.support;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import semo.back.service.database.pub.entity.ClubProfile;
import semo.back.service.database.pub.entity.ClubScheduleEvent;
import semo.back.service.database.pub.entity.DecisionRecord;
import semo.back.service.database.pub.entity.TodoItem;
import semo.back.service.database.pub.entity.TodoItemApplication;
import semo.back.service.database.pub.entity.TodoItemAssignee;
import semo.back.service.database.pub.repository.ClubProfileRepository;
import semo.back.service.feature.club.biz.policy.ClubAccessResolver;
import semo.back.service.feature.todo.biz.policy.ClubTodoPermissionService;
import semo.back.service.feature.todo.vo.TodoActionResponse;
import semo.back.service.feature.todo.vo.TodoAssigneeResponse;
import semo.back.service.feature.todo.vo.TodoItemApplicationResponse;
import semo.back.service.feature.todo.vo.TodoSummaryResponse;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class ClubTodoViewSupport {
    private static final String TODO_TYPE_VOLUNTEER = "VOLUNTEER";
    private static final String TODO_TYPE_OPERATIONS = "OPERATIONS";
    private static final String ASSIGNMENT_MODE_DIRECT_ASSIGN = "DIRECT_ASSIGN";
    private static final String ASSIGNMENT_MODE_OPEN_SUPPORT = "OPEN_SUPPORT";
    private static final String STATUS_OPEN = "OPEN";
    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_CANCELED = "CANCELED";
    private static final String APPLICATION_STATUS_APPLIED = "APPLIED";
    private static final String APPLICATION_STATUS_SELECTED = "SELECTED";
    private static final String APPLICATION_STATUS_REJECTED = "REJECTED";
    private static final String APPLICATION_STATUS_WITHDRAWN = "WITHDRAWN";

    private static final DateTimeFormatter REQUEST_DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter DATE_TIME_LABEL_FORMATTER = DateTimeFormatter.ofPattern(
            "yyyy.MM.dd HH:mm",
            Locale.KOREAN
    );

    private final ClubProfileRepository clubProfileRepository;
    private final ClubTodoPermissionService clubTodoPermissionService;

    public Map<Long, ClubProfile> resolveClubProfiles(
            List<List<TodoItem>> itemGroups,
            List<List<TodoItemApplication>> applicationGroups
    ) {
        return resolveClubProfiles(itemGroups, applicationGroups, List.of());
    }

    public Map<Long, ClubProfile> resolveClubProfiles(
            List<List<TodoItem>> itemGroups,
            List<List<TodoItemApplication>> applicationGroups,
            List<List<TodoItemAssignee>> assigneeGroups
    ) {
        List<Long> ids = Stream.concat(
                        Stream.concat(
                                itemGroups.stream()
                                        .filter(Objects::nonNull)
                                        .flatMap(Collection::stream)
                                        .flatMap(item -> Stream.of(
                                                item.getCreatedByClubProfileId(),
                                                item.getAssignedClubProfileId(),
                                                item.getCompletedByClubProfileId()
                                        )),
                                applicationGroups.stream()
                                        .filter(Objects::nonNull)
                                        .flatMap(Collection::stream)
                                        .flatMap(application -> Stream.of(
                                                application.getClubProfileId(),
                                                application.getReviewedByClubProfileId()
                                        ))
                        ),
                        assigneeGroups.stream()
                                .filter(Objects::nonNull)
                                .flatMap(Collection::stream)
                                .flatMap(assignee -> Stream.of(
                                        assignee.getClubProfileId(),
                                        assignee.getAssignedByClubProfileId()
                                ))
                )
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return clubProfileRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(ClubProfile::getClubProfileId, Function.identity()));
    }

    public Map<Long, Integer> resolveApplicationCountByTodoItemId(List<TodoItemApplication> applications) {
        return applications.stream()
                .filter(application -> !APPLICATION_STATUS_WITHDRAWN.equals(application.getApplicationStatus()))
                .collect(Collectors.toMap(
                        TodoItemApplication::getTodoItemId,
                        application -> 1,
                        Integer::sum,
                        LinkedHashMap::new
                ));
    }

    public Comparator<TodoItem> todoPriorityComparator() {
        return Comparator
                .comparing((TodoItem item) -> !isOverdue(item))
                .thenComparing(item -> prioritySortOrder(item.getPriorityCode()))
                .thenComparing(item -> item.getStatusCode(), Comparator.comparingInt(this::statusSortOrder))
                .thenComparing(item -> item.getDueAt() == null)
                .thenComparing(TodoItem::getDueAt, Comparator.nullsLast(LocalDateTime::compareTo))
                .thenComparing(TodoItem::getTodoItemId, Comparator.reverseOrder());
    }

    public TodoSummaryResponse toSummaryResponse(
            TodoItem item,
            Map<Long, ClubProfile> profileById,
            ClubAccessResolver.ClubAccess access,
            boolean adminView,
            Map<Long, Integer> applicationCountByTodoItemId,
            Map<Long, TodoItemApplication> myApplicationByTodoItemId
    ) {
        return toSummaryResponse(
                item,
                profileById,
                access,
                adminView,
                applicationCountByTodoItemId,
                myApplicationByTodoItemId,
                Map.of(),
                Map.of(),
                Map.of()
        );
    }

    public TodoSummaryResponse toSummaryResponse(
            TodoItem item,
            Map<Long, ClubProfile> profileById,
            ClubAccessResolver.ClubAccess access,
            boolean adminView,
            Map<Long, Integer> applicationCountByTodoItemId,
            Map<Long, TodoItemApplication> myApplicationByTodoItemId,
            Map<Long, List<Long>> assigneeIdsByTodoItemId,
            Map<Long, ClubScheduleEvent> scheduleById,
            Map<Long, DecisionRecord> decisionById
    ) {
        TodoItemApplication myApplication = myApplicationByTodoItemId.get(item.getTodoItemId());
        String myApplicationStatus = myApplication == null ? null : myApplication.getApplicationStatus();
        List<Long> assigneeIds = resolveAssigneeIds(item, assigneeIdsByTodoItemId);
        List<TodoAssigneeResponse> assignees = assigneeIds.stream()
                .map(clubProfileId -> new TodoAssigneeResponse(
                        clubProfileId,
                        resolveDisplayName(profileById, clubProfileId)
                ))
                .toList();
        int recruitmentCapacity = item.getRecruitmentCapacity() == null
                ? 1
                : Math.max(1, item.getRecruitmentCapacity());
        boolean recruitmentFull = assigneeIds.size() >= recruitmentCapacity;
        boolean assignedToViewer = assigneeIds.contains(access.clubProfile().getClubProfileId());
        boolean canApply = ASSIGNMENT_MODE_OPEN_SUPPORT.equals(item.getAssignmentMode())
                && !recruitmentFull
                && (STATUS_OPEN.equals(item.getStatusCode()) || STATUS_IN_PROGRESS.equals(item.getStatusCode()))
                && (myApplication == null
                || APPLICATION_STATUS_REJECTED.equals(myApplicationStatus)
                || APPLICATION_STATUS_WITHDRAWN.equals(myApplicationStatus));
        boolean canCancelApplication = ASSIGNMENT_MODE_OPEN_SUPPORT.equals(item.getAssignmentMode())
                && STATUS_OPEN.equals(item.getStatusCode())
                && APPLICATION_STATUS_APPLIED.equals(myApplicationStatus);
        boolean canComplete = assignedToViewer
                && !isTerminalStatus(item.getStatusCode())
                && (!ASSIGNMENT_MODE_OPEN_SUPPORT.equals(item.getAssignmentMode())
                || STATUS_IN_PROGRESS.equals(item.getStatusCode()));
        boolean canEdit = adminView && clubTodoPermissionService.canCreateTodo(access);
        boolean canManageStatus = adminView && clubTodoPermissionService.canManageStatus(access);
        boolean canReviewApplications = adminView
                && clubTodoPermissionService.canAssignTodo(access)
                && ASSIGNMENT_MODE_OPEN_SUPPORT.equals(item.getAssignmentMode());

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
                normalizePriorityCode(item.getPriorityCode()),
                toPriorityLabel(item.getPriorityCode()),
                formatDateTime(item.getDueAt()),
                formatDateTimeLabel(item.getDueAt()),
                formatDateTime(item.getWorkStartAt()),
                formatDateTime(item.getWorkEndAt()),
                formatWorkTimeLabel(item.getWorkStartAt(), item.getWorkEndAt()),
                isOverdue(item),
                assigneeIds.isEmpty() ? null : assigneeIds.getFirst(),
                assignees.isEmpty() ? null : assignees.stream()
                        .map(TodoAssigneeResponse::displayName)
                        .filter(Objects::nonNull)
                        .collect(Collectors.joining(", ")),
                assignees,
                assignees.size(),
                recruitmentCapacity,
                recruitmentFull,
                item.getLinkedScheduleEventId(),
                resolveScheduleTitle(scheduleById, item.getLinkedScheduleEventId()),
                item.getLinkedDecisionRecordId(),
                resolveDecisionTitle(decisionById, item.getLinkedDecisionRecordId()),
                normalizeRecurrenceFrequency(item.getRecurrenceFrequency()),
                normalizeRecurrenceInterval(item.getRecurrenceInterval()),
                formatDate(item.getRecurrenceEndDate()),
                toRecurrenceLabel(
                        item.getRecurrenceFrequency(),
                        item.getRecurrenceInterval(),
                        item.getRecurrenceEndDate()
                ),
                resolveDisplayName(profileById, item.getCreatedByClubProfileId()),
                resolveDisplayName(profileById, item.getCompletedByClubProfileId()),
                formatDateTime(item.getCompletedAt()),
                formatDateTimeLabel(item.getCompletedAt()),
                false,
                canApply,
                canCancelApplication,
                canComplete,
                canEdit,
                canManageStatus,
                canReviewApplications,
                myApplication == null ? null : myApplication.getTodoItemApplicationId(),
                myApplicationStatus,
                toApplicationStatusLabel(myApplicationStatus),
                applicationCountByTodoItemId.getOrDefault(item.getTodoItemId(), 0)
        );
    }

    public TodoActionResponse toActionResponse(TodoItem item, Map<Long, ClubProfile> profileById) {
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

    public TodoItemApplicationResponse toApplicationResponse(
            TodoItemApplication application,
            Map<Long, ClubProfile> profileById,
            Long viewerClubProfileId,
            boolean canReview
    ) {
        return new TodoItemApplicationResponse(
                application.getTodoItemApplicationId(),
                application.getTodoItemId(),
                application.getClubProfileId(),
                resolveDisplayName(profileById, application.getClubProfileId()),
                application.getApplicationStatus(),
                toApplicationStatusLabel(application.getApplicationStatus()),
                application.getApplicationNote(),
                application.getReviewNote(),
                formatDateTimeLabel(application.getCreateDate()),
                formatDateTimeLabel(application.getReviewedAt()),
                Objects.equals(application.getClubProfileId(), viewerClubProfileId),
                canReview && APPLICATION_STATUS_APPLIED.equals(application.getApplicationStatus())
        );
    }

    public String resolveDisplayName(Map<Long, ClubProfile> profileById, Long clubProfileId) {
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

    public String toPriorityLabel(String priorityCode) {
        return switch (normalizePriorityCode(priorityCode)) {
            case "LOW" -> "낮음";
            case "HIGH" -> "높음";
            case "URGENT" -> "긴급";
            default -> "보통";
        };
    }

    public String toAssignmentModeLabel(String assignmentMode) {
        return switch (assignmentMode) {
            case ASSIGNMENT_MODE_DIRECT_ASSIGN -> "직접 배정";
            case ASSIGNMENT_MODE_OPEN_SUPPORT -> "신청 모집";
            default -> assignmentMode;
        };
    }

    public String toStatusLabel(String statusCode) {
        return switch (statusCode) {
            case STATUS_OPEN -> "열림";
            case STATUS_IN_PROGRESS -> "진행중";
            case STATUS_COMPLETED -> "완료";
            case STATUS_CANCELED -> "취소";
            default -> statusCode;
        };
    }

    private String toApplicationStatusLabel(String applicationStatus) {
        if (applicationStatus == null) {
            return null;
        }
        return switch (applicationStatus) {
            case APPLICATION_STATUS_APPLIED -> "신청 대기";
            case APPLICATION_STATUS_SELECTED -> "선정";
            case APPLICATION_STATUS_REJECTED -> "반려";
            case APPLICATION_STATUS_WITHDRAWN -> "취소";
            default -> applicationStatus;
        };
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? null : value.format(REQUEST_DATE_TIME_FORMATTER);
    }

    private String formatDate(LocalDate value) {
        return value == null ? null : value.toString();
    }

    private String formatDateTimeLabel(LocalDateTime value) {
        return value == null ? null : value.format(DATE_TIME_LABEL_FORMATTER);
    }

    private String formatWorkTimeLabel(LocalDateTime startAt, LocalDateTime endAt) {
        if (startAt == null && endAt == null) {
            return null;
        }
        if (startAt == null) {
            return "종료 " + formatDateTimeLabel(endAt);
        }
        if (endAt == null) {
            return formatDateTimeLabel(startAt) + " 시작";
        }
        return formatDateTimeLabel(startAt) + " ~ " + formatDateTimeLabel(endAt);
    }

    public boolean isOverdue(TodoItem item) {
        return item.getDueAt() != null
                && item.getDueAt().isBefore(LocalDateTime.now())
                && !isTerminalStatus(item.getStatusCode());
    }

    private boolean isTerminalStatus(String statusCode) {
        return STATUS_COMPLETED.equals(statusCode) || STATUS_CANCELED.equals(statusCode);
    }

    private int statusSortOrder(String statusCode) {
        return switch (statusCode) {
            case STATUS_OPEN -> 0;
            case STATUS_IN_PROGRESS -> 1;
            case STATUS_COMPLETED -> 2;
            default -> 3;
        };
    }

    private int prioritySortOrder(String priorityCode) {
        return switch (normalizePriorityCode(priorityCode)) {
            case "URGENT" -> 0;
            case "HIGH" -> 1;
            case "NORMAL" -> 2;
            default -> 3;
        };
    }

    private String normalizePriorityCode(String priorityCode) {
        return priorityCode == null || priorityCode.isBlank()
                ? "NORMAL"
                : priorityCode.toUpperCase(Locale.ROOT);
    }

    private String normalizeRecurrenceFrequency(String recurrenceFrequency) {
        return recurrenceFrequency == null || recurrenceFrequency.isBlank()
                ? "NONE"
                : recurrenceFrequency.toUpperCase(Locale.ROOT);
    }

    private int normalizeRecurrenceInterval(Integer recurrenceInterval) {
        return recurrenceInterval == null ? 1 : Math.max(1, recurrenceInterval);
    }

    private String toRecurrenceLabel(
            String recurrenceFrequency,
            Integer recurrenceInterval,
            LocalDate recurrenceEndDate
    ) {
        String frequency = normalizeRecurrenceFrequency(recurrenceFrequency);
        if ("NONE".equals(frequency)) {
            return "반복 없음";
        }
        int interval = normalizeRecurrenceInterval(recurrenceInterval);
        String unit = "WEEKLY".equals(frequency) ? "주" : "개월";
        String label = interval == 1
                ? "WEEKLY".equals(frequency) ? "매주" : "매월"
                : interval + unit + "마다";
        return recurrenceEndDate == null ? label : label + " · " + recurrenceEndDate + "까지";
    }

    private List<Long> resolveAssigneeIds(
            TodoItem item,
            Map<Long, List<Long>> assigneeIdsByTodoItemId
    ) {
        List<Long> assignedIds = assigneeIdsByTodoItemId.getOrDefault(item.getTodoItemId(), List.of());
        if (!assignedIds.isEmpty()) {
            return assignedIds;
        }
        return item.getAssignedClubProfileId() == null
                ? List.of()
                : List.of(item.getAssignedClubProfileId());
    }

    private String resolveScheduleTitle(Map<Long, ClubScheduleEvent> scheduleById, Long eventId) {
        if (eventId == null) {
            return null;
        }
        ClubScheduleEvent event = scheduleById.get(eventId);
        return event == null ? null : event.getTitle();
    }

    private String resolveDecisionTitle(Map<Long, DecisionRecord> decisionById, Long decisionRecordId) {
        if (decisionRecordId == null) {
            return null;
        }
        DecisionRecord decision = decisionById.get(decisionRecordId);
        return decision == null ? null : decision.getTitle();
    }
}
