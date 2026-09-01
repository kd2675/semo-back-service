package semo.back.service.feature.club.biz;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import semo.back.service.common.exception.SemoException;
import semo.back.service.common.util.ImageFileUrlResolver;
import semo.back.service.database.pub.entity.Club;
import semo.back.service.database.pub.entity.ClubActivityTag;
import semo.back.service.database.pub.entity.ClubJoinRequest;
import semo.back.service.database.pub.entity.ClubMember;
import semo.back.service.database.pub.entity.ClubProfile;
import semo.back.service.database.pub.entity.ProfileUser;
import semo.back.service.database.pub.repository.ClubJoinRequestRepository;
import semo.back.service.database.pub.repository.ClubActivityTagRepository;
import semo.back.service.database.pub.repository.ClubMemberRepository;
import semo.back.service.database.pub.repository.ClubProfileRepository;
import semo.back.service.database.pub.repository.ClubRepository;
import semo.back.service.database.pub.repository.ProfileUserRepository;
import semo.back.service.feature.activity.biz.ClubActivityContextHolder;
import semo.back.service.feature.activity.biz.RecordClubActivity;
import semo.back.service.feature.club.biz.policy.ClubAccessResolver;
import semo.back.service.feature.club.biz.support.ClubProfileProvisioner;
import semo.back.service.feature.club.biz.support.ClubClassificationSupport;
import semo.back.service.feature.clubfeature.biz.ClubFeatureService;
import semo.back.service.feature.club.vo.ClubDiscoverResponse;
import semo.back.service.feature.club.vo.ClubDiscoverSummaryResponse;
import semo.back.service.feature.club.vo.ClubJoinActionResponse;
import semo.back.service.feature.club.vo.ClubJoinRequestInboxItemResponse;
import semo.back.service.feature.club.vo.ClubJoinRequestInboxResponse;
import semo.back.service.feature.club.vo.ReviewClubJoinRequestRequest;
import semo.back.service.feature.club.vo.SubmitClubJoinRequestRequest;
import semo.back.service.feature.growth.biz.ClubGrowthCoreQueryService;
import semo.back.service.feature.growth.vo.ClubGrowthCoreResponse;
import semo.back.service.feature.notification.biz.ClubNotificationPublisher;
import semo.back.service.feature.notification.biz.ClubNotificationPublisher.NotificationCommand;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubJoinRequestService {
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_REJECTED = "REJECTED";
    private static final String STATUS_CANCELED = "CANCELED";
    private static final String VISIBILITY_PUBLIC = "PUBLIC";
    private static final String MEMBERSHIP_OPEN = "OPEN";
    private static final String MEMBERSHIP_APPROVAL = "APPROVAL";
    private static final String ROLE_MEMBER = "MEMBER";
    private static final int DISCOVER_LIMIT = 12;
    private static final DateTimeFormatter REQUESTED_LABEL_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm", Locale.KOREAN);

    private final ClubRepository clubRepository;
    private final ClubActivityTagRepository clubActivityTagRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final ClubProfileRepository clubProfileRepository;
    private final ClubProfileProvisioner clubProfileProvisioner;
    private final ClubJoinRequestRepository clubJoinRequestRepository;
    private final ProfileUserRepository profileUserRepository;
    private final ImageFileUrlResolver imageFileUrlResolver;
    private final ClubClassificationSupport clubClassificationSupport;
    private final ClubAccessResolver clubAccessResolver;
    private final ClubFeatureService clubFeatureService;
    private final ClubNotificationPublisher clubNotificationPublisher;
    private final ClubGrowthCoreQueryService clubGrowthCoreQueryService;

    public ClubDiscoverResponse getDiscoverClubs(String userKey, String query) {
        ProfileUser profileUser = requireProfileUser(userKey);
        List<ClubMember> memberships = clubMemberRepository.findByProfileId(profileUser.getProfileId());
        Set<Long> memberClubIds = memberships.stream()
                .map(ClubMember::getClubId)
                .collect(Collectors.toSet());

        List<Club> myClubs = memberClubIds.isEmpty()
                ? List.of()
                : clubRepository.findByClubIdInAndActiveTrue(memberClubIds);
        Map<Long, List<String>> myActivityTagsByClubId = toActivityTagsByClubId(clubActivityTagRepository.findByClubIdIn(memberClubIds));
        Set<String> preferredCategoryKeys = myClubs.stream()
                .map(club -> clubClassificationSupport.resolveStored(
                        club.getActivityCategory(),
                        myActivityTagsByClubId.getOrDefault(club.getClubId(), List.of()),
                        club.getAffiliationType(),
                        club.getCategoryKey()
                ))
                .map(ClubClassificationSupport.ResolvedClubClassification::activityCategory)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
        Set<String> preferredActivityTags = myActivityTagsByClubId.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toSet());

        String normalizedQuery = normalizeQuery(query);
        List<Club> publicClubs = clubRepository.searchPublicActiveClubs(normalizedQuery).stream()
                .filter(club -> !memberClubIds.contains(club.getClubId()))
                .toList();
        if (publicClubs.isEmpty()) {
            return new ClubDiscoverResponse(normalizedQuery, !StringUtils.hasText(normalizedQuery), recommendationLabel(preferredActivityTags, preferredCategoryKeys), 0, List.of());
        }

        List<Long> clubIds = publicClubs.stream().map(Club::getClubId).toList();
        Map<Long, List<String>> publicActivityTagsByClubId = toActivityTagsByClubId(clubActivityTagRepository.findByClubIdIn(clubIds));
        Map<Long, ClubJoinRequest> joinRequestByClubId = clubJoinRequestRepository.findByProfileIdAndClubIdIn(profileUser.getProfileId(), clubIds).stream()
                .collect(Collectors.toMap(ClubJoinRequest::getClubId, Function.identity()));
        Map<Long, ClubGrowthCoreResponse> growthCoreByClubId = clubGrowthCoreQueryService.getByClubIds(clubIds);

        Comparator<Club> comparator = buildDiscoverComparator(normalizedQuery, preferredCategoryKeys, preferredActivityTags, publicActivityTagsByClubId, joinRequestByClubId);
        List<ClubDiscoverSummaryResponse> clubs = publicClubs.stream()
                .sorted(comparator)
                .limit(DISCOVER_LIMIT)
                .map(club -> toDiscoverSummary(
                        club,
                        publicActivityTagsByClubId.getOrDefault(club.getClubId(), List.of()),
                        growthCoreByClubId.get(club.getClubId()).memberCount(),
                        joinRequestByClubId.get(club.getClubId()),
                        preferredCategoryKeys.contains(clubClassificationSupport.resolveStored(
                                club.getActivityCategory(),
                                publicActivityTagsByClubId.getOrDefault(club.getClubId(), List.of()),
                                club.getAffiliationType(),
                                club.getCategoryKey()
                        ).activityCategory()),
                        hasMatchingTags(preferredActivityTags, publicActivityTagsByClubId.getOrDefault(club.getClubId(), List.of())),
                        growthCoreByClubId.get(club.getClubId())
                ))
                .toList();

        return new ClubDiscoverResponse(
                normalizedQuery,
                !StringUtils.hasText(normalizedQuery),
                recommendationLabel(preferredActivityTags, preferredCategoryKeys),
                clubs.size(),
                clubs
        );
    }

    @Transactional(transactionManager = "pubTransactionManager")
    public ClubJoinActionResponse submitJoinRequest(Long clubId, String userKey, SubmitClubJoinRequestRequest request) {
        ProfileUser profileUser = requireProfileUser(userKey);
        Club club = requireJoinableClub(clubId);
        validateNoExistingMembership(club, profileUser.getProfileId());

        ClubJoinRequest existingRequest = clubJoinRequestRepository.findByClubIdAndProfileId(clubId, profileUser.getProfileId())
                .orElse(null);
        if (MEMBERSHIP_OPEN.equals(club.getMembershipPolicy())) {
            ClubMember membership = createMember(club, profileUser);
            if (existingRequest != null && !STATUS_APPROVED.equals(existingRequest.getRequestStatus())) {
                existingRequest.approve(profileUser.getProfileId(), LocalDateTime.now());
            }
            return new ClubJoinActionResponse(
                    club.getClubId(),
                    club.getName(),
                    "JOINED",
                    STATUS_ACTIVE,
                    existingRequest == null ? null : existingRequest.getClubJoinRequestId(),
                    membership.getClubMemberId()
            );
        }

        clubFeatureService.requireFeatureEnabled(clubId, "JOIN_REQUEST", "가입 신청");

        String requestMessage = trimToNull(request == null ? null : request.requestMessage());
        ClubJoinRequest joinRequest;
        if (existingRequest == null) {
            joinRequest = clubJoinRequestRepository.save(ClubJoinRequest.builder()
                    .clubId(club.getClubId())
                    .profileId(profileUser.getProfileId())
                    .requestMessage(requestMessage)
                    .requestStatus(STATUS_PENDING)
                    .build());
        } else if (STATUS_PENDING.equals(existingRequest.getRequestStatus())) {
            throw new SemoException.ConflictException("이미 가입 신청이 접수되어 있습니다.");
        } else {
            existingRequest.resubmit(requestMessage);
            joinRequest = existingRequest;
        }

        return new ClubJoinActionResponse(
                club.getClubId(),
                club.getName(),
                "REQUESTED",
                STATUS_PENDING,
                joinRequest.getClubJoinRequestId(),
                null
        );
    }

    @Transactional(transactionManager = "pubTransactionManager")
    public ClubJoinActionResponse cancelMyJoinRequest(Long clubId, String userKey) {
        ProfileUser profileUser = requireProfileUser(userKey);
        Club club = requireJoinableClub(clubId);
        ClubJoinRequest joinRequest = clubJoinRequestRepository.findByClubIdAndProfileId(clubId, profileUser.getProfileId())
                .filter(request -> STATUS_PENDING.equals(request.getRequestStatus()))
                .orElseThrow(() -> new SemoException.ResourceNotFoundException("ClubJoinRequest", "clubId", clubId));
        joinRequest.cancel(LocalDateTime.now());
        return new ClubJoinActionResponse(
                club.getClubId(),
                club.getName(),
                "CANCELED",
                STATUS_CANCELED,
                joinRequest.getClubJoinRequestId(),
                null
        );
    }

    public ClubJoinRequestInboxResponse getAdminJoinRequestInbox(Long clubId, String userKey) {
        clubFeatureService.requireFeatureEnabled(clubId, "JOIN_REQUEST", "가입 신청");
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireAdmin(clubId, userKey);
        return buildJoinRequestInboxResponse(access);
    }

    @Transactional(transactionManager = "pubTransactionManager")
    @RecordClubActivity(subject = "가입신청")
    public ClubJoinActionResponse reviewJoinRequest(
            Long clubId,
            Long clubJoinRequestId,
            String userKey,
            ReviewClubJoinRequestRequest request
    ) {
        clubFeatureService.requireFeatureEnabled(clubId, "JOIN_REQUEST", "가입 신청");
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireAdmin(clubId, userKey);
        ClubJoinRequest joinRequest = clubJoinRequestRepository.findForUpdate(clubJoinRequestId, clubId)
                .orElseThrow(() -> new SemoException.ResourceNotFoundException("ClubJoinRequest", "clubJoinRequestId", clubJoinRequestId));
        if (!STATUS_PENDING.equals(joinRequest.getRequestStatus())) {
            throw new SemoException.ValidationException("대기 중인 가입 신청만 검토할 수 있습니다.");
        }

        String normalizedStatus = normalizeReviewStatus(request.requestStatus());
        LocalDateTime now = LocalDateTime.now();
        if (STATUS_APPROVED.equals(normalizedStatus)) {
            Club club = access.club();
            validateNoExistingMembership(club, joinRequest.getProfileId());
            ProfileUser applicant = profileUserRepository.findById(joinRequest.getProfileId())
                    .orElseThrow(() -> new SemoException.ResourceNotFoundException("ProfileUser", "profileId", joinRequest.getProfileId()));
            ClubMember membership = createMember(club, applicant);
            joinRequest.approve(access.profileUser().getProfileId(), now);
            clubNotificationPublisher.notifyProfile(
                    applicant.getProfileId(),
                    new NotificationCommand(
                            clubId,
                            "JOIN_REQUEST_REVIEW",
                            "가입 신청이 승인되었습니다",
                            "'" + club.getName() + "' 가입이 승인되었습니다.",
                            "JOIN_REQUEST",
                            joinRequest.getClubJoinRequestId(),
                            "/clubs/" + clubId,
                            "join-request:" + joinRequest.getClubJoinRequestId() + ":" + STATUS_APPROVED
                    )
            );
            ClubActivityContextHolder.setDetails(
                    applicant.getDisplayName() + "의 가입 신청을 승인했습니다.",
                    applicant.getDisplayName() + "의 가입 신청 승인에 실패했습니다."
            );
            return new ClubJoinActionResponse(
                    club.getClubId(),
                    club.getName(),
                    STATUS_APPROVED,
                    STATUS_ACTIVE,
                    joinRequest.getClubJoinRequestId(),
                    membership.getClubMemberId()
            );
        }

        joinRequest.reject(access.profileUser().getProfileId(), now);
        ProfileUser applicant = profileUserRepository.findById(joinRequest.getProfileId())
                .orElseThrow(() -> new SemoException.ResourceNotFoundException("ProfileUser", "profileId", joinRequest.getProfileId()));
        clubNotificationPublisher.notifyProfile(
                applicant.getProfileId(),
                new NotificationCommand(
                        clubId,
                        "JOIN_REQUEST_REVIEW",
                        "가입 신청이 반려되었습니다",
                        "'" + access.club().getName() + "' 가입 신청이 반려되었습니다.",
                        "JOIN_REQUEST",
                        joinRequest.getClubJoinRequestId(),
                        "/",
                        "join-request:" + joinRequest.getClubJoinRequestId() + ":" + STATUS_REJECTED
                )
        );
        ClubActivityContextHolder.setDetails(
                applicant.getDisplayName() + "의 가입 신청을 반려했습니다.",
                applicant.getDisplayName() + "의 가입 신청 반려에 실패했습니다."
        );
        return new ClubJoinActionResponse(
                access.club().getClubId(),
                access.club().getName(),
                STATUS_REJECTED,
                STATUS_REJECTED,
                joinRequest.getClubJoinRequestId(),
                null
        );
    }

    private Comparator<Club> buildDiscoverComparator(
            String query,
            Collection<String> preferredCategoryKeys,
            Collection<String> preferredActivityTags,
            Map<Long, List<String>> activityTagsByClubId,
            Map<Long, ClubJoinRequest> joinRequestByClubId
    ) {
        Comparator<Club> comparator = Comparator
                .comparingInt((Club club) -> statusPriority(joinRequestByClubId.get(club.getClubId())))
                .thenComparingInt(club -> tagPriority(preferredActivityTags, activityTagsByClubId.getOrDefault(club.getClubId(), List.of())))
                .thenComparingInt(club -> categoryPriority(
                        preferredCategoryKeys,
                        clubClassificationSupport.resolveStored(
                                club.getActivityCategory(),
                                activityTagsByClubId.getOrDefault(club.getClubId(), List.of()),
                                club.getAffiliationType(),
                                club.getCategoryKey()
                        ).activityCategory()
                ));

        if (StringUtils.hasText(query)) {
            comparator = comparator
                    .thenComparingInt(club -> queryMatchPriority(club, query))
                    .thenComparing((Club club) -> club.getName(), String.CASE_INSENSITIVE_ORDER);
        }

        return comparator
                .thenComparing(Club::getCreateDate, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(Club::getClubId, Comparator.reverseOrder());
    }

    private int statusPriority(ClubJoinRequest joinRequest) {
        if (joinRequest == null) {
            return 1;
        }
        return switch (joinRequest.getRequestStatus()) {
            case STATUS_PENDING -> 0;
            case STATUS_REJECTED -> 2;
            default -> 1;
        };
    }

    private int categoryPriority(Collection<String> preferredCategoryKeys, String categoryKey) {
        return preferredCategoryKeys.contains(categoryKey) ? 0 : 1;
    }

    private int tagPriority(Collection<String> preferredActivityTags, Collection<String> activityTags) {
        return hasMatchingTags(preferredActivityTags, activityTags) ? 0 : 1;
    }

    private int queryMatchPriority(Club club, String query) {
        String normalizedQuery = query.toLowerCase(Locale.ROOT);
        String clubName = club.getName().toLowerCase(Locale.ROOT);
        String regionLabel = trimToNull(club.getRegionLabel());
        String normalizedRegionLabel = regionLabel == null ? null : regionLabel.toLowerCase(Locale.ROOT);
        if (clubName.equals(normalizedQuery)) {
            return 0;
        }
        if (clubName.startsWith(normalizedQuery)) {
            return 1;
        }
        if (normalizedRegionLabel != null && normalizedRegionLabel.equals(normalizedQuery)) {
            return 2;
        }
        if (normalizedRegionLabel != null && normalizedRegionLabel.startsWith(normalizedQuery)) {
            return 3;
        }
        return 4;
    }

    private ClubDiscoverSummaryResponse toDiscoverSummary(
            Club club,
            List<String> storedActivityTags,
            int activeMemberCount,
            ClubJoinRequest joinRequest,
            boolean recommendedByCategory,
            boolean recommendedByTags,
            ClubGrowthCoreResponse growthCore
    ) {
        String fileName = club.getImageFileName();
        ClubClassificationSupport.ResolvedClubClassification resolvedClassification = clubClassificationSupport.resolveStored(
                club.getActivityCategory(),
                storedActivityTags,
                club.getAffiliationType(),
                club.getCategoryKey()
        );
        return new ClubDiscoverSummaryResponse(
                club.getClubId(),
                club.getName(),
                club.getSummary(),
                club.getDescription(),
                club.getCategoryKey(),
                resolvedClassification.activityCategory(),
                resolvedClassification.activityTags(),
                resolvedClassification.affiliationType(),
                club.getVisibilityStatus(),
                club.getMembershipPolicy(),
                club.getRegionScope(),
                club.getRegionDepth1Code(),
                club.getRegionDepth2Code(),
                club.getRegionDepth1Name(),
                club.getRegionDepth2Name(),
                club.getRegionLabel(),
                activeMemberCount,
                fileName,
                imageFileUrlResolver.resolveImageUrl(fileName),
                imageFileUrlResolver.resolveThumbnailUrl(fileName),
                joinRequest == null ? "NONE" : joinRequest.getRequestStatus(),
                joinRequest == null ? null : joinRequest.getClubJoinRequestId(),
                recommendedByCategory,
                recommendedByTags,
                growthCore
        );
    }

    private ClubJoinRequestInboxItemResponse toJoinRequestInboxItemResponse(
            ClubJoinRequest request,
            ProfileUser profileUser
    ) {
        String displayName = profileUser == null || !StringUtils.hasText(profileUser.getDisplayName())
                ? "SEMO User"
                : profileUser.getDisplayName();
        return new ClubJoinRequestInboxItemResponse(
                request.getClubJoinRequestId(),
                request.getClubId(),
                request.getProfileId(),
                displayName,
                trimToNull(profileUser == null ? null : profileUser.getTagline()),
                trimToNull(profileUser == null ? null : profileUser.getProfileColor()),
                trimToNull(request.getRequestMessage()),
                request.getCreateDate() == null ? null : request.getCreateDate().toString(),
                request.getCreateDate() == null ? null : request.getCreateDate().format(REQUESTED_LABEL_FORMATTER),
                request.getRequestStatus()
        );
    }

    private ClubJoinRequestInboxResponse buildJoinRequestInboxResponse(ClubAccessResolver.ClubAccess access) {
        List<ClubJoinRequestInboxItemResponse> requests = loadJoinRequestItems(access.club().getClubId());
        int requestedTodayCount = (int) requests.stream()
                .filter(request -> isRequestedToday(request.requestedAt()))
                .count();
        int messageAttachedCount = (int) requests.stream()
                .filter(request -> StringUtils.hasText(request.requestMessage()))
                .count();
        String latestRequestedAtLabel = requests.isEmpty() ? null : requests.getFirst().requestedAtLabel();

        return new ClubJoinRequestInboxResponse(
                access.club().getClubId(),
                access.club().getName(),
                access.isAdmin(),
                requests.size(),
                requestedTodayCount,
                messageAttachedCount,
                latestRequestedAtLabel,
                requests
        );
    }

    private List<ClubJoinRequestInboxItemResponse> loadJoinRequestItems(Long clubId) {
        List<ClubJoinRequest> requests = clubJoinRequestRepository
                .findByClubIdAndRequestStatusOrderByCreateDateDescClubJoinRequestIdDesc(clubId, STATUS_PENDING);
        if (requests.isEmpty()) {
            return List.of();
        }

        Map<Long, ProfileUser> profileUserById = profileUserRepository.findAllById(
                        requests.stream().map(ClubJoinRequest::getProfileId).distinct().toList()
                ).stream()
                .collect(Collectors.toMap(ProfileUser::getProfileId, Function.identity()));

        return requests.stream()
                .map(request -> toJoinRequestInboxItemResponse(request, profileUserById.get(request.getProfileId())))
                .toList();
    }

    private Map<Long, List<String>> toActivityTagsByClubId(List<ClubActivityTag> activityTags) {
        return activityTags.stream()
                .collect(Collectors.groupingBy(
                        ClubActivityTag::getClubId,
                        Collectors.mapping(ClubActivityTag::getTagKey, Collectors.toList())
                ));
    }

    private boolean hasMatchingTags(Collection<String> preferredActivityTags, Collection<String> activityTags) {
        if (preferredActivityTags == null || preferredActivityTags.isEmpty() || activityTags == null || activityTags.isEmpty()) {
            return false;
        }
        return activityTags.stream().anyMatch(preferredActivityTags::contains);
    }

    private String recommendationLabel(Collection<String> preferredActivityTags, Collection<String> preferredCategoryKeys) {
        if (!preferredActivityTags.isEmpty()) {
            return "내 클럽과 비슷한 활동 태그 우선";
        }
        if (!preferredCategoryKeys.isEmpty()) {
            return "내 클럽과 비슷한 활동 카테고리 우선";
        }
        return "최근 생성된 공개 클럽";
    }

    private Club requireJoinableClub(Long clubId) {
        Club club = clubRepository.findById(clubId)
                .filter(Club::isActive)
                .orElseThrow(() -> new SemoException.ResourceNotFoundException("Club", "clubId", clubId));
        if (!VISIBILITY_PUBLIC.equals(club.getVisibilityStatus())) {
            throw new SemoException.ForbiddenException("공개 클럽만 가입 신청할 수 있습니다.");
        }
        return club;
    }

    private ProfileUser requireProfileUser(String userKey) {
        return profileUserRepository.findByUserKey(userKey)
                .orElseThrow(() -> new SemoException.ResourceNotFoundException("ProfileUser", "userKey", userKey));
    }

    private void validateNoExistingMembership(Club club, Long profileId) {
        clubMemberRepository.findByClubIdAndProfileId(club.getClubId(), profileId).ifPresent(member -> {
            throw new SemoException.ConflictException("이미 모임에 등록된 사용자입니다.");
        });
    }

    private ClubMember createMember(Club club, ProfileUser profileUser) {
        LocalDateTime now = LocalDateTime.now();
        ClubMember membership = clubMemberRepository.save(ClubMember.builder()
                .clubId(club.getClubId())
                .profileId(profileUser.getProfileId())
                .roleCode(ROLE_MEMBER)
                .membershipStatus(STATUS_ACTIVE)
                .joinedAt(now)
                .lastActivityAt(now)
                .build());
        clubProfileProvisioner.ensureProfile(membership, profileUser);
        return membership;
    }

    private String normalizeReviewStatus(String requestStatus) {
        String normalized = StringUtils.hasText(requestStatus)
                ? requestStatus.trim().toUpperCase(Locale.ROOT)
                : STATUS_REJECTED;
        if (!Set.of(STATUS_APPROVED, STATUS_REJECTED).contains(normalized)) {
            throw new SemoException.ValidationException("지원하지 않는 가입 신청 처리 상태입니다.");
        }
        return normalized;
    }

    private String normalizeQuery(String query) {
        if (!StringUtils.hasText(query)) {
            return "";
        }
        return query.trim();
    }

    private boolean isRequestedToday(String requestedAt) {
        if (!StringUtils.hasText(requestedAt)) {
            return false;
        }
        try {
            return LocalDateTime.parse(requestedAt).toLocalDate().isEqual(LocalDate.now());
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
