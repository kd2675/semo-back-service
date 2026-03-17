package semo.back.service.feature.notice.biz;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import semo.back.service.common.exception.SemoException;
import semo.back.service.database.pub.entity.Club;
import semo.back.service.database.pub.entity.ClubNotice;
import semo.back.service.database.pub.entity.ClubProfile;
import semo.back.service.database.pub.entity.ClubScheduleEvent;
import semo.back.service.database.pub.entity.NoticeCategoryCatalog;
import semo.back.service.database.pub.repository.ClubNoticeRepository;
import semo.back.service.database.pub.repository.ClubProfileRepository;
import semo.back.service.database.pub.repository.ClubRepository;
import semo.back.service.database.pub.repository.ClubScheduleEventRepository;
import semo.back.service.database.pub.repository.ClubScheduleVoteRepository;
import semo.back.service.feature.club.biz.ClubAccessResolver;
import semo.back.service.feature.notice.vo.ClubNoticeDetailResponse;
import semo.back.service.feature.notice.vo.ClubNoticeFeedResponse;
import semo.back.service.feature.notice.vo.ClubNoticeSummaryResponse;
import semo.back.service.feature.notice.vo.ClubNoticeUpsertResponse;
import semo.back.service.feature.notice.vo.NoticeCategoryOptionResponse;
import semo.back.service.feature.notice.vo.UpsertClubNoticeRequest;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubNoticeService {
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 30;
    private static final DateTimeFormatter DATE_TIME_REQUEST_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter DATE_TIME_LABEL_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");

    private final ClubNoticeRepository clubNoticeRepository;
    private final ClubRepository clubRepository;
    private final ClubProfileRepository clubProfileRepository;
    private final ClubScheduleEventRepository clubScheduleEventRepository;
    private final ClubScheduleVoteRepository clubScheduleVoteRepository;
    private final ClubAccessResolver clubAccessResolver;
    private final NoticeCategorySupport noticeCategorySupport;

    public ClubNoticeFeedResponse getNoticeFeed(
            Long clubId,
            String userKey,
            String categoryKey,
            String query,
            String cursorPublishedAt,
            Long cursorNoticeId,
            Integer size
    ) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        Club club = getActiveClub(clubId);

        int pageSize = normalizePageSize(size);
        LocalDateTime cursorDateTime = parseCursorDateTime(cursorPublishedAt);
        Long normalizedCursorNoticeId = cursorDateTime == null ? null : (cursorNoticeId == null ? Long.MAX_VALUE : cursorNoticeId);

        List<ClubNotice> notices = clubNoticeRepository.findFeed(
                clubId,
                noticeCategorySupport.normalizeOptionalCategoryKey(categoryKey),
                normalizeQuery(query),
                cursorDateTime,
                normalizedCursorNoticeId,
                PageRequest.of(0, pageSize + 1)
        );

        boolean hasNext = notices.size() > pageSize;
        List<ClubNotice> pageItems = hasNext ? notices.subList(0, pageSize) : notices;

        Map<Long, ClubProfile> profileById = loadProfiles(pageItems);
        Map<Long, LinkedTarget> linkedTargetsByNoticeId = loadLinkedTargets(pageItems);
        Map<String, NoticeCategoryCatalog> categoryByKey = noticeCategorySupport.getActiveCategoryMap();
        List<ClubNoticeSummaryResponse> responses = pageItems.stream()
                .map(notice -> toSummaryResponse(
                        access,
                        notice,
                        profileById.get(notice.getAuthorClubProfileId()),
                        linkedTargetsByNoticeId.get(notice.getNoticeId()),
                        categoryByKey.get(normalizeCategoryKey(notice.getCategoryKey()))
                ))
                .toList();

        ClubNotice lastItem = hasNext ? pageItems.get(pageItems.size() - 1) : null;

        return new ClubNoticeFeedResponse(
                club.getClubId(),
                club.getName(),
                isAdminRole(access.membership().getRoleCode()),
                responses,
                lastItem == null ? null : lastItem.getPublishedAt().format(DATE_TIME_REQUEST_FORMATTER),
                lastItem == null ? null : lastItem.getNoticeId(),
                hasNext
        );
    }

    public ClubNoticeDetailResponse getNoticeDetail(Long clubId, Long noticeId, String userKey) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        Club club = getActiveClub(clubId);
        ClubNotice notice = getNotice(clubId, noticeId);
        ClubProfile authorProfile = clubProfileRepository.findById(notice.getAuthorClubProfileId())
                .orElseThrow(() -> new SemoException.ResourceNotFoundException("ClubProfile", "clubProfileId", notice.getAuthorClubProfileId()));
        LinkedTarget linkedTarget = loadLinkedTarget(notice);
        NoticeCategoryCatalog category = noticeCategorySupport.getActiveCategoryMap()
                .get(normalizeCategoryKey(notice.getCategoryKey()));

        return new ClubNoticeDetailResponse(
                club.getClubId(),
                club.getName(),
                isAdminRole(access.membership().getRoleCode()),
                notice.getNoticeId(),
                notice.getTitle(),
                notice.getContent(),
                notice.getCategoryKey(),
                category == null ? "General" : category.getDisplayName(),
                category == null ? "description" : category.getIconName(),
                category == null ? "slate" : category.getAccentTone(),
                authorProfile.getDisplayName(),
                access.membership().getRoleCode(),
                formatDateTime(notice.getPublishedAt()),
                formatDateTime(notice.getUpdatedAt()),
                notice.isPinned(),
                notice.getLocationLabel(),
                formatDateTimeValue(notice.getScheduleAt()),
                formatDateTime(notice.getScheduleAt()),
                formatDateTimeValue(notice.getScheduleEndAt()),
                formatDateTime(notice.getScheduleEndAt()),
                canManage(access, notice.getAuthorClubProfileId()),
                linkedTarget == null ? null : linkedTarget.type(),
                linkedTarget == null ? null : linkedTarget.targetId()
        );
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    public ClubNoticeUpsertResponse createNotice(Long clubId, String userKey, UpsertClubNoticeRequest request) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireAdmin(clubId, userKey);
        validateRequest(request);
        boolean postToSchedule = shouldPostToSchedule(request.postToSchedule());
        ClubNotice notice = clubNoticeRepository.save(ClubNotice.builder()
                .clubId(clubId)
                .authorClubProfileId(access.clubProfile().getClubProfileId())
                .categoryKey(noticeCategorySupport.normalizeRequiredCategoryKey(request.categoryKey()))
                .title(request.title().trim())
                .content(request.content().trim())
                .locationLabel(trimToNull(request.locationLabel()))
                .scheduleAt(postToSchedule ? parseOptionalDateTime(request.scheduleAt()) : null)
                .scheduleEndAt(postToSchedule ? parseOptionalDateTime(request.scheduleEndAt()) : null)
                .pinned(Boolean.TRUE.equals(request.pinned()))
                .publishedAt(LocalDateTime.now())
                .deleted(false)
                .build());
        upsertLinkedScheduleEvent(notice, access.clubProfile().getClubProfileId());
        return toUpsertResponse(notice);
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    public ClubNoticeUpsertResponse updateNotice(Long clubId, Long noticeId, String userKey, UpsertClubNoticeRequest request) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        validateRequest(request);
        ClubNotice current = getNotice(clubId, noticeId);
        requireManagePermission(access, current.getAuthorClubProfileId());
        boolean postToSchedule = shouldPostToSchedule(request.postToSchedule());
        ClubNotice updated = clubNoticeRepository.save(ClubNotice.builder()
                .noticeId(current.getNoticeId())
                .clubId(current.getClubId())
                .authorClubProfileId(current.getAuthorClubProfileId())
                .categoryKey(noticeCategorySupport.normalizeRequiredCategoryKey(request.categoryKey()))
                .title(request.title().trim())
                .content(request.content().trim())
                .locationLabel(trimToNull(request.locationLabel()))
                .scheduleAt(postToSchedule ? parseOptionalDateTime(request.scheduleAt()) : null)
                .scheduleEndAt(postToSchedule ? parseOptionalDateTime(request.scheduleEndAt()) : null)
                .pinned(Boolean.TRUE.equals(request.pinned()))
                .publishedAt(current.getPublishedAt())
                .deleted(false)
                .build());
        upsertLinkedScheduleEvent(updated, current.getAuthorClubProfileId());
        return toUpsertResponse(updated);
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    public void deleteNotice(Long clubId, Long noticeId, String userKey) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        ClubNotice current = getNotice(clubId, noticeId);
        requireManagePermission(access, current.getAuthorClubProfileId());
        deleteLinkedScheduleEvent(current.getNoticeId());
        clubNoticeRepository.save(ClubNotice.builder()
                .noticeId(current.getNoticeId())
                .clubId(current.getClubId())
                .authorClubProfileId(current.getAuthorClubProfileId())
                .categoryKey(current.getCategoryKey())
                .title(current.getTitle())
                .content(current.getContent())
                .locationLabel(current.getLocationLabel())
                .scheduleAt(current.getScheduleAt())
                .scheduleEndAt(current.getScheduleEndAt())
                .pinned(current.isPinned())
                .publishedAt(current.getPublishedAt())
                .deleted(true)
                .build());
    }

    public List<ClubNotice> getScheduledNotices(Long clubId, LocalDateTime from, LocalDateTime to) {
        return clubNoticeRepository.findScheduledBetween(clubId, from, to);
    }

    public List<NoticeCategoryOptionResponse> getCategoryOptions(Long clubId, String userKey) {
        clubAccessResolver.requireActiveMember(clubId, userKey);
        return noticeCategorySupport.getCategoryOptions();
    }

    private void upsertLinkedScheduleEvent(ClubNotice notice, Long authorClubProfileId) {
        if (notice.getScheduleAt() == null) {
            deleteLinkedScheduleEvent(notice.getNoticeId());
            return;
        }

        ClubScheduleEvent current = clubScheduleEventRepository.findByLinkedNoticeId(notice.getNoticeId())
                .orElse(null);

        clubScheduleEventRepository.save(ClubScheduleEvent.builder()
                .eventId(current == null ? null : current.getEventId())
                .clubId(notice.getClubId())
                .authorClubProfileId(authorClubProfileId)
                .linkedNoticeId(notice.getNoticeId())
                .categoryKey(normalizeCategoryKey(notice.getCategoryKey()))
                .title(notice.getTitle())
                .description(notice.getContent())
                .locationLabel(notice.getLocationLabel())
                .startAt(notice.getScheduleAt())
                .endAt(notice.getScheduleEndAt())
                .attendeeLimit(current == null ? null : current.getAttendeeLimit())
                .visibilityStatus(current == null ? "CLUB" : current.getVisibilityStatus())
                .eventStatus(current == null ? "SCHEDULED" : current.getEventStatus())
                .build());
    }

    private void deleteLinkedScheduleEvent(Long noticeId) {
        clubScheduleEventRepository.findByLinkedNoticeId(noticeId)
                .ifPresent(clubScheduleEventRepository::delete);
    }

    private ClubNoticeUpsertResponse toUpsertResponse(ClubNotice notice) {
        return new ClubNoticeUpsertResponse(
                notice.getNoticeId(),
                notice.getTitle(),
                notice.getCategoryKey(),
                formatDateTimeValue(notice.getScheduleAt()),
                formatDateTime(notice.getScheduleAt()),
                notice.getLocationLabel()
        );
    }

    private Club getActiveClub(Long clubId) {
        return clubRepository.findById(clubId)
                .filter(Club::isActive)
                .orElseThrow(() -> new SemoException.ResourceNotFoundException("Club", "clubId", clubId));
    }

    private ClubNotice getNotice(Long clubId, Long noticeId) {
        return clubNoticeRepository.findByNoticeIdAndClubIdAndDeletedFalse(noticeId, clubId)
                .orElseThrow(() -> new SemoException.ResourceNotFoundException("ClubNotice", "noticeId", noticeId));
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

    private Map<Long, LinkedTarget> loadLinkedTargets(List<ClubNotice> notices) {
        if (notices.isEmpty()) {
            return Map.of();
        }

        Set<Long> noticeIds = notices.stream()
                .map(ClubNotice::getNoticeId)
                .collect(Collectors.toSet());

        Map<Long, LinkedTarget> result = new HashMap<>();
        clubScheduleEventRepository.findByLinkedNoticeIdIn(noticeIds)
                .forEach(event -> result.put(event.getLinkedNoticeId(), new LinkedTarget("SCHEDULE_EVENT", event.getEventId())));
        clubScheduleVoteRepository.findByLinkedNoticeIdIn(noticeIds)
                .forEach(vote -> result.put(vote.getLinkedNoticeId(), new LinkedTarget("SCHEDULE_VOTE", vote.getVoteId())));
        return result;
    }

    private LinkedTarget loadLinkedTarget(ClubNotice notice) {
        return loadLinkedTargets(List.of(notice)).get(notice.getNoticeId());
    }

    private ClubNoticeSummaryResponse toSummaryResponse(
            ClubAccessResolver.ClubAccess access,
            ClubNotice notice,
            ClubProfile authorProfile,
            LinkedTarget linkedTarget,
            NoticeCategoryCatalog category
    ) {
        String authorName = authorProfile == null ? "Unknown Member" : authorProfile.getDisplayName();
        return new ClubNoticeSummaryResponse(
                notice.getNoticeId(),
                notice.getTitle(),
                summarizeContent(notice.getContent()),
                authorName,
                null,
                notice.getCategoryKey(),
                category == null ? "General" : category.getDisplayName(),
                category == null ? "description" : category.getIconName(),
                category == null ? "slate" : category.getAccentTone(),
                formatDateTime(notice.getPublishedAt()),
                toTimeAgo(notice.getPublishedAt()),
                notice.isPinned(),
                formatDateTime(notice.getScheduleAt()),
                notice.getLocationLabel(),
                canManage(access, notice.getAuthorClubProfileId()),
                linkedTarget == null ? null : linkedTarget.type(),
                linkedTarget == null ? null : linkedTarget.targetId()
        );
    }

    private record LinkedTarget(
            String type,
            Long targetId
    ) {
    }

    private String summarizeContent(String content) {
        String normalized = trimToNull(content);
        if (normalized == null) {
            return "";
        }
        return normalized.length() <= 140 ? normalized : normalized.substring(0, 140) + "...";
    }

    private String normalizeCategoryKey(String categoryKey) {
        if (!StringUtils.hasText(categoryKey)) {
            return "ANNOUNCEMENT";
        }
        return categoryKey.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeQuery(String query) {
        return trimToNull(query);
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

    private LocalDateTime parseOptionalDateTime(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return LocalDateTime.parse(value, DATE_TIME_REQUEST_FORMATTER);
        } catch (RuntimeException exception) {
            throw new SemoException.ValidationException("잘못된 날짜 형식입니다.");
        }
    }

    private void validateRequest(UpsertClubNoticeRequest request) {
        if (!shouldPostToSchedule(request.postToSchedule())) {
            return;
        }
        LocalDateTime startAt = parseOptionalDateTime(request.scheduleAt());
        LocalDateTime endAt = parseOptionalDateTime(request.scheduleEndAt());
        if (startAt != null && endAt != null && endAt.isBefore(startAt)) {
            throw new SemoException.ValidationException("종료 시간은 시작 시간보다 빠를 수 없습니다.");
        }
    }

    private boolean shouldPostToSchedule(Boolean postToSchedule) {
        return postToSchedule == null || postToSchedule;
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

    private boolean canManage(ClubAccessResolver.ClubAccess access, Long authorClubProfileId) {
        return isAdminRole(access.membership().getRoleCode())
                || access.clubProfile().getClubProfileId().equals(authorClubProfileId);
    }

    private void requireManagePermission(ClubAccessResolver.ClubAccess access, Long authorClubProfileId) {
        if (!canManage(access, authorClubProfileId)) {
            throw new SemoException.ForbiddenException("공지 수정 또는 삭제 권한이 없습니다.");
        }
    }
}
