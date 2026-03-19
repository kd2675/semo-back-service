package semo.back.service.feature.timeline.biz;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import semo.back.service.common.exception.SemoException;
import semo.back.service.database.pub.entity.ClubNotice;
import semo.back.service.database.pub.entity.ClubProfile;
import semo.back.service.database.pub.repository.ClubNoticeRepository;
import semo.back.service.database.pub.repository.ClubProfileRepository;
import semo.back.service.database.pub.repository.ClubScheduleEventRepository;
import semo.back.service.database.pub.repository.ClubScheduleVoteRepository;
import semo.back.service.feature.club.biz.ClubAccessResolver;
import semo.back.service.feature.clubfeature.biz.ClubFeatureService;
import semo.back.service.feature.timeline.vo.ClubAdminTimelineResponse;
import semo.back.service.feature.timeline.vo.ClubTimelineResponse;
import semo.back.service.feature.timeline.vo.TimelineEntryResponse;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubTimelineService {
    private static final String FEATURE_POLL = "POLL";
    private static final String FEATURE_TIMELINE = "TIMELINE";
    private static final int DEFAULT_PAGE_SIZE = 12;
    private static final int MAX_PAGE_SIZE = 30;
    private static final DateTimeFormatter DATE_TIME_REQUEST_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter DATE_TIME_LABEL_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");

    private final ClubNoticeRepository clubNoticeRepository;
    private final ClubProfileRepository clubProfileRepository;
    private final ClubScheduleEventRepository clubScheduleEventRepository;
    private final ClubScheduleVoteRepository clubScheduleVoteRepository;
    private final ClubAccessResolver clubAccessResolver;
    private final ClubFeatureService clubFeatureService;

    public ClubTimelineResponse getTimeline(
            Long clubId,
            String userKey,
            String cursorPublishedAt,
            Long cursorNoticeId,
            Integer size
    ) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        requireTimelineFeature(clubId);
        boolean pollFeatureEnabled = clubFeatureService.isFeatureEnabled(clubId, FEATURE_POLL);

        int pageSize = normalizePageSize(size);
        LocalDateTime cursorDateTime = parseCursorDateTime(cursorPublishedAt);
        Long normalizedCursorNoticeId = cursorDateTime == null ? null : (cursorNoticeId == null ? Long.MAX_VALUE : cursorNoticeId);

        List<ClubNotice> notices = clubNoticeRepository.findTimelineFeed(
                clubId,
                pollFeatureEnabled,
                cursorDateTime,
                normalizedCursorNoticeId,
                PageRequest.of(0, pageSize + 1)
        );

        boolean hasNext = notices.size() > pageSize;
        List<ClubNotice> pageItems = hasNext ? notices.subList(0, pageSize) : notices;
        Map<Long, ClubProfile> profileById = loadProfiles(pageItems);
        Map<Long, LinkedTarget> linkedTargetsByNoticeId = loadLinkedTargets(pageItems, pollFeatureEnabled);
        List<TimelineEntryResponse> entries = pageItems.stream()
                .map(notice -> toEntryResponse(
                        notice,
                        profileById.get(notice.getAuthorClubProfileId()),
                        linkedTargetsByNoticeId.get(notice.getNoticeId())
                ))
                .toList();

        ClubNotice lastItem = hasNext ? pageItems.get(pageItems.size() - 1) : null;
        return new ClubTimelineResponse(
                access.club().getClubId(),
                access.club().getName(),
                isAdminRole(access.membership().getRoleCode()),
                entries,
                lastItem == null ? null : formatDateTimeValue(lastItem.getPublishedAt()),
                lastItem == null ? null : lastItem.getNoticeId(),
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

    private TimelineEntryResponse toEntryResponse(
            ClubNotice notice,
            ClubProfile authorProfile,
            LinkedTarget linkedTarget
    ) {
        return new TimelineEntryResponse(
                notice.getNoticeId(),
                notice.getTitle(),
                summarizeContent(notice.getContent()),
                authorProfile == null ? "Unknown Member" : authorProfile.getDisplayName(),
                formatDateTimeValue(notice.getPublishedAt()),
                formatDateTime(notice.getPublishedAt()),
                toTimeAgo(notice.getPublishedAt()),
                notice.isPinned(),
                formatDateTime(notice.getScheduleAt()),
                notice.getLocationLabel(),
                linkedTarget == null ? null : linkedTarget.type(),
                linkedTarget == null ? null : linkedTarget.targetId()
        );
    }

    private Map<Long, ClubProfile> loadProfiles(List<ClubNotice> notices) {
        if (notices.isEmpty()) {
            return Map.of();
        }
        Map<Long, ClubProfile> result = new HashMap<>();
        clubProfileRepository.findAllById(notices.stream().map(ClubNotice::getAuthorClubProfileId).distinct().toList())
                .forEach(profile -> result.put(profile.getClubProfileId(), profile));
        return result;
    }

    private Map<Long, LinkedTarget> loadLinkedTargets(List<ClubNotice> notices, boolean pollFeatureEnabled) {
        if (notices.isEmpty()) {
            return Map.of();
        }

        Set<Long> noticeIds = notices.stream()
                .map(ClubNotice::getNoticeId)
                .collect(Collectors.toSet());
        Map<Long, LinkedTarget> result = new HashMap<>();
        clubScheduleEventRepository.findByLinkedNoticeIdIn(noticeIds)
                .forEach(event -> result.put(event.getLinkedNoticeId(), new LinkedTarget("SCHEDULE_EVENT", event.getEventId())));
        if (pollFeatureEnabled) {
            clubScheduleVoteRepository.findByLinkedNoticeIdIn(noticeIds)
                    .forEach(vote -> result.put(vote.getLinkedNoticeId(), new LinkedTarget("POLL", vote.getVoteId())));
        }
        return result;
    }

    private boolean requireTimelineFeature(Long clubId) {
        if (!clubFeatureService.isFeatureEnabled(clubId, FEATURE_TIMELINE)) {
            throw new SemoException.ValidationException("타임라인 기능이 활성화되지 않았습니다.");
        }
        return true;
    }

    private String summarizeContent(String content) {
        String normalized = trimToNull(content);
        if (normalized == null) {
            return "";
        }
        return normalized.length() <= 140 ? normalized : normalized.substring(0, 140) + "...";
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
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

    private String toTimeAgo(LocalDateTime value) {
        if (value == null) {
            return "";
        }
        Duration duration = Duration.between(value, LocalDateTime.now());
        long minutes = Math.max(duration.toMinutes(), 0);
        if (minutes < 1) {
            return "just now";
        }
        if (minutes < 60) {
            return minutes + "m ago";
        }
        long hours = duration.toHours();
        if (hours < 24) {
            return hours + "h ago";
        }
        long days = duration.toDays();
        if (days < 7) {
            return days + "d ago";
        }
        return formatDateTime(value);
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

    private record LinkedTarget(
            String type,
            Long targetId
    ) {
    }
}
