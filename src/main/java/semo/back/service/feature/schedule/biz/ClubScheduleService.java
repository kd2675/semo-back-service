package semo.back.service.feature.schedule.biz;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import semo.back.service.common.exception.SemoException;
import semo.back.service.database.pub.entity.Club;
import semo.back.service.database.pub.entity.ClubNotice;
import semo.back.service.database.pub.entity.ClubScheduleEvent;
import semo.back.service.database.pub.repository.ClubNoticeRepository;
import semo.back.service.database.pub.repository.ClubRepository;
import semo.back.service.database.pub.repository.ClubScheduleEventRepository;
import semo.back.service.feature.club.biz.ClubAccessResolver;
import semo.back.service.feature.schedule.vo.ScheduleEventDetailResponse;
import semo.back.service.feature.schedule.vo.ScheduleEventUpsertResponse;
import semo.back.service.feature.schedule.vo.UpsertScheduleEventRequest;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubScheduleService {
    private static final DateTimeFormatter DATE_TIME_REQUEST_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter DATE_TIME_LABEL_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");

    private final ClubScheduleEventRepository clubScheduleEventRepository;
    private final ClubNoticeRepository clubNoticeRepository;
    private final ClubRepository clubRepository;
    private final ClubAccessResolver clubAccessResolver;

    public ScheduleEventDetailResponse getScheduleEventDetail(Long clubId, Long eventId, String userKey) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        Club club = getActiveClub(clubId);
        ClubScheduleEvent event = getEvent(clubId, eventId);
        boolean canManage = isAdminRole(access.membership().getRoleCode())
                || access.clubProfile().getClubProfileId().equals(event.getAuthorClubProfileId());
        return new ScheduleEventDetailResponse(
                club.getClubId(),
                club.getName(),
                isAdminRole(access.membership().getRoleCode()),
                event.getEventId(),
                event.getTitle(),
                event.getDescription(),
                event.getCategoryKey(),
                event.getLocationLabel(),
                formatDateTimeValue(event.getStartAt()),
                formatDateTime(event.getStartAt()),
                formatDateTimeValue(event.getEndAt()),
                formatDateTime(event.getEndAt()),
                event.getLinkedNoticeId() != null,
                event.getLinkedNoticeId(),
                canManage
        );
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    public ScheduleEventUpsertResponse createScheduleEvent(Long clubId, String userKey, UpsertScheduleEventRequest request) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireAdmin(clubId, userKey);
        validateRequest(request);
        LocalDateTime startAt = parseDateTime(request.startAt());
        LocalDateTime endAt = parseOptionalDateTime(request.endAt());

        ClubScheduleEvent event = clubScheduleEventRepository.save(ClubScheduleEvent.builder()
                .clubId(clubId)
                .authorClubProfileId(access.clubProfile().getClubProfileId())
                .linkedNoticeId(null)
                .categoryKey(normalizeCategoryKey(request.categoryKey()))
                .title(request.title().trim())
                .description(trimToNull(request.description()))
                .locationLabel(trimToNull(request.locationLabel()))
                .startAt(startAt)
                .endAt(endAt)
                .attendeeLimit(null)
                .visibilityStatus("CLUB")
                .eventStatus("SCHEDULED")
                .build());

        if (shouldPostToBoard(request.postToBoard())) {
            ClubNotice linkedNotice = clubNoticeRepository.save(ClubNotice.builder()
                    .clubId(clubId)
                    .authorClubProfileId(access.clubProfile().getClubProfileId())
                    .categoryKey(normalizeCategoryKey(request.categoryKey()))
                    .title(request.title().trim())
                    .content(buildBoardContent(request))
                    .locationLabel(trimToNull(request.locationLabel()))
                    .scheduleAt(startAt)
                    .scheduleEndAt(endAt)
                    .pinned(false)
                    .publishedAt(LocalDateTime.now())
                    .deleted(false)
                    .build());
            event = clubScheduleEventRepository.save(ClubScheduleEvent.builder()
                    .eventId(event.getEventId())
                    .clubId(event.getClubId())
                    .authorClubProfileId(event.getAuthorClubProfileId())
                    .linkedNoticeId(linkedNotice.getNoticeId())
                    .categoryKey(event.getCategoryKey())
                    .title(event.getTitle())
                    .description(event.getDescription())
                    .locationLabel(event.getLocationLabel())
                    .startAt(event.getStartAt())
                    .endAt(event.getEndAt())
                    .attendeeLimit(event.getAttendeeLimit())
                    .visibilityStatus(event.getVisibilityStatus())
                    .eventStatus(event.getEventStatus())
                    .build());
        }

        return toUpsertResponse(event);
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    public ScheduleEventUpsertResponse updateScheduleEvent(Long clubId, Long eventId, String userKey, UpsertScheduleEventRequest request) {
        clubAccessResolver.requireAdmin(clubId, userKey);
        validateRequest(request);
        ClubScheduleEvent current = getEvent(clubId, eventId);
        LocalDateTime startAt = parseDateTime(request.startAt());
        LocalDateTime endAt = parseOptionalDateTime(request.endAt());

        Long linkedNoticeId = current.getLinkedNoticeId();
        boolean postToBoard = shouldPostToBoard(request.postToBoard());
        if (linkedNoticeId != null && !postToBoard) {
            softDeleteNotice(linkedNoticeId);
            linkedNoticeId = null;
        } else if (postToBoard) {
            linkedNoticeId = upsertLinkedNotice(current.getClubId(), current.getAuthorClubProfileId(), linkedNoticeId, request, startAt, endAt);
        }

        ClubScheduleEvent updated = clubScheduleEventRepository.save(ClubScheduleEvent.builder()
                .eventId(current.getEventId())
                .clubId(current.getClubId())
                .authorClubProfileId(current.getAuthorClubProfileId())
                .linkedNoticeId(linkedNoticeId)
                .categoryKey(normalizeCategoryKey(request.categoryKey()))
                .title(request.title().trim())
                .description(trimToNull(request.description()))
                .locationLabel(trimToNull(request.locationLabel()))
                .startAt(startAt)
                .endAt(endAt)
                .attendeeLimit(current.getAttendeeLimit())
                .visibilityStatus(current.getVisibilityStatus())
                .eventStatus("SCHEDULED")
                .build());

        return toUpsertResponse(updated);
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    public void deleteScheduleEvent(Long clubId, Long eventId, String userKey) {
        clubAccessResolver.requireAdmin(clubId, userKey);
        ClubScheduleEvent current = getEvent(clubId, eventId);
        if (current.getLinkedNoticeId() != null) {
            softDeleteNotice(current.getLinkedNoticeId());
        }
        clubScheduleEventRepository.delete(current);
    }

    private Long upsertLinkedNotice(
            Long clubId,
            Long authorClubProfileId,
            Long linkedNoticeId,
            UpsertScheduleEventRequest request,
            LocalDateTime startAt,
            LocalDateTime endAt
    ) {
        ClubNotice current = linkedNoticeId == null ? null : clubNoticeRepository.findById(linkedNoticeId).orElse(null);
        ClubNotice saved = clubNoticeRepository.save(ClubNotice.builder()
                .noticeId(current == null ? null : current.getNoticeId())
                .clubId(clubId)
                .authorClubProfileId(authorClubProfileId)
                .categoryKey(normalizeCategoryKey(request.categoryKey()))
                .title(request.title().trim())
                .content(buildBoardContent(request))
                .locationLabel(trimToNull(request.locationLabel()))
                .scheduleAt(startAt)
                .scheduleEndAt(endAt)
                .pinned(current != null && current.isPinned())
                .publishedAt(current == null ? LocalDateTime.now() : current.getPublishedAt())
                .deleted(false)
                .build());
        return saved.getNoticeId();
    }

    private void softDeleteNotice(Long noticeId) {
        clubNoticeRepository.findById(noticeId).ifPresent(current -> clubNoticeRepository.save(ClubNotice.builder()
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
                .build()));
    }

    private ScheduleEventUpsertResponse toUpsertResponse(ClubScheduleEvent event) {
        return new ScheduleEventUpsertResponse(
                event.getEventId(),
                event.getLinkedNoticeId(),
                event.getTitle(),
                formatDateTimeValue(event.getStartAt()),
                formatDateTime(event.getStartAt()),
                formatDateTimeValue(event.getEndAt()),
                formatDateTime(event.getEndAt()),
                event.getLinkedNoticeId() != null
        );
    }

    private Club getActiveClub(Long clubId) {
        return clubRepository.findById(clubId)
                .filter(Club::isActive)
                .orElseThrow(() -> new SemoException.ResourceNotFoundException("Club", "clubId", clubId));
    }

    private ClubScheduleEvent getEvent(Long clubId, Long eventId) {
        return clubScheduleEventRepository.findByEventIdAndClubId(eventId, clubId)
                .orElseThrow(() -> new SemoException.ResourceNotFoundException("ScheduleEvent", "eventId", eventId));
    }

    private void validateRequest(UpsertScheduleEventRequest request) {
        LocalDateTime startAt = parseDateTime(request.startAt());
        LocalDateTime endAt = parseOptionalDateTime(request.endAt());
        if (endAt != null && endAt.isBefore(startAt)) {
            throw new SemoException.ValidationException("종료 시간은 시작 시간보다 빠를 수 없습니다.");
        }
    }

    private String buildBoardContent(UpsertScheduleEventRequest request) {
        String description = trimToNull(request.description());
        if (description != null) {
            return description;
        }
        return request.title().trim() + " 일정이 등록되었습니다.";
    }

    private String normalizeCategoryKey(String categoryKey) {
        if (!StringUtils.hasText(categoryKey)) {
            return "GENERAL";
        }
        return categoryKey.trim().toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private LocalDateTime parseDateTime(String value) {
        try {
            return LocalDateTime.parse(value, DATE_TIME_REQUEST_FORMATTER);
        } catch (RuntimeException exception) {
            throw new SemoException.ValidationException("잘못된 날짜 형식입니다.");
        }
    }

    private LocalDateTime parseOptionalDateTime(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return parseDateTime(value);
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

    private boolean isAdminRole(String roleCode) {
        return "OWNER".equals(roleCode) || "ADMIN".equals(roleCode);
    }

    private boolean shouldPostToBoard(Boolean postToBoard) {
        return postToBoard == null || postToBoard;
    }
}
