package semo.back.service.feature.club.biz;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import semo.back.service.common.exception.SemoException;
import semo.back.service.common.util.ImageFileUrlResolver;
import semo.back.service.database.pub.entity.Club;
import semo.back.service.database.pub.entity.ClubJoinRequest;
import semo.back.service.database.pub.entity.ClubMember;
import semo.back.service.database.pub.entity.ClubProfile;
import semo.back.service.database.pub.entity.ProfileUser;
import semo.back.service.database.pub.repository.ClubJoinRequestRepository;
import semo.back.service.database.pub.repository.ClubMemberCountRow;
import semo.back.service.database.pub.repository.ClubMemberRepository;
import semo.back.service.database.pub.repository.ClubProfileRepository;
import semo.back.service.database.pub.repository.ClubRepository;
import semo.back.service.database.pub.repository.ProfileUserRepository;
import semo.back.service.feature.activity.biz.ClubActivityContextHolder;
import semo.back.service.feature.activity.biz.RecordClubActivity;
import semo.back.service.feature.club.vo.ClubAdminJoinRequestResponse;
import semo.back.service.feature.club.vo.ClubAdminJoinRequestsResponse;
import semo.back.service.feature.club.vo.ClubDiscoverResponse;
import semo.back.service.feature.club.vo.ClubDiscoverSummaryResponse;
import semo.back.service.feature.club.vo.ClubJoinActionResponse;
import semo.back.service.feature.club.vo.ReviewClubJoinRequestRequest;
import semo.back.service.feature.club.vo.SubmitClubJoinRequestRequest;

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
    private final ClubMemberRepository clubMemberRepository;
    private final ClubProfileRepository clubProfileRepository;
    private final ClubJoinRequestRepository clubJoinRequestRepository;
    private final ProfileUserRepository profileUserRepository;
    private final ImageFileUrlResolver imageFileUrlResolver;
    private final ClubAccessResolver clubAccessResolver;

    public ClubDiscoverResponse getDiscoverClubs(String userKey, String query) {
        ProfileUser profileUser = requireProfileUser(userKey);
        List<ClubMember> memberships = clubMemberRepository.findByProfileId(profileUser.getProfileId());
        Set<Long> memberClubIds = memberships.stream()
                .map(ClubMember::getClubId)
                .collect(Collectors.toSet());

        List<Club> myClubs = memberClubIds.isEmpty()
                ? List.of()
                : clubRepository.findByClubIdInAndActiveTrue(memberClubIds);
        Set<String> preferredCategoryKeys = myClubs.stream()
                .map(Club::getCategoryKey)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());

        String normalizedQuery = normalizeQuery(query);
        List<Club> publicClubs = clubRepository.searchPublicActiveClubs(normalizedQuery).stream()
                .filter(club -> !memberClubIds.contains(club.getClubId()))
                .toList();
        if (publicClubs.isEmpty()) {
            return new ClubDiscoverResponse(normalizedQuery, !StringUtils.hasText(normalizedQuery), recommendationLabel(preferredCategoryKeys), 0, List.of());
        }

        List<Long> clubIds = publicClubs.stream().map(Club::getClubId).toList();
        Map<Long, Integer> activeMemberCountByClubId = toActiveMemberCountMap(
                clubMemberRepository.countMembersByClubIdInAndMembershipStatus(clubIds, STATUS_ACTIVE)
        );
        Map<Long, ClubJoinRequest> joinRequestByClubId = clubJoinRequestRepository.findByProfileIdAndClubIdIn(profileUser.getProfileId(), clubIds).stream()
                .collect(Collectors.toMap(ClubJoinRequest::getClubId, Function.identity()));

        Comparator<Club> comparator = buildDiscoverComparator(normalizedQuery, preferredCategoryKeys, joinRequestByClubId);
        List<ClubDiscoverSummaryResponse> clubs = publicClubs.stream()
                .sorted(comparator)
                .limit(DISCOVER_LIMIT)
                .map(club -> toDiscoverSummary(
                        club,
                        activeMemberCountByClubId.getOrDefault(club.getClubId(), 0),
                        joinRequestByClubId.get(club.getClubId()),
                        preferredCategoryKeys.contains(club.getCategoryKey())
                ))
                .toList();

        return new ClubDiscoverResponse(
                normalizedQuery,
                !StringUtils.hasText(normalizedQuery),
                recommendationLabel(preferredCategoryKeys),
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

    public ClubAdminJoinRequestsResponse getAdminJoinRequests(Long clubId, String userKey) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireAdmin(clubId, userKey);
        List<ClubJoinRequest> requests = clubJoinRequestRepository
                .findByClubIdAndRequestStatusOrderByCreateDateDescClubJoinRequestIdDesc(clubId, STATUS_PENDING);
        if (requests.isEmpty()) {
            return new ClubAdminJoinRequestsResponse(clubId, access.club().getName(), true, List.of());
        }

        Map<Long, ProfileUser> profileUserById = profileUserRepository.findAllById(
                        requests.stream().map(ClubJoinRequest::getProfileId).distinct().toList()
                ).stream()
                .collect(Collectors.toMap(ProfileUser::getProfileId, Function.identity()));

        List<ClubAdminJoinRequestResponse> responses = requests.stream()
                .map(request -> toAdminJoinRequestResponse(request, profileUserById.get(request.getProfileId())))
                .toList();

        return new ClubAdminJoinRequestsResponse(clubId, access.club().getName(), true, responses);
    }

    @Transactional(transactionManager = "pubTransactionManager")
    @RecordClubActivity(subject = "가입신청")
    public ClubJoinActionResponse reviewJoinRequest(
            Long clubId,
            Long clubJoinRequestId,
            String userKey,
            ReviewClubJoinRequestRequest request
    ) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireAdmin(clubId, userKey);
        ClubJoinRequest joinRequest = clubJoinRequestRepository.findByClubJoinRequestIdAndClubId(clubJoinRequestId, clubId)
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
            Map<Long, ClubJoinRequest> joinRequestByClubId
    ) {
        Comparator<Club> comparator = Comparator
                .comparingInt((Club club) -> statusPriority(joinRequestByClubId.get(club.getClubId())))
                .thenComparingInt(club -> categoryPriority(preferredCategoryKeys, club.getCategoryKey()));

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
            int activeMemberCount,
            ClubJoinRequest joinRequest,
            boolean recommendedByCategory
    ) {
        String fileName = club.getImageFileName();
        return new ClubDiscoverSummaryResponse(
                club.getClubId(),
                club.getName(),
                club.getSummary(),
                club.getDescription(),
                club.getCategoryKey(),
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
                recommendedByCategory
        );
    }

    private ClubAdminJoinRequestResponse toAdminJoinRequestResponse(ClubJoinRequest request, ProfileUser profileUser) {
        String displayName = profileUser == null || !StringUtils.hasText(profileUser.getDisplayName())
                ? "SEMO User"
                : profileUser.getDisplayName();
        return new ClubAdminJoinRequestResponse(
                request.getClubJoinRequestId(),
                request.getClubId(),
                request.getProfileId(),
                displayName,
                profileUser == null ? null : profileUser.getTagline(),
                profileUser == null ? null : profileUser.getProfileColor(),
                request.getRequestMessage(),
                request.getCreateDate() == null ? null : request.getCreateDate().toString(),
                request.getCreateDate() == null ? null : request.getCreateDate().format(REQUESTED_LABEL_FORMATTER),
                request.getRequestStatus()
        );
    }

    private Map<Long, Integer> toActiveMemberCountMap(List<ClubMemberCountRow> rows) {
        Map<Long, Integer> result = new HashMap<>();
        for (ClubMemberCountRow row : rows) {
            result.put(row.getClubId(), Math.toIntExact(row.getMemberCount()));
        }
        return result;
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
        clubProfileRepository.findByClubMemberId(membership.getClubMemberId())
                .orElseGet(() -> clubProfileRepository.save(ClubProfile.builder()
                        .clubMemberId(membership.getClubMemberId())
                        .displayName(StringUtils.hasText(profileUser.getDisplayName()) ? profileUser.getDisplayName().trim() : "SEMO Member")
                        .tagline(trimToNull(profileUser.getTagline()))
                        .introText(null)
                        .avatarFileName(null)
                        .build()));
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

    private String recommendationLabel(Collection<String> preferredCategoryKeys) {
        if (preferredCategoryKeys.isEmpty()) {
            return "최근 개설된 공개 클럽";
        }
        return "내 클럽과 비슷한 카테고리 우선";
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
