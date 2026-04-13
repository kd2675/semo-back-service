package semo.back.service.feature.schedule.biz;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import semo.back.service.common.exception.SemoException;
import semo.back.service.database.pub.entity.ClubScheduleVote;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

@Component
public class ClubScheduleFormatter {
    private static final DateTimeFormatter DATE_REQUEST_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DATE_LABEL_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy.MM.dd (E)", Locale.KOREAN);
    private static final DateTimeFormatter TIME_REQUEST_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter TIME_LABEL_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    public LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value, DATE_REQUEST_FORMATTER);
        } catch (DateTimeParseException exception) {
            throw new SemoException.ValidationException("잘못된 날짜 형식입니다.");
        }
    }

    public LocalDate parseOptionalDate(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return parseDate(value);
    }

    public LocalTime parseOptionalTime(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return LocalTime.parse(value, TIME_REQUEST_FORMATTER);
        } catch (DateTimeParseException exception) {
            throw new SemoException.ValidationException("잘못된 시간 형식입니다.");
        }
    }

    public String formatDateValue(LocalDate value) {
        return value.format(DATE_REQUEST_FORMATTER);
    }

    public String formatDateRangeLabel(LocalDate startDate, LocalDate endDate) {
        if (endDate == null || endDate.equals(startDate)) {
            return formatDateLabel(startDate);
        }
        return formatDateLabel(startDate) + " - " + formatDateLabel(endDate);
    }

    public String formatOptionalTimeValue(LocalTime value) {
        if (value == null) {
            return null;
        }
        return value.format(TIME_REQUEST_FORMATTER);
    }

    public String formatVoteTimeLabel(LocalTime startTime, LocalTime endTime) {
        if (startTime == null) {
            return null;
        }
        if (endTime == null) {
            return startTime.format(TIME_LABEL_FORMATTER);
        }
        return startTime.format(TIME_LABEL_FORMATTER)
                + " - "
                + endTime.format(TIME_LABEL_FORMATTER);
    }

    public String formatTimeValue(LocalDateTime startAt, LocalDateTime endAt) {
        if (!hasExplicitTime(startAt, endAt)) {
            return null;
        }
        return startAt.toLocalTime().format(TIME_REQUEST_FORMATTER);
    }

    public String formatEndTimeValue(LocalDateTime startAt, LocalDateTime endAt) {
        if (endAt == null || !hasExplicitTime(startAt, endAt)) {
            return null;
        }
        return endAt.toLocalTime().format(TIME_REQUEST_FORMATTER);
    }

    public String formatTimeLabel(LocalDateTime startAt, LocalDateTime endAt) {
        if (!hasExplicitTime(startAt, endAt)) {
            return null;
        }
        if (endAt == null) {
            return startAt.toLocalTime().format(TIME_LABEL_FORMATTER);
        }
        return startAt.toLocalTime().format(TIME_LABEL_FORMATTER)
                + " - "
                + endAt.toLocalTime().format(TIME_LABEL_FORMATTER);
    }

    public boolean shouldPostToBoard(Boolean postToBoard) {
        return postToBoard == null || postToBoard;
    }

    public boolean shouldPin(Boolean pinned) {
        return Boolean.TRUE.equals(pinned);
    }

    public boolean shouldPostToCalendar(Boolean postToCalendar) {
        return postToCalendar == null || postToCalendar;
    }

    public boolean shouldPostVoteToCalendar(Boolean postToCalendar, Boolean postToSchedule) {
        if (postToCalendar != null) {
            return postToCalendar;
        }
        if (postToSchedule != null) {
            return postToSchedule;
        }
        return true;
    }

    public LocalDate resolveMonthStart(Integer year, Integer month) {
        LocalDate today = LocalDate.now();
        int resolvedYear = year == null ? today.getYear() : year;
        int resolvedMonth = month == null ? today.getMonthValue() : month;
        if (resolvedMonth < 1 || resolvedMonth > 12) {
            throw new SemoException.ValidationException("조회 월은 1월부터 12월 사이여야 합니다.");
        }
        return LocalDate.of(resolvedYear, resolvedMonth, 1);
    }

    public LocalDateTime toVoteStartAt(LocalDate startDate, LocalTime startTime) {
        return startDate.atTime(startTime == null ? LocalTime.MIDNIGHT : startTime);
    }

    public LocalDateTime toVoteEffectiveEndAt(LocalDate endDate, LocalTime endTime) {
        return endDate.atTime(endTime == null ? LocalTime.MAX : endTime);
    }

    public boolean isVoteOpen(ClubScheduleVote vote) {
        return "ONGOING".equals(resolveVoteStatus(vote));
    }

    public String resolveVoteStatus(ClubScheduleVote vote) {
        if (vote.getClosedAt() != null) {
            return "CLOSED";
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startAt = toVoteStartAt(vote.getVoteStartDate(), vote.getVoteStartTime());
        if (now.isBefore(startAt)) {
            return "WAITING";
        }
        if (now.isAfter(toVoteEffectiveEndAt(vote.getVoteEndDate(), vote.getVoteEndTime()))) {
            return "CLOSED";
        }
        return "ONGOING";
    }

    public String trimRequired(String value, String message) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new SemoException.ValidationException(message);
        }
        return normalized;
    }

    public String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String formatDateLabel(LocalDate value) {
        return value.format(DATE_LABEL_FORMATTER);
    }

    private boolean hasExplicitTime(LocalDateTime startAt, LocalDateTime endAt) {
        return !LocalTime.MIDNIGHT.equals(startAt.toLocalTime())
                || (endAt != null && !LocalTime.MIDNIGHT.equals(endAt.toLocalTime()));
    }
}
