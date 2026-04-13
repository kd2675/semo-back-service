package semo.back.service.feature.todo.biz;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import semo.back.service.common.exception.SemoException;
import semo.back.service.database.pub.entity.TodoItem;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;

@Component
public class ClubTodoCommandSupport {
    private static final String STATUS_OPEN = "OPEN";
    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_CANCELED = "CANCELED";
    private static final String STATUS_REOPEN = "REOPEN";

    private static final Set<String> ALLOWED_TODO_TYPES = Set.of("VOLUNTEER", "OPERATIONS");
    private static final Set<String> ALLOWED_ASSIGNMENT_MODES = Set.of("DIRECT_ASSIGN", "OPEN_SUPPORT");
    private static final Set<String> TERMINAL_STATUSES = Set.of(STATUS_COMPLETED, STATUS_CANCELED);
    private static final Set<String> ADMIN_FILTERABLE_STATUSES = Set.of(
            STATUS_OPEN,
            STATUS_IN_PROGRESS,
            STATUS_COMPLETED,
            "OVERDUE"
    );
    private static final Set<String> ALLOWED_STATUS_ACTIONS = Set.of(
            STATUS_OPEN,
            STATUS_IN_PROGRESS,
            STATUS_COMPLETED,
            STATUS_CANCELED,
            STATUS_REOPEN
    );
    private static final Set<String> ALLOWED_ASSIGNMENT_FILTERS = Set.of(
            "ALL",
            "ASSIGNED",
            "UNASSIGNED",
            "OPEN_SUPPORT",
            "DIRECT_ASSIGN"
    );
    private static final Set<String> ALLOWED_APPLICATION_FILTERS = Set.of(
            "ALL",
            "APPLIED",
            "SELECTED",
            "REJECTED",
            "WITHDRAWN"
    );
    private static final Set<String> ALLOWED_APPLICATION_REVIEW_STATUSES = Set.of(
            "SELECTED",
            "REJECTED"
    );
    private static final int DEFAULT_ADMIN_PAGE_SIZE = 12;
    private static final int MAX_ADMIN_PAGE_SIZE = 30;
    private static final DateTimeFormatter REQUEST_DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    String normalizeTitle(String title) {
        String normalized = trimToNull(title);
        if (normalized == null) {
            throw new SemoException.ValidationException("업무 이름은 필수입니다.");
        }
        if (normalized.length() > 150) {
            throw new SemoException.ValidationException("업무 이름은 150자 이하여야 합니다.");
        }
        return normalized;
    }

    String normalizeTodoType(String todoType) {
        String normalized = normalizeUpperCase(todoType, "업무 유형은 필수입니다.");
        if (!ALLOWED_TODO_TYPES.contains(normalized)) {
            throw new SemoException.ValidationException("지원하지 않는 업무 유형입니다.");
        }
        return normalized;
    }

    String normalizeAssignmentMode(String assignmentMode) {
        String normalized = normalizeUpperCase(assignmentMode, "배정 방식은 필수입니다.");
        if (!ALLOWED_ASSIGNMENT_MODES.contains(normalized)) {
            throw new SemoException.ValidationException("지원하지 않는 배정 방식입니다.");
        }
        return normalized;
    }

    String normalizeStatusCode(String statusCode) {
        String normalized = normalizeUpperCase(statusCode, "상태 코드는 필수입니다.");
        if (!ALLOWED_STATUS_ACTIONS.contains(normalized)) {
            throw new SemoException.ValidationException("지원하지 않는 상태 코드입니다.");
        }
        return normalized;
    }

    String normalizeStatusFilter(String statusFilter) {
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

    String normalizeAssignmentFilter(String assignmentFilter) {
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

    String normalizeApplicationFilter(String applicationFilter) {
        String normalized = trimToNull(applicationFilter);
        if (normalized == null) {
            return "ALL";
        }
        normalized = normalized.toUpperCase(Locale.ROOT);
        if (!ALLOWED_APPLICATION_FILTERS.contains(normalized)) {
            throw new SemoException.ValidationException("지원하지 않는 신청 필터입니다.");
        }
        return normalized;
    }

    String normalizeApplicationReviewStatus(String applicationStatus) {
        String normalized = normalizeUpperCase(applicationStatus, "신청 상태는 필수입니다.");
        if (!ALLOWED_APPLICATION_REVIEW_STATUSES.contains(normalized)) {
            throw new SemoException.ValidationException("신청 상태는 SELECTED 또는 REJECTED만 가능합니다.");
        }
        return normalized;
    }

    int normalizeAdminPageSize(Integer size) {
        if (size == null) {
            return DEFAULT_ADMIN_PAGE_SIZE;
        }
        if (size < 1 || size > MAX_ADMIN_PAGE_SIZE) {
            throw new SemoException.ValidationException("페이지 크기는 1~30 범위여야 합니다.");
        }
        return size;
    }

    LocalDateTime parseDateTime(String value, String errorMessage) {
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

    void validateStatusTransition(TodoItem current, String nextStatus) {
        if (STATUS_REOPEN.equals(nextStatus)) {
            if (!TERMINAL_STATUSES.contains(current.getStatusCode())) {
                throw new SemoException.ValidationException("다시 열기는 완료되거나 취소된 업무에만 사용할 수 있습니다.");
            }
            return;
        }
        if (STATUS_OPEN.equals(nextStatus)) {
            if (TERMINAL_STATUSES.contains(current.getStatusCode())) {
                throw new SemoException.ValidationException("종료된 업무는 다시 열기로만 복구할 수 있습니다.");
            }
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

    String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String normalizeUpperCase(String value, String message) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new SemoException.ValidationException(message);
        }
        return normalized.toUpperCase(Locale.ROOT);
    }
}
