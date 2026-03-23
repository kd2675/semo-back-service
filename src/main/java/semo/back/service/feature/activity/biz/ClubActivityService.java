package semo.back.service.feature.activity.biz;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import semo.back.service.database.pub.entity.ClubActivityLog;
import semo.back.service.database.pub.repository.ClubActivityLogRepository;
import semo.back.service.feature.activity.vo.ClubAdminActivityFeedResponse;
import semo.back.service.feature.activity.vo.ClubAdminActivityItemResponse;
import semo.back.service.feature.club.biz.ClubAccessResolver;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubActivityService {
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 50;
    private static final DateTimeFormatter DATE_TIME_REQUEST_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter DATE_TIME_LABEL_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm", Locale.KOREAN);

    private final ClubAccessResolver clubAccessResolver;
    private final ClubActivityLogRepository clubActivityLogRepository;

    public ClubAdminActivityFeedResponse getRecentAdminActivities(
            Long clubId,
            String userKey,
            String cursorCreatedAt,
            Long cursorActivityId,
            Integer size
    ) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireAdmin(clubId, userKey);
        int pageSize = normalizePageSize(size);
        LocalDateTime parsedCursorCreatedAt = parseCursorCreatedAt(cursorCreatedAt);
        List<ClubActivityLog> logs = clubActivityLogRepository.findFeed(
                clubId,
                parsedCursorCreatedAt,
                cursorActivityId,
                PageRequest.of(0, pageSize + 1)
        );
        boolean hasNext = logs.size() > pageSize;
        List<ClubActivityLog> pageLogs = hasNext ? logs.subList(0, pageSize) : logs;
        List<ClubAdminActivityItemResponse> activities = pageLogs.stream()
                .map(this::toResponse)
                .toList();
        ClubActivityLog lastLog = pageLogs.isEmpty() ? null : pageLogs.get(pageLogs.size() - 1);
        return new ClubAdminActivityFeedResponse(
                access.club().getClubId(),
                access.club().getName(),
                activities,
                formatDateTimeValue(lastLog == null ? null : lastLog.getCreatedAt()),
                lastLog == null ? null : lastLog.getClubActivityLogId(),
                hasNext
        );
    }

    private ClubAdminActivityItemResponse toResponse(ClubActivityLog activityLog) {
        return new ClubAdminActivityItemResponse(
                activityLog.getClubActivityLogId(),
                activityLog.getActorDisplayName(),
                toAvatarLabel(activityLog.getActorDisplayName()),
                activityLog.getSubject(),
                activityLog.getDetailText(),
                activityLog.getStatusCode(),
                activityLog.getErrorMessage(),
                formatDateTimeValue(activityLog.getCreatedAt()),
                formatDateTimeLabel(activityLog.getCreatedAt())
        );
    }

    private int normalizePageSize(Integer size) {
        if (size == null || size < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private LocalDateTime parseCursorCreatedAt(String cursorCreatedAt) {
        if (!StringUtils.hasText(cursorCreatedAt)) {
            return null;
        }
        return LocalDateTime.parse(cursorCreatedAt.trim(), DATE_TIME_REQUEST_FORMATTER);
    }

    private String formatDateTimeValue(LocalDateTime value) {
        if (value == null) {
            return null;
        }
        return value.format(DATE_TIME_REQUEST_FORMATTER);
    }

    private String formatDateTimeLabel(LocalDateTime value) {
        if (value == null) {
            return null;
        }
        return value.format(DATE_TIME_LABEL_FORMATTER);
    }

    private String toAvatarLabel(String displayName) {
        if (!StringUtils.hasText(displayName)) {
            return "?";
        }
        return displayName.trim().substring(0, 1);
    }
}
