package semo.back.service.feature.notice.biz;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import semo.back.service.common.exception.SemoException;
import semo.back.service.common.util.ImageFileUrlResolver;
import semo.back.service.common.util.ImageFinalizeClient;
import semo.back.service.database.pub.entity.Club;
import semo.back.service.database.pub.entity.ClubNotice;
import semo.back.service.database.pub.entity.ClubProfile;
import semo.back.service.database.pub.repository.ClubNoticeRepository;
import semo.back.service.database.pub.repository.ClubProfileRepository;
import semo.back.service.database.pub.repository.ClubRepository;
import semo.back.service.feature.activity.biz.ClubActivityContextHolder;
import semo.back.service.feature.activity.biz.RecordClubActivity;
import semo.back.service.feature.club.biz.ClubAccessResolver;
import semo.back.service.feature.clubfeature.biz.ClubFeatureService;
import semo.back.service.feature.notice.vo.ClubNoticeDetailResponse;
import semo.back.service.feature.notice.vo.ClubNoticeSummaryResponse;
import semo.back.service.feature.notice.vo.ClubNoticeUpsertResponse;
import semo.back.service.feature.notice.vo.UpsertClubNoticeRequest;
import semo.back.service.feature.share.biz.ClubContentShareService;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubNoticeService {
    private static final String FEATURE_NOTICE = "NOTICE";
    private static final String NOTICE_IMAGE_TARGET_DIR = "semo/notices";
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 30;
    private static final DateTimeFormatter DATE_TIME_REQUEST_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter DATE_TIME_LABEL_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");

    private final ClubNoticeRepository clubNoticeRepository;
    private final ClubRepository clubRepository;
    private final ClubProfileRepository clubProfileRepository;
    private final ClubAccessResolver clubAccessResolver;
    private final ClubFeatureService clubFeatureService;
    private final ClubNoticePermissionService clubNoticePermissionService;
    private final ImageFinalizeClient imageFinalizeClient;
    private final ImageFileUrlResolver imageFileUrlResolver;
    private final ClubContentShareService clubContentShareService;

    public ClubNoticeDetailResponse getNoticeDetail(Long clubId, Long noticeId, String userKey) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        Club club = getActiveClub(clubId);
        ClubNotice notice = getNotice(clubId, noticeId);
        ClubProfile authorProfile = clubProfileRepository.findById(notice.getAuthorClubProfileId())
                .orElseThrow(() -> new SemoException.ResourceNotFoundException("ClubProfile", "clubProfileId", notice.getAuthorClubProfileId()));
        ClubNoticePermissionService.NoticeActionPermission actionPermission = clubNoticePermissionService
                .getActionPermission(access, notice.getAuthorClubProfileId());

        return new ClubNoticeDetailResponse(
                club.getClubId(),
                club.getName(),
                isAdminRole(access.membership().getRoleCode()),
                notice.getNoticeId(),
                notice.getTitle(),
                notice.getContent(),
                notice.getImageFileName(),
                imageFileUrlResolver.resolveImageUrl(notice.getImageFileName()),
                imageFileUrlResolver.resolveThumbnailUrl(notice.getImageFileName()),
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
                notice.isSharedToBoard(),
                notice.isSharedToCalendar(),
                actionPermission.canManage(),
                actionPermission.canEdit(),
                actionPermission.canDelete(),
                null,
                null
        );
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "공지관리")
    public ClubNoticeUpsertResponse createNotice(Long clubId, String userKey, UpsertClubNoticeRequest request) {
        requireNoticeFeature(clubId);
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        requireCreatePermission(access);
        validateRequest(request);
        ClubActivityContextHolder.setDetails(
                "공지 '" + request.title().trim() + "'을 생성했습니다.",
                "공지 '" + request.title().trim() + "' 생성에 실패했습니다."
        );
        boolean postToBoard = resolvePostToBoard(request.postToBoard());
        boolean postToCalendar = resolvePostToCalendar(request.postToCalendar(), request.postToSchedule());
        ClubNotice notice = clubNoticeRepository.save(ClubNotice.builder()
                .clubId(clubId)
                .authorClubProfileId(access.clubProfile().getClubProfileId())
                .title(request.title().trim())
                .content(request.content().trim())
                .imageFileName(finalizeNoticeImageFileName(request.fileName(), null))
                .locationLabel(trimToNull(request.locationLabel()))
                .scheduleAt(postToCalendar ? parseOptionalDateTime(request.scheduleAt()) : null)
                .scheduleEndAt(postToCalendar ? parseOptionalDateTime(request.scheduleEndAt()) : null)
                .sharedToBoard(postToBoard)
                .sharedToCalendar(postToCalendar)
                .pinned(Boolean.TRUE.equals(request.pinned()))
                .publishedAt(LocalDateTime.now())
                .deleted(false)
                .build());
        syncNoticeShares(notice);
        return toUpsertResponse(notice);
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "공지관리")
    public ClubNoticeUpsertResponse updateNotice(Long clubId, Long noticeId, String userKey, UpsertClubNoticeRequest request) {
        requireNoticeFeature(clubId);
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        validateRequest(request);
        ClubNotice current = getNotice(clubId, noticeId);
        requireUpdatePermission(access, current.getAuthorClubProfileId());
        ClubActivityContextHolder.setDetails(
                "공지 '" + current.getTitle() + "'을 수정했습니다.",
                "공지 '" + current.getTitle() + "' 수정에 실패했습니다."
        );
        boolean postToBoard = resolvePostToBoard(request.postToBoard());
        boolean postToCalendar = resolvePostToCalendar(request.postToCalendar(), request.postToSchedule());
        ClubNotice updated = clubNoticeRepository.save(ClubNotice.builder()
                .noticeId(current.getNoticeId())
                .clubId(current.getClubId())
                .authorClubProfileId(current.getAuthorClubProfileId())
                .title(request.title().trim())
                .content(request.content().trim())
                .imageFileName(finalizeNoticeImageFileName(request.fileName(), current.getImageFileName()))
                .locationLabel(trimToNull(request.locationLabel()))
                .scheduleAt(postToCalendar ? parseOptionalDateTime(request.scheduleAt()) : null)
                .scheduleEndAt(postToCalendar ? parseOptionalDateTime(request.scheduleEndAt()) : null)
                .sharedToBoard(postToBoard)
                .sharedToCalendar(postToCalendar)
                .pinned(Boolean.TRUE.equals(request.pinned()))
                .publishedAt(current.getPublishedAt())
                .deleted(false)
                .build());
        syncNoticeShares(updated);
        return toUpsertResponse(updated);
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "공지관리")
    public void deleteNotice(Long clubId, Long noticeId, String userKey) {
        requireNoticeFeature(clubId);
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        ClubNotice current = getNotice(clubId, noticeId);
        requireDeletePermission(access, current.getAuthorClubProfileId());
        ClubActivityContextHolder.setDetails(
                "공지 '" + current.getTitle() + "'을 삭제했습니다.",
                "공지 '" + current.getTitle() + "' 삭제에 실패했습니다."
        );
        clubNoticeRepository.save(ClubNotice.builder()
                .noticeId(current.getNoticeId())
                .clubId(current.getClubId())
                .authorClubProfileId(current.getAuthorClubProfileId())
                .title(current.getTitle())
                .content(current.getContent())
                .imageFileName(current.getImageFileName())
                .locationLabel(current.getLocationLabel())
                .scheduleAt(current.getScheduleAt())
                .scheduleEndAt(current.getScheduleEndAt())
                .sharedToBoard(current.isSharedToBoard())
                .sharedToCalendar(current.isSharedToCalendar())
                .pinned(current.isPinned())
                .publishedAt(current.getPublishedAt())
                .deleted(true)
                .build());
        clubContentShareService.removeAllShares(clubId, ClubContentShareService.CONTENT_NOTICE, noticeId);
    }

    public List<ClubNotice> getScheduledNotices(Long clubId, LocalDateTime from, LocalDateTime to) {
        return clubNoticeRepository.findScheduledBetween(clubId, from, to);
    }

    public List<ClubNotice> getActiveNotices(Long clubId) {
        return clubNoticeRepository.findAllByClubIdAndDeletedFalseOrderByPublishedAtDescNoticeIdDesc(clubId);
    }

    public List<ClubNotice> getPinnedNotices(Long clubId) {
        return clubNoticeRepository.findAllByClubIdAndDeletedFalseAndPinnedTrueOrderByPublishedAtDescNoticeIdDesc(clubId);
    }

    public List<ClubNotice> getDirectNoticesByAuthor(Long clubId, Long authorClubProfileId) {
        return clubNoticeRepository.findDirectNoticesByClubIdAndAuthorClubProfileIdOrderByPublishedAtDescNoticeIdDesc(
                clubId,
                authorClubProfileId
        );
    }

    public List<ClubNotice> getDirectPinnedNoticesByAuthor(Long clubId, Long authorClubProfileId) {
        return clubNoticeRepository.findDirectPinnedNoticesByClubIdAndAuthorClubProfileIdOrderByPublishedAtDescNoticeIdDesc(
                clubId,
                authorClubProfileId
        );
    }

    public List<ClubNoticeSummaryResponse> toNoticeSummaries(
            ClubAccessResolver.ClubAccess access,
            List<ClubNotice> notices
    ) {
        Map<Long, ClubProfile> profileById = loadProfiles(notices);
        return notices.stream()
                .map(notice -> toSummaryResponse(
                        access,
                        notice,
                        profileById.get(notice.getAuthorClubProfileId())
                ))
                .toList();
    }

    private ClubNoticeUpsertResponse toUpsertResponse(ClubNotice notice) {
        return new ClubNoticeUpsertResponse(
                notice.getNoticeId(),
                notice.getTitle(),
                notice.getImageFileName(),
                imageFileUrlResolver.resolveImageUrl(notice.getImageFileName()),
                imageFileUrlResolver.resolveThumbnailUrl(notice.getImageFileName()),
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

    private ClubNoticeSummaryResponse toSummaryResponse(
            ClubAccessResolver.ClubAccess access,
            ClubNotice notice,
            ClubProfile authorProfile
    ) {
        String authorName = authorProfile == null ? "Unknown Member" : authorProfile.getDisplayName();
        ClubNoticePermissionService.NoticeActionPermission actionPermission = clubNoticePermissionService
                .getActionPermission(access, notice.getAuthorClubProfileId());
        return new ClubNoticeSummaryResponse(
                notice.getNoticeId(),
                notice.getTitle(),
                summarizeContent(notice.getContent()),
                notice.getImageFileName(),
                imageFileUrlResolver.resolveImageUrl(notice.getImageFileName()),
                imageFileUrlResolver.resolveThumbnailUrl(notice.getImageFileName()),
                authorName,
                null,
                authorProfile == null ? null : imageFileUrlResolver.resolveImageUrl(authorProfile.getAvatarFileName()),
                authorProfile == null ? null : imageFileUrlResolver.resolveThumbnailUrl(authorProfile.getAvatarFileName()),
                formatDateTime(notice.getPublishedAt()),
                toTimeAgo(notice.getPublishedAt()),
                notice.isPinned(),
                formatDateTimeValue(notice.getScheduleAt()),
                formatDateTimeValue(notice.getScheduleEndAt()),
                formatDateTime(notice.getScheduleAt()),
                notice.getLocationLabel(),
                notice.isSharedToBoard(),
                notice.isSharedToCalendar(),
                actionPermission.canManage(),
                actionPermission.canEdit(),
                actionPermission.canDelete(),
                null,
                null
        );
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
        if (request == null) {
            throw new SemoException.ValidationException("공지 요청이 비어 있습니다.");
        }
        if (!resolvePostToCalendar(request.postToCalendar(), request.postToSchedule())) {
            return;
        }
        LocalDateTime startAt = parseOptionalDateTime(request.scheduleAt());
        LocalDateTime endAt = parseOptionalDateTime(request.scheduleEndAt());
        if (startAt != null && endAt != null && endAt.isBefore(startAt)) {
            throw new SemoException.ValidationException("종료 시간은 시작 시간보다 빠를 수 없습니다.");
        }
    }

    private boolean resolvePostToBoard(Boolean postToBoard) {
        return postToBoard == null || postToBoard;
    }

    private boolean resolvePostToCalendar(Boolean postToCalendar, Boolean postToSchedule) {
        if (postToCalendar != null) {
            return postToCalendar;
        }
        if (postToSchedule != null) {
            return postToSchedule;
        }
        return false;
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

    private boolean isAdminRole(String roleCode) {
        return "OWNER".equals(roleCode) || "ADMIN".equals(roleCode);
    }

    public boolean canManageNotice(ClubAccessResolver.ClubAccess access, Long authorClubProfileId) {
        return clubNoticePermissionService.canManageNotice(access, authorClubProfileId);
    }

    public void requireNoticeFeature(Long clubId) {
        if (!isNoticeFeatureEnabled(clubId)) {
            throw new SemoException.ValidationException("공지 기능이 활성화되지 않았습니다.");
        }
    }

    private boolean isNoticeFeatureEnabled(Long clubId) {
        return clubFeatureService.isFeatureEnabled(clubId, FEATURE_NOTICE);
    }

    private String finalizeNoticeImageFileName(String requestFileName, String currentFileName) {
        String normalized = trimToNull(requestFileName);
        if (normalized == null) {
            return null;
        }
        if (normalized.equals(trimToNull(currentFileName))) {
            return currentFileName;
        }
        return imageFinalizeClient.finalizeImage(normalized, NOTICE_IMAGE_TARGET_DIR).fileName();
    }

    private void syncNoticeShares(ClubNotice notice) {
        clubContentShareService.syncBoardShare(
                notice.getClubId(),
                ClubContentShareService.CONTENT_NOTICE,
                notice.getNoticeId(),
                notice.isSharedToBoard() && !notice.isDeleted()
        );
        clubContentShareService.syncCalendarShare(
                notice.getClubId(),
                ClubContentShareService.CONTENT_NOTICE,
                notice.getNoticeId(),
                notice.isSharedToCalendar() && !notice.isDeleted()
        );
    }

    private void requireCreatePermission(ClubAccessResolver.ClubAccess access) {
        if (!clubNoticePermissionService.canCreateNotice(access)) {
            throw new SemoException.ForbiddenException("공지 작성 권한이 없습니다.");
        }
    }

    private void requireUpdatePermission(ClubAccessResolver.ClubAccess access, Long authorClubProfileId) {
        if (!clubNoticePermissionService.getActionPermission(access, authorClubProfileId).canEdit()) {
            throw new SemoException.ForbiddenException("공지 수정 또는 삭제 권한이 없습니다.");
        }
    }

    private void requireDeletePermission(ClubAccessResolver.ClubAccess access, Long authorClubProfileId) {
        if (!clubNoticePermissionService.getActionPermission(access, authorClubProfileId).canDelete()) {
            throw new SemoException.ForbiddenException("공지 수정 또는 삭제 권한이 없습니다.");
        }
    }
}
