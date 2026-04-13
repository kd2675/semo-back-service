package semo.back.service.feature.schedule.biz;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import semo.back.service.common.exception.SemoException;
import semo.back.service.common.util.ImageFileUrlResolver;
import semo.back.service.database.pub.entity.ClubProfile;
import semo.back.service.database.pub.repository.ClubProfileRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubScheduleViewSupport {
    private static final DateTimeFormatter DATE_REQUEST_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter DATE_LABEL_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy.MM.dd (E)", Locale.KOREAN);
    private static final DateTimeFormatter TIME_REQUEST_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter TIME_LABEL_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final ClubProfileRepository clubProfileRepository;
    private final ImageFileUrlResolver imageFileUrlResolver;

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
        if (startTime == null && endTime == null) {
            return null;
        }
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

    public LocalDate resolveMonthStart(Integer year, Integer month) {
        LocalDate today = LocalDate.now();
        int resolvedYear = year == null ? today.getYear() : year;
        int resolvedMonth = month == null ? today.getMonthValue() : month;
        if (resolvedMonth < 1 || resolvedMonth > 12) {
            throw new SemoException.ValidationException("조회 월은 1월부터 12월 사이여야 합니다.");
        }
        return LocalDate.of(resolvedYear, resolvedMonth, 1);
    }

    public Map<Long, ClubProfile> loadAuthorProfiles(List<Long> clubProfileIds) {
        if (clubProfileIds.isEmpty()) {
            return Map.of();
        }
        return clubProfileRepository.findAllById(clubProfileIds).stream()
                .collect(Collectors.toMap(ClubProfile::getClubProfileId, Function.identity()));
    }

    public String resolveAuthorDisplayName(ClubProfile authorProfile) {
        return authorProfile == null ? "Unknown Member" : authorProfile.getDisplayName();
    }

    public String resolveAuthorAvatarImageUrl(ClubProfile authorProfile) {
        return authorProfile == null ? null : imageFileUrlResolver.resolveImageUrl(authorProfile.getAvatarFileName());
    }

    public String resolveAuthorAvatarThumbnailUrl(ClubProfile authorProfile) {
        return authorProfile == null ? null : imageFileUrlResolver.resolveThumbnailUrl(authorProfile.getAvatarFileName());
    }

    private String formatDateLabel(LocalDate value) {
        return value.format(DATE_LABEL_FORMATTER);
    }

    private boolean hasExplicitTime(LocalDateTime startAt, LocalDateTime endAt) {
        return !LocalTime.MIDNIGHT.equals(startAt.toLocalTime())
                || (endAt != null && !LocalTime.MIDNIGHT.equals(endAt.toLocalTime()));
    }
}
