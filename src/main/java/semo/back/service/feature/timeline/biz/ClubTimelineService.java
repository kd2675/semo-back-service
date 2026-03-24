package semo.back.service.feature.timeline.biz;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import semo.back.service.common.exception.SemoException;
import semo.back.service.database.pub.entity.ClubActivityLog;
import semo.back.service.database.pub.repository.ClubActivityLogRepository;
import semo.back.service.feature.club.biz.ClubAccessResolver;
import semo.back.service.feature.clubfeature.biz.ClubFeatureService;
import semo.back.service.feature.timeline.vo.ClubAdminTimelineResponse;
import semo.back.service.feature.timeline.vo.ClubTimelineResponse;
import semo.back.service.feature.timeline.vo.TimelineEntryResponse;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubTimelineService {
    private static final String FEATURE_TIMELINE = "TIMELINE";
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 50;
    private static final DateTimeFormatter DATE_TIME_REQUEST_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter DATE_TIME_LABEL_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm", Locale.KOREAN);

    private final ClubActivityLogRepository clubActivityLogRepository;
    private final ClubAccessResolver clubAccessResolver;
    private final ClubFeatureService clubFeatureService;

    public ClubTimelineResponse getTimeline(
            Long clubId,
            String userKey,
            String cursorCreatedAt,
            Long cursorActivityId,
            Integer size
    ) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        requireTimelineFeature(clubId);

        int pageSize = normalizePageSize(size);
        LocalDateTime parsedCursorCreatedAt = parseCursorDateTime(cursorCreatedAt);
        List<ClubActivityLog> logs = clubActivityLogRepository.findActorFeed(
                clubId,
                access.clubProfile().getClubProfileId(),
                parsedCursorCreatedAt,
                cursorActivityId,
                PageRequest.of(0, pageSize + 1)
        );

        boolean hasNext = logs.size() > pageSize;
        List<ClubActivityLog> pageItems = hasNext ? logs.subList(0, pageSize) : logs;
        List<TimelineEntryResponse> entries = pageItems.stream()
                .map(this::toEntryResponse)
                .toList();

        ClubActivityLog lastItem = pageItems.isEmpty() ? null : pageItems.get(pageItems.size() - 1);
        return new ClubTimelineResponse(
                access.club().getClubId(),
                access.club().getName(),
                isAdminRole(access.membership().getRoleCode()),
                entries,
                formatDateTimeValue(lastItem == null ? null : lastItem.getCreatedAt()),
                lastItem == null ? null : lastItem.getClubActivityLogId(),
                hasNext
        );
    }

    public ClubAdminTimelineResponse getAdminTimeline(Long clubId, String userKey) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireAdmin(clubId, userKey);
        requireTimelineFeature(clubId);
        return new ClubAdminTimelineResponse(
                access.club().getClubId(),
                access.club().getName()
        );
    }

    public ClubAdminTimelineResponse updateAdminTimeline(Long clubId, String userKey) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireAdmin(clubId, userKey);
        requireTimelineFeature(clubId);
        return new ClubAdminTimelineResponse(
                access.club().getClubId(),
                access.club().getName()
        );
    }

    private TimelineEntryResponse toEntryResponse(ClubActivityLog activityLog) {
        return new TimelineEntryResponse(
                activityLog.getClubActivityLogId(),
                activityLog.getActorDisplayName(),
                toAvatarLabel(activityLog.getActorDisplayName()),
                activityLog.getSubject(),
                activityLog.getDetailText(),
                activityLog.getStatusCode(),
                formatDateTimeValue(activityLog.getCreatedAt()),
                formatDateTime(activityLog.getCreatedAt())
        );
    }

    private boolean requireTimelineFeature(Long clubId) {
        if (!clubFeatureService.isFeatureEnabled(clubId, FEATURE_TIMELINE)) {
            throw new SemoException.ValidationException("타임라인 기능이 활성화되지 않았습니다.");
        }
        return true;
    }

    private LocalDateTime parseCursorDateTime(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return LocalDateTime.parse(value, DATE_TIME_REQUEST_FORMATTER);
        } catch (RuntimeException exception) {
            throw new SemoException.ValidationException("잘못된 커서 형식입니다.");
        }
    }

    private String formatDateTime(LocalDateTime value) {
        if (value == null) {
            return null;
        }
        return value.format(DATE_TIME_LABEL_FORMATTER);
    }

    private String formatDateTimeValue(LocalDateTime value) {
        if (value == null) {
            return null;
        }
        return value.format(DATE_TIME_REQUEST_FORMATTER);
    }

    private int normalizePageSize(Integer size) {
        if (size == null || size < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private boolean isAdminRole(String roleCode) {
        return "OWNER".equals(roleCode) || "ADMIN".equals(roleCode);
    }

    private String toAvatarLabel(String displayName) {
        if (!StringUtils.hasText(displayName)) {
            return "?";
        }
        return displayName.trim().substring(0, 1);
    }
}
