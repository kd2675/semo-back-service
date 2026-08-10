package semo.back.service.feature.feedback.biz;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import semo.back.service.common.exception.SemoException;
import semo.back.service.database.pub.entity.ClubFeedback;
import semo.back.service.database.pub.entity.ClubProfile;
import semo.back.service.database.pub.repository.ClubFeedbackRepository;
import semo.back.service.database.pub.repository.ClubProfileRepository;
import semo.back.service.feature.activity.biz.ClubActivityContextHolder;
import semo.back.service.feature.activity.biz.RecordClubActivity;
import semo.back.service.feature.club.biz.policy.ClubAccessResolver;
import semo.back.service.feature.clubfeature.biz.ClubFeatureService;
import semo.back.service.feature.feedback.vo.ClubAdminFeedbackResponse;
import semo.back.service.feature.feedback.vo.ClubFeedbackDetailResponse;
import semo.back.service.feature.feedback.vo.ClubFeedbackHomeResponse;
import semo.back.service.feature.feedback.vo.ClubFeedbackSummaryResponse;
import semo.back.service.feature.feedback.vo.CreateClubFeedbackRequest;
import semo.back.service.feature.feedback.vo.UpdateClubAdminFeedbackRequest;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubFeedbackService {
    private static final String FEATURE_FEEDBACK = "FEEDBACK";
    private static final String VISIBILITY_PRIVATE = "PRIVATE";
    private static final String VISIBILITY_PUBLIC = "PUBLIC";
    private static final String STATUS_RECEIVED = "RECEIVED";
    private static final String STATUS_IN_REVIEW = "IN_REVIEW";
    private static final String STATUS_ANSWERED = "ANSWERED";
    private static final String STATUS_CLOSED = "CLOSED";
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "SUGGESTION",
            "INCONVENIENCE",
            "IMPROVEMENT_REQUEST"
    );
    private static final Set<String> ALLOWED_STATUSES = Set.of(
            STATUS_RECEIVED,
            STATUS_IN_REVIEW,
            STATUS_ANSWERED,
            STATUS_CLOSED
    );
    private static final Set<String> ALLOWED_VISIBILITY_SCOPES = Set.of(
            VISIBILITY_PRIVATE,
            VISIBILITY_PUBLIC
    );
    private static final int CONTENT_PREVIEW_LENGTH = 120;
    private static final DateTimeFormatter DATE_TIME_REQUEST_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter DATE_TIME_LABEL_FORMATTER = DateTimeFormatter.ofPattern(
            "yyyy.MM.dd HH:mm",
            Locale.KOREAN
    );

    private final ClubAccessResolver clubAccessResolver;
    private final ClubFeatureService clubFeatureService;
    private final ClubFeedbackRepository clubFeedbackRepository;
    private final ClubProfileRepository clubProfileRepository;

    public ClubFeedbackHomeResponse getFeedbackHome(Long clubId, String userKey) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        requireFeedbackFeature(clubId);
        List<ClubFeedback> visibleItems = getVisibleFeedback(access);
        Map<Long, ClubProfile> profileById = getProfilesById(visibleItems);

        return new ClubFeedbackHomeResponse(
                access.club().getClubId(),
                access.club().getName(),
                access.isAdmin(),
                true,
                visibleItems.size(),
                (int) visibleItems.stream().filter(item -> isOwner(item, access)).count(),
                (int) visibleItems.stream().filter(item -> VISIBILITY_PUBLIC.equals(item.getVisibilityScope())).count(),
                (int) visibleItems.stream().filter(item -> STATUS_ANSWERED.equals(item.getStatusCode())).count(),
                visibleItems.stream()
                        .map(item -> toSummaryResponse(item, access, profileById, false))
                        .toList()
        );
    }

    public ClubFeedbackDetailResponse getFeedbackDetail(Long clubId, Long feedbackId, String userKey) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        requireFeedbackFeature(clubId);
        ClubFeedback feedback = getFeedback(clubId, feedbackId);
        requireVisibleToMember(feedback, access);
        Map<Long, ClubProfile> profileById = getProfilesById(List.of(feedback));
        return toDetailResponse(feedback, access, profileById, false);
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "피드백")
    public ClubFeedbackDetailResponse createFeedback(Long clubId, String userKey, CreateClubFeedbackRequest request) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(clubId, userKey);
        requireFeedbackFeature(clubId);
        String feedbackType = normalizeFeedbackType(request == null ? null : request.feedbackType());
        String title = requireText(request == null ? null : request.title(), 200, "제목");
        String content = requireText(request == null ? null : request.content(), 2000, "내용");
        boolean anonymous = Boolean.TRUE.equals(request == null ? null : request.anonymous());

        ClubActivityContextHolder.setDetails(
                anonymous ? "비공개 익명 피드백을 등록했습니다." : "비공개 피드백을 등록했습니다.",
                "비공개 피드백을 등록하지 못했습니다."
        );

        ClubFeedback saved = clubFeedbackRepository.save(ClubFeedback.builder()
                .clubId(clubId)
                .submitterClubProfileId(access.clubProfile().getClubProfileId())
                .feedbackType(feedbackType)
                .visibilityScope(VISIBILITY_PRIVATE)
                .statusCode(STATUS_RECEIVED)
                .anonymous(anonymous)
                .title(title)
                .content(content)
                .adminAnswer(null)
                .answeredByClubProfileId(null)
                .answeredAt(null)
                .deleted(false)
                .build());

        return toDetailResponse(
                saved,
                access,
                getProfilesById(List.of(saved)),
                false
        );
    }

    public ClubAdminFeedbackResponse getAdminFeedback(Long clubId, String userKey) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireAdmin(clubId, userKey);
        requireFeedbackFeature(clubId);
        List<ClubFeedback> items = clubFeedbackRepository.findFeed(clubId);
        Map<Long, ClubProfile> profileById = getProfilesById(items);

        return new ClubAdminFeedbackResponse(
                access.club().getClubId(),
                access.club().getName(),
                true,
                true,
                items.size(),
                countByStatus(items, STATUS_RECEIVED),
                countByStatus(items, STATUS_IN_REVIEW),
                countByStatus(items, STATUS_ANSWERED),
                countByStatus(items, STATUS_CLOSED),
                (int) items.stream().filter(item -> VISIBILITY_PRIVATE.equals(item.getVisibilityScope())).count(),
                (int) items.stream().filter(item -> VISIBILITY_PUBLIC.equals(item.getVisibilityScope())).count(),
                items.stream()
                        .map(item -> toSummaryResponse(item, access, profileById, true))
                        .toList()
        );
    }

    public ClubFeedbackDetailResponse getAdminFeedbackDetail(Long clubId, Long feedbackId, String userKey) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireAdmin(clubId, userKey);
        requireFeedbackFeature(clubId);
        ClubFeedback feedback = getFeedback(clubId, feedbackId);
        return toDetailResponse(feedback, access, getProfilesById(List.of(feedback)), true);
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "피드백관리")
    public ClubFeedbackDetailResponse updateAdminFeedback(
            Long clubId,
            Long feedbackId,
            String userKey,
            UpdateClubAdminFeedbackRequest request
    ) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireAdmin(clubId, userKey);
        requireFeedbackFeature(clubId);
        ClubFeedback current = getFeedback(clubId, feedbackId);
        String feedbackType = normalizeFeedbackType(request == null ? null : request.feedbackType());
        String statusCode = normalizeStatusCode(request == null ? null : request.statusCode());
        String visibilityScope = normalizeVisibilityScope(request == null ? null : request.visibilityScope());
        String adminAnswer = trimToNull(request == null ? null : request.adminAnswer());
        if (VISIBILITY_PRIVATE.equals(current.getVisibilityScope()) && VISIBILITY_PUBLIC.equals(visibilityScope)) {
            throw new SemoException.ValidationException("제출자의 동의 없이 비공개 피드백을 공개할 수 없습니다.");
        }
        if (STATUS_ANSWERED.equals(statusCode) && !StringUtils.hasText(adminAnswer)) {
            throw new SemoException.ValidationException("답변 완료 상태로 저장하려면 답변을 입력해주세요.");
        }

        LocalDateTime now = LocalDateTime.now();
        ClubActivityContextHolder.setDetails(
                "피드백 '" + current.getTitle() + "' 상태를 " + toStatusLabel(statusCode) + "로 저장했습니다.",
                "피드백 '" + current.getTitle() + "' 상태를 저장하지 못했습니다."
        );

        ClubFeedback updated = clubFeedbackRepository.save(ClubFeedback.builder()
                .feedbackId(current.getFeedbackId())
                .clubId(current.getClubId())
                .submitterClubProfileId(current.getSubmitterClubProfileId())
                .feedbackType(feedbackType)
                .visibilityScope(visibilityScope)
                .statusCode(statusCode)
                .anonymous(current.isAnonymous())
                .title(current.getTitle())
                .content(current.getContent())
                .adminAnswer(adminAnswer)
                .answeredByClubProfileId(StringUtils.hasText(adminAnswer) ? access.clubProfile().getClubProfileId() : null)
                .answeredAt(StringUtils.hasText(adminAnswer) ? now : null)
                .deleted(false)
                .build());

        return toDetailResponse(
                updated,
                access,
                getProfilesById(List.of(updated)),
                true
        );
    }

    private List<ClubFeedback> getVisibleFeedback(ClubAccessResolver.ClubAccess access) {
        return clubFeedbackRepository.findFeed(access.club().getClubId()).stream()
                .filter(item -> canMemberView(item, access))
                .toList();
    }

    private boolean canMemberView(ClubFeedback feedback, ClubAccessResolver.ClubAccess access) {
        return access.isAdmin()
                || isOwner(feedback, access)
                || VISIBILITY_PUBLIC.equals(feedback.getVisibilityScope());
    }

    private void requireVisibleToMember(ClubFeedback feedback, ClubAccessResolver.ClubAccess access) {
        if (!canMemberView(feedback, access)) {
            throw new SemoException.ForbiddenException("해당 피드백을 조회할 권한이 없습니다.");
        }
    }

    private boolean isOwner(ClubFeedback feedback, ClubAccessResolver.ClubAccess access) {
        return feedback.getSubmitterClubProfileId().equals(access.clubProfile().getClubProfileId());
    }

    private ClubFeedback getFeedback(Long clubId, Long feedbackId) {
        return clubFeedbackRepository.findByFeedbackIdAndClubIdAndDeletedFalse(feedbackId, clubId)
                .orElseThrow(() -> new SemoException.ResourceNotFoundException("ClubFeedback", "feedbackId", feedbackId));
    }

    private Map<Long, ClubProfile> getProfilesById(List<ClubFeedback> feedbacks) {
        List<Long> clubProfileIds = feedbacks.stream()
                .flatMap(item -> java.util.stream.Stream.of(
                        item.getSubmitterClubProfileId(),
                        item.getAnsweredByClubProfileId()
                ))
                .filter(id -> id != null)
                .distinct()
                .toList();
        if (clubProfileIds.isEmpty()) {
            return Map.of();
        }
        return clubProfileRepository.findAllById(clubProfileIds).stream()
                .collect(Collectors.toMap(ClubProfile::getClubProfileId, Function.identity()));
    }

    private ClubFeedbackSummaryResponse toSummaryResponse(
            ClubFeedback feedback,
            ClubAccessResolver.ClubAccess access,
            Map<Long, ClubProfile> profileById,
            boolean adminView
    ) {
        return new ClubFeedbackSummaryResponse(
                feedback.getFeedbackId(),
                feedback.getFeedbackType(),
                toFeedbackTypeLabel(feedback.getFeedbackType()),
                feedback.getVisibilityScope(),
                toVisibilityLabel(feedback.getVisibilityScope()),
                feedback.getStatusCode(),
                toStatusLabel(feedback.getStatusCode()),
                feedback.getTitle(),
                abbreviate(feedback.getContent(), CONTENT_PREVIEW_LENGTH),
                feedback.isAnonymous(),
                resolveAuthorDisplayName(feedback, access, profileById, adminView),
                isOwner(feedback, access),
                StringUtils.hasText(feedback.getAdminAnswer()),
                formatDateTimeValue(feedback.getCreatedAt()),
                formatDateTimeLabel(feedback.getCreatedAt()),
                formatDateTimeValue(feedback.getAnsweredAt()),
                formatDateTimeLabel(feedback.getAnsweredAt())
        );
    }

    private ClubFeedbackDetailResponse toDetailResponse(
            ClubFeedback feedback,
            ClubAccessResolver.ClubAccess access,
            Map<Long, ClubProfile> profileById,
            boolean adminView
    ) {
        ClubProfile answeredByProfile = feedback.getAnsweredByClubProfileId() == null
                ? null
                : profileById.get(feedback.getAnsweredByClubProfileId());
        return new ClubFeedbackDetailResponse(
                access.club().getClubId(),
                access.club().getName(),
                access.isAdmin(),
                true,
                feedback.getFeedbackId(),
                feedback.getFeedbackType(),
                toFeedbackTypeLabel(feedback.getFeedbackType()),
                feedback.getVisibilityScope(),
                toVisibilityLabel(feedback.getVisibilityScope()),
                feedback.getStatusCode(),
                toStatusLabel(feedback.getStatusCode()),
                feedback.getTitle(),
                feedback.getContent(),
                feedback.isAnonymous(),
                resolveAuthorDisplayName(feedback, access, profileById, adminView),
                isOwner(feedback, access),
                adminView,
                feedback.getAdminAnswer(),
                answeredByProfile == null ? null : answeredByProfile.getDisplayName(),
                formatDateTimeValue(feedback.getCreatedAt()),
                formatDateTimeLabel(feedback.getCreatedAt()),
                formatDateTimeValue(feedback.getUpdatedAt()),
                formatDateTimeLabel(feedback.getUpdatedAt()),
                formatDateTimeValue(feedback.getAnsweredAt()),
                formatDateTimeLabel(feedback.getAnsweredAt())
        );
    }

    private String resolveAuthorDisplayName(
            ClubFeedback feedback,
            ClubAccessResolver.ClubAccess access,
            Map<Long, ClubProfile> profileById,
            boolean adminView
    ) {
        if (feedback.isAnonymous()) {
            return "익명";
        }
        ClubProfile profile = profileById.get(feedback.getSubmitterClubProfileId());
        String actualName = profile == null ? "알 수 없는 회원" : profile.getDisplayName();
        return actualName;
    }

    private int countByStatus(List<ClubFeedback> items, String statusCode) {
        return (int) items.stream()
                .filter(item -> statusCode.equals(item.getStatusCode()))
                .count();
    }

    private void requireFeedbackFeature(Long clubId) {
        if (!clubFeatureService.isFeatureEnabled(clubId, FEATURE_FEEDBACK)) {
            throw new SemoException.ForbiddenException("피드백 기능이 활성화되지 않았습니다.");
        }
    }

    private String normalizeFeedbackType(String feedbackType) {
        String normalized = normalizeUpper(feedbackType);
        if (!ALLOWED_TYPES.contains(normalized)) {
            throw new SemoException.ValidationException("지원하지 않는 피드백 분류입니다.");
        }
        return normalized;
    }

    private String normalizeStatusCode(String statusCode) {
        String normalized = normalizeUpper(statusCode);
        if (!ALLOWED_STATUSES.contains(normalized)) {
            throw new SemoException.ValidationException("지원하지 않는 처리 상태입니다.");
        }
        return normalized;
    }

    private String normalizeVisibilityScope(String visibilityScope) {
        String normalized = normalizeUpper(visibilityScope);
        if (!ALLOWED_VISIBILITY_SCOPES.contains(normalized)) {
            throw new SemoException.ValidationException("지원하지 않는 공개 범위입니다.");
        }
        return normalized;
    }

    private String normalizeUpper(String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw new SemoException.ValidationException("필수 값이 비어 있습니다.");
        }
        return trimmed.toUpperCase(Locale.ROOT);
    }

    private String requireText(String value, int maxLength, String label) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw new SemoException.ValidationException(label + "은(는) 필수입니다.");
        }
        if (trimmed.length() > maxLength) {
            throw new SemoException.ValidationException(label + " 길이를 확인해주세요.");
        }
        return trimmed;
    }

    private String abbreviate(String value, int maxLength) {
        String trimmed = trimToNull(value);
        if (trimmed == null || trimmed.length() <= maxLength) {
            return trimmed;
        }
        return trimmed.substring(0, maxLength - 1) + "…";
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String toFeedbackTypeLabel(String feedbackType) {
        return switch (feedbackType) {
            case "SUGGESTION" -> "건의";
            case "INCONVENIENCE" -> "불편 신고";
            case "IMPROVEMENT_REQUEST" -> "개선 요청";
            default -> feedbackType;
        };
    }

    private String toVisibilityLabel(String visibilityScope) {
        return switch (visibilityScope) {
            case VISIBILITY_PRIVATE -> "비공개";
            case VISIBILITY_PUBLIC -> "공개";
            default -> visibilityScope;
        };
    }

    private String toStatusLabel(String statusCode) {
        return switch (statusCode) {
            case STATUS_RECEIVED -> "접수";
            case STATUS_IN_REVIEW -> "검토 중";
            case STATUS_ANSWERED -> "답변 완료";
            case STATUS_CLOSED -> "종료";
            default -> statusCode;
        };
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
}
