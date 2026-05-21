package semo.back.service.feature.activity.biz;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import semo.back.service.database.pub.entity.ClubActivityLog;
import semo.back.service.database.pub.entity.ClubMemberPositionHistory;
import semo.back.service.database.pub.repository.ClubActivityLogRepository;
import semo.back.service.database.pub.repository.ClubMemberPositionHistoryRepository;
import semo.back.service.feature.activity.vo.ClubAdminActivityFeedResponse;
import semo.back.service.feature.activity.vo.ClubAdminActivityItemResponse;
import semo.back.service.feature.activity.vo.ClubAdminActivityPositionResponse;
import semo.back.service.feature.club.biz.policy.ClubAccessResolver;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    private final ClubMemberPositionHistoryRepository clubMemberPositionHistoryRepository;

    public ClubAdminActivityFeedResponse getRecentAdminActivities(
            Long clubId,
            String userKey,
            String cursorCreatedAt,
            Long cursorActivityId,
            Integer size,
            Long clubPositionId
    ) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireAdmin(clubId, userKey);
        int pageSize = normalizePageSize(size);
        Long normalizedPositionId = normalizePositionId(clubPositionId);
        LocalDateTime parsedCursorCreatedAt = parseCursorCreatedAt(cursorCreatedAt);
        List<ClubActivityLog> logs = normalizedPositionId == null
                ? clubActivityLogRepository.findFeed(
                        clubId,
                        parsedCursorCreatedAt,
                        cursorActivityId,
                        PageRequest.of(0, pageSize + 1)
                )
                : clubActivityLogRepository.findFeedByPosition(
                        clubId,
                        normalizedPositionId,
                        parsedCursorCreatedAt,
                        cursorActivityId,
                        PageRequest.of(0, pageSize + 1)
                );
        boolean hasNext = logs.size() > pageSize;
        List<ClubActivityLog> pageLogs = hasNext ? logs.subList(0, pageSize) : logs;
        Map<Long, List<ClubAdminActivityPositionResponse>> positionsByActivityId = resolvePositionsByActivityId(clubId, pageLogs);
        List<ClubAdminActivityItemResponse> activities = pageLogs.stream()
                .map(activity -> toResponse(activity, positionsByActivityId.getOrDefault(activity.getClubActivityLogId(), List.of())))
                .toList();
        ClubActivityLog lastLog = pageLogs.isEmpty() ? null : pageLogs.get(pageLogs.size() - 1);
        return new ClubAdminActivityFeedResponse(
                access.club().getClubId(),
                access.club().getName(),
                normalizedPositionId,
                resolvePositionFilters(clubId),
                activities,
                formatDateTimeValue(lastLog == null ? null : lastLog.getCreatedAt()),
                lastLog == null ? null : lastLog.getClubActivityLogId(),
                hasNext
        );
    }

    private ClubAdminActivityItemResponse toResponse(
            ClubActivityLog activityLog,
            List<ClubAdminActivityPositionResponse> actorPositions
    ) {
        return new ClubAdminActivityItemResponse(
                activityLog.getClubActivityLogId(),
                activityLog.getActorDisplayName(),
                toAvatarLabel(activityLog.getActorDisplayName()),
                actorPositions,
                activityLog.getSubject(),
                activityLog.getDetailText(),
                activityLog.getStatusCode(),
                activityLog.getErrorMessage(),
                formatDateTimeValue(activityLog.getCreatedAt()),
                formatDateTimeLabel(activityLog.getCreatedAt())
        );
    }

    private List<ClubAdminActivityPositionResponse> resolvePositionFilters(Long clubId) {
        Map<Long, ClubAdminActivityPositionResponse> filtersByPositionId = new LinkedHashMap<>();
        clubMemberPositionHistoryRepository.findByClubIdAndDeletedFalseOrderByStartedAtDescClubMemberPositionHistoryIdDesc(clubId)
                .forEach(history -> filtersByPositionId.putIfAbsent(
                        history.getClubPositionId(),
                        toPositionResponse(history)
                ));
        return filtersByPositionId.values().stream()
                .sorted(Comparator.comparing(ClubAdminActivityPositionResponse::displayName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(ClubAdminActivityPositionResponse::clubPositionId))
                .toList();
    }

    private Map<Long, List<ClubAdminActivityPositionResponse>> resolvePositionsByActivityId(
            Long clubId,
            List<ClubActivityLog> pageLogs
    ) {
        List<ClubActivityLog> logsWithMember = pageLogs.stream()
                .filter(log -> log.getActorClubMemberId() != null && log.getCreatedAt() != null)
                .toList();
        if (logsWithMember.isEmpty()) {
            return Map.of();
        }
        LocalDateTime minCreatedAt = logsWithMember.stream()
                .map(ClubActivityLog::getCreatedAt)
                .min(LocalDateTime::compareTo)
                .orElse(null);
        LocalDateTime maxCreatedAt = logsWithMember.stream()
                .map(ClubActivityLog::getCreatedAt)
                .max(LocalDateTime::compareTo)
                .orElse(null);
        if (minCreatedAt == null || maxCreatedAt == null) {
            return Map.of();
        }
        Set<Long> clubMemberIds = logsWithMember.stream()
                .map(ClubActivityLog::getActorClubMemberId)
                .collect(Collectors.toSet());
        Map<Long, List<ClubMemberPositionHistory>> historiesByMemberId = clubMemberPositionHistoryRepository
                .findEffectiveHistoriesForActivityWindow(clubId, clubMemberIds, minCreatedAt, maxCreatedAt).stream()
                .collect(Collectors.groupingBy(ClubMemberPositionHistory::getClubMemberId));

        Map<Long, List<ClubAdminActivityPositionResponse>> result = new LinkedHashMap<>();
        for (ClubActivityLog log : logsWithMember) {
            List<ClubAdminActivityPositionResponse> positions = historiesByMemberId
                    .getOrDefault(log.getActorClubMemberId(), List.of())
                    .stream()
                    .filter(history -> isEffectiveAt(history, log.getCreatedAt()))
                    .map(this::toPositionResponse)
                    .collect(Collectors.collectingAndThen(
                            Collectors.toMap(
                                    ClubAdminActivityPositionResponse::clubPositionId,
                                    Function.identity(),
                                    (left, right) -> left,
                                    LinkedHashMap::new
                            ),
                            map -> map.values().stream()
                                    .sorted(Comparator.comparing(ClubAdminActivityPositionResponse::displayName, String.CASE_INSENSITIVE_ORDER)
                                            .thenComparing(ClubAdminActivityPositionResponse::clubPositionId))
                                    .toList()
                    ));
            result.put(log.getClubActivityLogId(), positions);
        }
        return result;
    }

    private int normalizePageSize(Integer size) {
        if (size == null || size < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private Long normalizePositionId(Long clubPositionId) {
        return clubPositionId == null || clubPositionId < 1 ? null : clubPositionId;
    }

    private LocalDateTime parseCursorCreatedAt(String cursorCreatedAt) {
        if (!StringUtils.hasText(cursorCreatedAt)) {
            return null;
        }
        return LocalDateTime.parse(cursorCreatedAt.trim(), DATE_TIME_REQUEST_FORMATTER);
    }

    private boolean isEffectiveAt(ClubMemberPositionHistory history, LocalDateTime value) {
        if (history == null || value == null || history.isDeleted()) {
            return false;
        }
        boolean startsBeforeOrAt = !history.getStartedAt().isAfter(value);
        boolean endsAfterOrAt = history.getEndedAt() == null || !history.getEndedAt().isBefore(value);
        return startsBeforeOrAt && endsAfterOrAt;
    }

    private ClubAdminActivityPositionResponse toPositionResponse(ClubMemberPositionHistory history) {
        return new ClubAdminActivityPositionResponse(
                history.getClubPositionId(),
                history.getPositionCodeSnapshot(),
                history.getPositionDisplayNameSnapshot()
        );
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
