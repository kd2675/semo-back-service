package semo.back.service.feature.club.biz;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import semo.back.service.common.exception.SemoException;
import semo.back.service.common.util.ImageFileUrlResolver;
import semo.back.service.common.util.ImageFinalizeClient;
import semo.back.service.database.pub.entity.Club;
import semo.back.service.database.pub.entity.ClubMember;
import semo.back.service.database.pub.entity.ClubNotice;
import semo.back.service.database.pub.entity.ClubProfile;
import semo.back.service.database.pub.entity.ClubScheduleEvent;
import semo.back.service.database.pub.repository.ClubNoticeBoardFeedRow;
import semo.back.service.database.pub.repository.ClubMemberRepository;
import semo.back.service.database.pub.repository.ClubNoticeRepository;
import semo.back.service.database.pub.repository.ClubProfileRepository;
import semo.back.service.database.pub.repository.ClubRepository;
import semo.back.service.database.pub.repository.ClubScheduleEventRepository;
import semo.back.service.feature.activity.biz.ClubActivityContextHolder;
import semo.back.service.feature.activity.biz.RecordClubActivity;
import semo.back.service.feature.club.vo.ClubCreateResponse;
import semo.back.service.feature.club.vo.ClubBoardNoticeResponse;
import semo.back.service.feature.club.vo.ClubBoardResponse;
import semo.back.service.feature.club.vo.ClubProfileDetailResponse;
import semo.back.service.feature.club.vo.ClubProfileRecordResponse;
import semo.back.service.feature.club.vo.ClubProfileResponse;
import semo.back.service.feature.club.vo.ClubScheduleDayEventsResponse;
import semo.back.service.feature.club.vo.ClubScheduleEventResponse;
import semo.back.service.feature.club.vo.ClubScheduleMonthResponse;
import semo.back.service.feature.club.vo.ClubScheduleResponse;
import semo.back.service.feature.club.vo.CreateClubRequest;
import semo.back.service.feature.club.vo.MyClubSummaryResponse;
import semo.back.service.feature.club.vo.UpdateClubProfileRequest;
import semo.back.service.feature.profile.biz.ProfileUserService;
import semo.back.service.feature.profile.vo.ProfileSummaryResponse;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubService {
    private static final Set<String> ALLOWED_CATEGORY_KEYS = Set.of(
            "TENNIS",
            "RUNNING",
            "CROSSFIT",
            "HIKING",
            "CYCLING",
            "OTHER"
    );
    private static final Set<String> ALLOWED_VISIBILITY_STATUSES = Set.of("PUBLIC", "PRIVATE");
    private static final Set<String> ALLOWED_MEMBERSHIP_POLICIES = Set.of("APPROVAL", "OPEN");
    private static final String ROLE_OWNER = "OWNER";
    private static final Set<String> ADMIN_ROLE_CODES = Set.of("OWNER", "ADMIN");
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String CLUB_IMAGE_TARGET_DIR = "semo/clubs";
    private static final String CLUB_PROFILE_IMAGE_TARGET_DIR = "semo/club-profiles";
    private static final DateTimeFormatter JOINED_LABEL_FORMATTER = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter SCHEDULE_MONTH_LABEL_FORMATTER = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter SCHEDULE_MONTH_SHORT_LABEL_FORMATTER = DateTimeFormatter.ofPattern("MMM", Locale.ENGLISH);
    private static final DateTimeFormatter SCHEDULE_TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);

    private final ClubRepository clubRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final ClubProfileRepository clubProfileRepository;
    private final ClubNoticeRepository clubNoticeRepository;
    private final ClubScheduleEventRepository clubScheduleEventRepository;
    private final ProfileUserService profileUserService;
    private final ImageFinalizeClient imageFinalizeClient;
    private final ImageFileUrlResolver imageFileUrlResolver;

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    public ClubCreateResponse createClub(String userKey, String userName, CreateClubRequest request) {
        validateRequest(request);

        Long profileId = profileUserService.resolveProfileId(userKey, userName);
        LocalDateTime now = LocalDateTime.now();
        String finalImageFileName = finalizeImageFileName(request.fileName());

        Club club = clubRepository.save(Club.builder()
                .name(request.name().trim())
                .summary(toSummary(request.description()))
                .description(trimToNull(request.description()))
                .categoryKey(normalizeCategoryKey(request.categoryKey()))
                .visibilityStatus(normalizeVisibilityStatus(request.visibilityStatus()))
                .membershipPolicy(normalizeMembershipPolicy(request.membershipPolicy()))
                .imageFileName(finalImageFileName)
                .active(true)
                .build());

        ClubMember membership = clubMemberRepository.save(ClubMember.builder()
                .clubId(club.getClubId())
                .profileId(profileId)
                .roleCode(ROLE_OWNER)
                .membershipStatus(STATUS_ACTIVE)
                .joinedAt(now)
                .lastActivityAt(now)
                .build());

        ensureClubProfile(membership, userName, null, null);

        return new ClubCreateResponse(
                club.getClubId(),
                club.getName(),
                club.getSummary(),
                club.getDescription(),
                club.getCategoryKey(),
                club.getVisibilityStatus(),
                club.getMembershipPolicy(),
                ROLE_OWNER,
                finalImageFileName,
                imageFileUrlResolver.resolveImageUrl(finalImageFileName),
                imageFileUrlResolver.resolveThumbnailUrl(finalImageFileName)
        );
    }

    public List<MyClubSummaryResponse> getMyClubs(String userKey) {
        Long profileId = profileUserService.getProfileIdByUserKey(userKey);
        List<ClubMember> memberships = clubMemberRepository.findActiveMemberships(profileId, STATUS_ACTIVE);
        if (memberships.isEmpty()) {
            return List.of();
        }

        List<Long> clubIds = memberships.stream()
                .map(ClubMember::getClubId)
                .distinct()
                .toList();
        Map<Long, Club> clubById = new HashMap<>();
        clubRepository.findByClubIdInAndActiveTrue(clubIds)
                .forEach(club -> clubById.put(club.getClubId(), club));

        return memberships.stream()
                .map(membership -> toMyClubSummary(membership, clubById.get(membership.getClubId())))
                .filter(response -> response != null)
                .toList();
    }

    public MyClubSummaryResponse getMyClub(Long clubId, String userKey) {
        MembershipClubPair pair = getMembershipClubPair(clubId, userKey);
        ClubMember membership = pair.membership();
        Club club = pair.club();
        return toMyClubSummary(membership, club);
    }

    public ClubBoardResponse getClubBoard(Long clubId, String userKey) {
        MembershipClubPair pair = getMembershipClubPair(clubId, userKey);
        List<ClubNoticeBoardFeedRow> rows = clubNoticeRepository.findBoardFeed(
                clubId,
                null,
                false,
                null,
                PageRequest.of(0, 5)
        );
        List<ClubNotice> notices = rows.stream()
                .map(ClubNoticeBoardFeedRow::notice)
                .toList();
        Map<Long, ClubProfile> profileById = clubProfileRepository.findAllById(
                notices.stream().map(ClubNotice::getAuthorClubProfileId).distinct().toList()
        ).stream().collect(HashMap::new, (map, profile) -> map.put(profile.getClubProfileId(), profile), HashMap::putAll);
        return new ClubBoardResponse(
                pair.club().getClubId(),
                pair.club().getName(),
                isAdminRole(pair.membership().getRoleCode()),
                notices.stream()
                        .map(notice -> new ClubBoardNoticeResponse(
                                String.valueOf(notice.getNoticeId()),
                                "campaign",
                                notice.getTitle(),
                                summarizeContent(notice.getContent()),
                                profileById.get(notice.getAuthorClubProfileId()) == null
                                        ? "Unknown Member"
                                        : profileById.get(notice.getAuthorClubProfileId()).getDisplayName(),
                                formatBoardTimeAgo(notice.getPublishedAt()),
                                "social"
                        ))
                        .toList()
        );
    }

    public ClubScheduleResponse getClubSchedule(Long clubId, String userKey) {
        MembershipClubPair pair = getMembershipClubPair(clubId, userKey);
        LocalDate now = LocalDate.now();
        List<ClubScheduleMonthResponse> months = List.of(
                toScheduleMonth(clubId, now.withDayOfMonth(1)),
                toScheduleMonth(clubId, now.plusMonths(1).withDayOfMonth(1))
        );

        return new ClubScheduleResponse(
                pair.club().getClubId(),
                pair.club().getName(),
                isAdminRole(pair.membership().getRoleCode()),
                months
        );
    }

    @Transactional(transactionManager = "pubTransactionManager")
    public ClubProfileResponse getClubProfile(Long clubId, String userKey) {
        MembershipClubPair pair = getMembershipClubPair(clubId, userKey);
        ProfileSummaryResponse appProfile = profileUserService.getProfileSummary(userKey);
        ClubMember membership = pair.membership();
        ClubProfile clubProfile = ensureClubProfile(
                membership,
                appProfile.displayName(),
                appProfile.tagline(),
                null
        );

        return new ClubProfileResponse(
                pair.club().getClubId(),
                pair.club().getName(),
                isAdminRole(membership.getRoleCode()),
                appProfile,
                new ClubProfileDetailResponse(
                        clubProfile.getClubProfileId(),
                        clubProfile.getDisplayName(),
                        clubProfile.getTagline(),
                        clubProfile.getIntroText(),
                        clubProfile.getAvatarFileName(),
                        imageFileUrlResolver.resolveImageUrl(clubProfile.getAvatarFileName()),
                        imageFileUrlResolver.resolveThumbnailUrl(clubProfile.getAvatarFileName()),
                        membership.getRoleCode(),
                        membership.getMembershipStatus(),
                        formatJoinedLabel(membership.getJoinedAt())
                ),
                List.of(
                        new ClubProfileRecordResponse("club-role", "Club Role", membership.getRoleCode(), "현재 클럽에서의 역할"),
                        new ClubProfileRecordResponse("club-status", "Membership", membership.getMembershipStatus(), "가입 상태"),
                        new ClubProfileRecordResponse("club-name", "Club Name", pair.club().getName(), "현재 활동 중인 모임"),
                        new ClubProfileRecordResponse("club-joined", "Joined", formatJoinedLabel(membership.getJoinedAt()), "클럽 가입 시점")
                )
        );
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    @RecordClubActivity(subject = "모임관리")
    public ClubProfileResponse updateClubProfile(Long clubId, String userKey, UpdateClubProfileRequest request) {
        MembershipClubPair pair = getMembershipClubPair(clubId, userKey);
        ProfileSummaryResponse appProfile = profileUserService.getProfileSummary(userKey);
        ClubMember membership = pair.membership();
        ClubProfile current = ensureClubProfile(
                membership,
                appProfile.displayName(),
                appProfile.tagline(),
                null
        );

        String displayName = resolveClubProfileDisplayName(current.getDisplayName(), request);
        String avatarFileName = resolveClubProfileAvatarFileName(current.getAvatarFileName(), request);
        ClubActivityContextHolder.setDetails(
                "모임 프로필을 수정했습니다.",
                "모임 프로필 수정에 실패했습니다."
        );
        clubProfileRepository.save(ClubProfile.builder()
                .clubProfileId(current.getClubProfileId())
                .clubMemberId(current.getClubMemberId())
                .displayName(displayName)
                .tagline(current.getTagline())
                .introText(current.getIntroText())
                .avatarFileName(avatarFileName)
                .build());

        return getClubProfile(clubId, userKey);
    }

    private void validateRequest(CreateClubRequest request) {
        if (request == null) {
            throw new SemoException.ValidationException("클럽 생성 요청이 비어 있습니다.");
        }
        if (!StringUtils.hasText(request.name())) {
            throw new SemoException.ValidationException("클럽 이름은 필수입니다.");
        }
        if (!ALLOWED_CATEGORY_KEYS.contains(normalizeCategoryKey(request.categoryKey()))) {
            throw new SemoException.ValidationException("지원하지 않는 클럽 카테고리입니다.");
        }
        if (!ALLOWED_VISIBILITY_STATUSES.contains(normalizeVisibilityStatus(request.visibilityStatus()))) {
            throw new SemoException.ValidationException("지원하지 않는 공개 범위입니다.");
        }
        if (!ALLOWED_MEMBERSHIP_POLICIES.contains(normalizeMembershipPolicy(request.membershipPolicy()))) {
            throw new SemoException.ValidationException("지원하지 않는 가입 방식입니다.");
        }
    }

    private String normalizeCategoryKey(String categoryKey) {
        if (!StringUtils.hasText(categoryKey)) {
            return "OTHER";
        }
        return categoryKey.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeVisibilityStatus(String visibilityStatus) {
        if (!StringUtils.hasText(visibilityStatus)) {
            return "PUBLIC";
        }
        return visibilityStatus.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeMembershipPolicy(String membershipPolicy) {
        if (!StringUtils.hasText(membershipPolicy)) {
            return "APPROVAL";
        }
        return membershipPolicy.trim().toUpperCase(Locale.ROOT);
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String toSummary(String description) {
        String normalized = trimToNull(description);
        if (normalized == null) {
            return null;
        }
        return normalized.length() <= 255 ? normalized : normalized.substring(0, 255);
    }

    private String finalizeImageFileName(String fileName) {
        String normalized = trimToNull(fileName);
        if (normalized == null) {
            return null;
        }
        return imageFinalizeClient.finalizeImage(normalized, CLUB_IMAGE_TARGET_DIR).fileName();
    }

    private String resolveClubProfileAvatarFileName(String currentFileName, UpdateClubProfileRequest request) {
        if (request == null) {
            return currentFileName;
        }
        if (Boolean.TRUE.equals(request.removeAvatar())) {
            return null;
        }
        String normalized = trimToNull(request.avatarFileName());
        if (normalized == null) {
            return currentFileName;
        }
        return imageFinalizeClient.finalizeImage(normalized, CLUB_PROFILE_IMAGE_TARGET_DIR).fileName();
    }

    private String resolveClubProfileDisplayName(String currentDisplayName, UpdateClubProfileRequest request) {
        if (request == null) {
            return currentDisplayName;
        }
        if (request.displayName() == null) {
            return currentDisplayName;
        }

        String normalized = trimToNull(request.displayName());
        if (normalized == null) {
            throw new SemoException.ValidationException("닉네임은 비워둘 수 없습니다.");
        }
        if (normalized.length() > 100) {
            throw new SemoException.ValidationException("닉네임은 100자 이하여야 합니다.");
        }
        return normalized;
    }

    private ClubProfile ensureClubProfile(
            ClubMember membership,
            String fallbackDisplayName,
            String fallbackTagline,
            String fallbackIntroText
    ) {
        return clubProfileRepository.findByClubMemberId(membership.getClubMemberId())
                .orElseGet(() -> clubProfileRepository.save(ClubProfile.builder()
                        .clubMemberId(membership.getClubMemberId())
                        .displayName(StringUtils.hasText(fallbackDisplayName) ? fallbackDisplayName.trim() : "SEMO Member")
                        .tagline(trimToNull(fallbackTagline))
                        .introText(trimToNull(fallbackIntroText))
                        .avatarFileName(null)
                        .build()));
    }

    private MyClubSummaryResponse toMyClubSummary(ClubMember membership, Club club) {
        if (club == null) {
            return null;
        }
        String roleCode = membership.getRoleCode();
        String fileName = club.getImageFileName();
        return new MyClubSummaryResponse(
                club.getClubId(),
                club.getName(),
                club.getSummary(),
                club.getDescription(),
                club.getCategoryKey(),
                roleCode,
                isAdminRole(roleCode),
                fileName,
                imageFileUrlResolver.resolveImageUrl(fileName),
                imageFileUrlResolver.resolveThumbnailUrl(fileName)
        );
    }

    private MembershipClubPair getMembershipClubPair(Long clubId, String userKey) {
        Long profileId = profileUserService.getProfileIdByUserKey(userKey);
        ClubMember membership = clubMemberRepository.findByClubIdAndProfileId(clubId, profileId)
                .filter(clubMember -> STATUS_ACTIVE.equals(clubMember.getMembershipStatus()))
                .orElseThrow(() -> new SemoException.ResourceNotFoundException("ClubMember", "clubId", clubId));
        Club club = clubRepository.findById(clubId)
                .filter(Club::isActive)
                .orElseThrow(() -> new SemoException.ResourceNotFoundException("Club", "clubId", clubId));
        return new MembershipClubPair(membership, club);
    }

    private boolean isAdminRole(String roleCode) {
        return ADMIN_ROLE_CODES.contains(roleCode);
    }

    private ClubScheduleMonthResponse toScheduleMonth(Long clubId, LocalDate month) {
        LocalDate firstDay = month.withDayOfMonth(1);
        LocalDate lastDay = firstDay.withDayOfMonth(firstDay.lengthOfMonth());
        LocalDate firstDayOfNextMonth = firstDay.plusMonths(1);
        int leadingBlankDays = firstDay.getDayOfWeek() == DayOfWeek.SUNDAY
                ? 0
                : firstDay.getDayOfWeek().getValue();

        Map<Integer, List<ClubScheduleEventResponse>> eventsByDay = new HashMap<>();
        clubScheduleEventRepository.findScheduledBetween(clubId, firstDay.atStartOfDay(), firstDayOfNextMonth.atStartOfDay())
                .forEach(event -> {
                    LocalDate startDate = event.getStartAt().toLocalDate().isBefore(firstDay)
                            ? firstDay
                            : event.getStartAt().toLocalDate();
                    LocalDate endDate = resolveScheduleEndDate(event, lastDay);

                    for (LocalDate current = startDate; !current.isAfter(endDate); current = current.plusDays(1)) {
                        int day = current.getDayOfMonth();
                        eventsByDay.computeIfAbsent(day, ignored -> new java.util.ArrayList<>())
                                .add(new ClubScheduleEventResponse(
                                        String.valueOf(event.getEventId()),
                                        toScheduleIcon(event.getCategoryKey()),
                                        event.getTitle(),
                                        event.getLocationLabel() != null ? event.getLocationLabel() : toSummary(event.getDescription()),
                                        event.getStartAt().format(SCHEDULE_TIME_FORMATTER),
                                        formatDurationLabel(event.getStartAt(), event.getEndAt()),
                                        toScheduleTone(event.getCategoryKey())
                                ));
                    }
                });

        List<ClubScheduleDayEventsResponse> days = eventsByDay.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new ClubScheduleDayEventsResponse(entry.getKey(), entry.getValue()))
                .toList();

        return new ClubScheduleMonthResponse(
                "%d-%02d".formatted(firstDay.getYear(), firstDay.getMonthValue()),
                firstDay.format(SCHEDULE_MONTH_LABEL_FORMATTER),
                firstDay.format(SCHEDULE_MONTH_SHORT_LABEL_FORMATTER),
                firstDay.getYear(),
                firstDay.getMonthValue(),
                leadingBlankDays,
                firstDay.lengthOfMonth(),
                month.equals(LocalDate.now().withDayOfMonth(1)) ? LocalDate.now().getDayOfMonth() : 1,
                days
        );
    }

    private LocalDate resolveScheduleEndDate(ClubScheduleEvent event, LocalDate monthLastDay) {
        if (event.getEndAt() == null) {
            return event.getStartAt().toLocalDate().isAfter(monthLastDay)
                    ? monthLastDay
                    : event.getStartAt().toLocalDate();
        }

        LocalDate endDate = event.getEndAt().toLocalDate();
        if (endDate.isAfter(monthLastDay)) {
            return monthLastDay;
        }
        return endDate;
    }

    private String formatDurationLabel(LocalDateTime startAt, LocalDateTime endAt) {
        if (startAt == null || endAt == null || !endAt.isAfter(startAt)) {
            return null;
        }
        long minutes = java.time.Duration.between(startAt, endAt).toMinutes();
        return minutes + " Min";
    }

    private String summarizeContent(String content) {
        String normalized = trimToNull(content);
        if (normalized == null) {
            return "";
        }
        return normalized.length() <= 140 ? normalized : normalized.substring(0, 140) + "...";
    }

    private String toScheduleIcon(String categoryKey) {
        return switch (categoryKey) {
            case "TOURNAMENT", "MATCH" -> "sports_tennis";
            case "SOCIAL" -> "groups";
            default -> "calendar_month";
        };
    }

    private String toScheduleTone(String categoryKey) {
        return switch (categoryKey) {
            case "MATCH" -> "amber";
            case "SOCIAL" -> "slate";
            default -> "primary";
        };
    }

    private String formatBoardTimeAgo(LocalDateTime value) {
        if (value == null) {
            return "";
        }
        java.time.Duration duration = java.time.Duration.between(value, LocalDateTime.now());
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
        return days + "d ago";
    }

    private String formatJoinedLabel(LocalDateTime joinedAt) {
        if (joinedAt == null) {
            return "Joined recently";
        }
        return "Joined " + joinedAt.format(JOINED_LABEL_FORMATTER);
    }

    private record MembershipClubPair(ClubMember membership, Club club) {
    }
}
