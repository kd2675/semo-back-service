package semo.back.service.feature.club.biz;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import semo.back.service.common.exception.SemoException;
import semo.back.service.common.util.ImageFileUrlResolver;
import semo.back.service.database.pub.entity.ClubMember;
import semo.back.service.database.pub.entity.ClubProfile;
import semo.back.service.database.pub.entity.ProfileUser;
import semo.back.service.database.pub.repository.ClubMemberRepository;
import semo.back.service.database.pub.repository.ClubProfileRepository;
import semo.back.service.database.pub.repository.ProfileUserRepository;
import semo.back.service.feature.club.vo.ClubAdminMemberResponse;
import semo.back.service.feature.club.vo.ClubAdminMembersResponse;
import semo.back.service.feature.club.vo.UpdateClubAdminMemberRoleRequest;
import semo.back.service.feature.club.vo.UpdateClubAdminMemberStatusRequest;
import semo.back.service.feature.position.biz.ClubPositionService;
import semo.back.service.feature.position.vo.ClubPositionSummaryResponse;
import semo.back.service.feature.position.vo.UpdateClubMemberPositionsRequest;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubAdminMemberService {
    private static final String ROLE_OWNER = "OWNER";
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_MEMBER = "MEMBER";
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_DORMANT = "DORMANT";
    private static final String STATUS_PENDING = "PENDING";
    private static final Set<String> ALLOWED_ROLE_CODES = Set.of(ROLE_OWNER, ROLE_ADMIN, ROLE_MEMBER);
    private static final Set<String> ALLOWED_STATUS_CODES = Set.of(STATUS_ACTIVE, STATUS_DORMANT);
    private static final DateTimeFormatter DATE_LABEL_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd", Locale.KOREAN);
    private static final DateTimeFormatter DATE_TIME_LABEL_FORMATTER = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm", Locale.KOREAN);

    private final ClubAccessResolver clubAccessResolver;
    private final ClubMemberRepository clubMemberRepository;
    private final ClubProfileRepository clubProfileRepository;
    private final ProfileUserRepository profileUserRepository;
    private final ImageFileUrlResolver imageFileUrlResolver;
    private final ClubPositionService clubPositionService;

    public ClubAdminMembersResponse getAdminMembers(Long clubId, String userKey) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireAdmin(clubId, userKey);
        boolean roleManagementEnabled = clubPositionService.isRoleManagementEnabled(clubId);
        List<ClubPositionSummaryResponse> availablePositions = clubPositionService.getAvailablePositionSummaries(clubId);
        List<ClubAdminMemberResponse> members = loadMemberResponses(clubId, access);
        return new ClubAdminMembersResponse(
                access.club().getClubId(),
                access.club().getName(),
                true,
                roleManagementEnabled,
                availablePositions,
                members
        );
    }

    @Transactional(transactionManager = "pubTransactionManager")
    public ClubAdminMemberResponse updateMemberRole(
            Long clubId,
            Long clubMemberId,
            String userKey,
            UpdateClubAdminMemberRoleRequest request
    ) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireAdmin(clubId, userKey);
        ClubMember target = requireManagedMember(clubId, clubMemberId, access);
        String normalizedRoleCode = normalizeRoleCode(request.roleCode());
        validateRoleMutation(access, target, normalizedRoleCode);
        target.updateRoleCode(normalizedRoleCode);
        return toResponse(target, access, loadClubProfile(target), loadProfileUser(target), loadAssignedPositions(clubId, target.getClubMemberId()));
    }

    @Transactional(transactionManager = "pubTransactionManager")
    public ClubAdminMemberResponse updateMemberStatus(
            Long clubId,
            Long clubMemberId,
            String userKey,
            UpdateClubAdminMemberStatusRequest request
    ) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireAdmin(clubId, userKey);
        ClubMember target = requireManagedMember(clubId, clubMemberId, access);
        String normalizedStatus = normalizeStatus(request.membershipStatus());
        validateStatusMutation(target, normalizedStatus);
        target.updateMembershipStatus(normalizedStatus);
        return toResponse(target, access, loadClubProfile(target), loadProfileUser(target), loadAssignedPositions(clubId, target.getClubMemberId()));
    }

    @Transactional(transactionManager = "pubTransactionManager")
    public ClubAdminMemberResponse approvePendingMember(Long clubId, Long clubMemberId, String userKey) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireAdmin(clubId, userKey);
        ClubMember target = requireManagedMember(clubId, clubMemberId, access);
        if (!STATUS_PENDING.equals(target.getMembershipStatus())) {
            throw new SemoException.ValidationException("가입 대기 상태인 멤버만 승인할 수 있습니다.");
        }
        target.markApproved(LocalDateTime.now());
        ProfileUser profileUser = loadProfileUser(target);
        ClubProfile clubProfile = clubProfileRepository.findByClubMemberId(target.getClubMemberId())
                .orElseGet(() -> clubProfileRepository.save(ClubProfile.builder()
                        .clubMemberId(target.getClubMemberId())
                        .displayName(StringUtils.hasText(profileUser.getDisplayName()) ? profileUser.getDisplayName().trim() : "SEMO Member")
                        .tagline(trimToNull(profileUser.getTagline()))
                        .introText(null)
                        .avatarFileName(null)
                        .build()));
        return toResponse(target, access, clubProfile, profileUser, List.of());
    }

    @Transactional(transactionManager = "pubTransactionManager")
    public ClubAdminMemberResponse updateMemberPositions(
            Long clubId,
            Long clubMemberId,
            String userKey,
            UpdateClubMemberPositionsRequest request
    ) {
        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireAdmin(clubId, userKey);
        ClubMember target = requireManagedMember(clubId, clubMemberId, access);
        clubPositionService.replaceMemberPositions(access, target, request == null ? null : request.clubPositionIds());
        return toResponse(target, access, loadClubProfile(target), loadProfileUser(target), loadAssignedPositions(clubId, target.getClubMemberId()));
    }

    private List<ClubAdminMemberResponse> loadMemberResponses(Long clubId, ClubAccessResolver.ClubAccess access) {
        List<ClubMember> memberships = clubMemberRepository.findByClubIdOrderByClubMemberIdAsc(clubId);
        if (memberships.isEmpty()) {
            return List.of();
        }
        Map<Long, List<ClubPositionSummaryResponse>> positionsByMemberId = clubPositionService.getAssignedPositionSummaries(
                clubId,
                memberships.stream().map(ClubMember::getClubMemberId).toList()
        );

        Map<Long, ClubProfile> clubProfileByMemberId = clubProfileRepository.findByClubMemberIdIn(
                        memberships.stream().map(ClubMember::getClubMemberId).toList()
                ).stream()
                .collect(Collectors.toMap(ClubProfile::getClubMemberId, Function.identity()));

        Map<Long, ProfileUser> profileUserById = profileUserRepository.findAllById(
                        memberships.stream().map(ClubMember::getProfileId).toList()
                ).stream()
                .collect(Collectors.toMap(ProfileUser::getProfileId, Function.identity()));

        return memberships.stream()
                .map(member -> toResponse(
                        member,
                        access,
                        clubProfileByMemberId.get(member.getClubMemberId()),
                        profileUserById.get(member.getProfileId()),
                        positionsByMemberId.getOrDefault(member.getClubMemberId(), List.of())
                ))
                .sorted(adminMemberComparator())
                .toList();
    }

    private Comparator<ClubAdminMemberResponse> adminMemberComparator() {
        return Comparator.comparingInt((ClubAdminMemberResponse member) -> statusPriority(member.membershipStatus()))
                .thenComparing(ClubAdminMemberResponse::displayName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(ClubAdminMemberResponse::clubMemberId);
    }

    private int statusPriority(String membershipStatus) {
        return switch (membershipStatus) {
            case STATUS_PENDING -> 0;
            case STATUS_ACTIVE -> 1;
            case STATUS_DORMANT -> 2;
            default -> 3;
        };
    }

    private ClubAdminMemberResponse toResponse(
            ClubMember member,
            ClubAccessResolver.ClubAccess access,
            ClubProfile clubProfile,
            ProfileUser profileUser,
            List<ClubPositionSummaryResponse> positions
    ) {
        String displayName = clubProfile != null && StringUtils.hasText(clubProfile.getDisplayName())
                ? clubProfile.getDisplayName()
                : profileUser != null && StringUtils.hasText(profileUser.getDisplayName())
                    ? profileUser.getDisplayName()
                    : "SEMO Member";
        String tagline = clubProfile != null ? clubProfile.getTagline() : null;
        boolean self = access.membership().getClubMemberId().equals(member.getClubMemberId());
        boolean ownerActor = ROLE_OWNER.equals(access.membership().getRoleCode());
        boolean targetOwner = ROLE_OWNER.equals(member.getRoleCode());
        boolean canManage = !self && (ownerActor || !targetOwner);
        boolean canApprove = canManage && STATUS_PENDING.equals(member.getMembershipStatus());

        return new ClubAdminMemberResponse(
                member.getClubMemberId(),
                clubProfile != null ? clubProfile.getClubProfileId() : null,
                member.getProfileId(),
                displayName,
                tagline,
                imageFileUrlResolver.resolveImageUrl(clubProfile != null ? clubProfile.getAvatarFileName() : null),
                member.getJoinedAt() == null ? null : member.getJoinedAt().format(DATE_LABEL_FORMATTER),
                member.getLastActivityAt() == null ? null : member.getLastActivityAt().format(DATE_TIME_LABEL_FORMATTER),
                member.getRoleCode(),
                member.getMembershipStatus(),
                canManage,
                canApprove,
                self,
                positions
        );
    }

    private ClubMember requireManagedMember(Long clubId, Long clubMemberId, ClubAccessResolver.ClubAccess access) {
        ClubMember target = clubMemberRepository.findByClubMemberIdAndClubId(clubMemberId, clubId)
                .orElseThrow(() -> new SemoException.ResourceNotFoundException("ClubMember", "clubMemberId", clubMemberId));
        if (access.membership().getClubMemberId().equals(target.getClubMemberId())) {
            throw new SemoException.ForbiddenException("본인 계정은 여기서 변경할 수 없습니다.");
        }
        if (ROLE_OWNER.equals(target.getRoleCode()) && !ROLE_OWNER.equals(access.membership().getRoleCode())) {
            throw new SemoException.ForbiddenException("OWNER 멤버는 OWNER만 관리할 수 있습니다.");
        }
        return target;
    }

    private void validateRoleMutation(
            ClubAccessResolver.ClubAccess access,
            ClubMember target,
            String normalizedRoleCode
    ) {
        if (ROLE_OWNER.equals(target.getRoleCode())) {
            throw new SemoException.ForbiddenException("OWNER 역할은 이 화면에서 변경할 수 없습니다.");
        }
        if (ROLE_OWNER.equals(normalizedRoleCode) && !ROLE_OWNER.equals(access.membership().getRoleCode())) {
            throw new SemoException.ForbiddenException("OWNER 역할 지정은 OWNER만 할 수 있습니다.");
        }
    }

    private void validateStatusMutation(ClubMember target, String normalizedStatus) {
        if (STATUS_PENDING.equals(target.getMembershipStatus())) {
            throw new SemoException.ValidationException("가입 대기 멤버는 승인 버튼으로 처리해야 합니다.");
        }
        if (ROLE_OWNER.equals(target.getRoleCode())) {
            throw new SemoException.ForbiddenException("OWNER 상태는 변경할 수 없습니다.");
        }
        if (normalizedStatus.equals(target.getMembershipStatus())) {
            return;
        }
    }

    private String normalizeRoleCode(String roleCode) {
        String normalized = StringUtils.hasText(roleCode) ? roleCode.trim().toUpperCase(Locale.ROOT) : ROLE_MEMBER;
        if (!ALLOWED_ROLE_CODES.contains(normalized)) {
            throw new SemoException.ValidationException("지원하지 않는 역할입니다.");
        }
        return normalized;
    }

    private String normalizeStatus(String membershipStatus) {
        String normalized = StringUtils.hasText(membershipStatus)
                ? membershipStatus.trim().toUpperCase(Locale.ROOT)
                : STATUS_ACTIVE;
        if (!ALLOWED_STATUS_CODES.contains(normalized)) {
            throw new SemoException.ValidationException("지원하지 않는 회원 상태입니다.");
        }
        return normalized;
    }

    private ClubProfile loadClubProfile(ClubMember target) {
        return clubProfileRepository.findByClubMemberId(target.getClubMemberId()).orElse(null);
    }

    private ProfileUser loadProfileUser(ClubMember target) {
        return profileUserRepository.findById(target.getProfileId())
                .orElseThrow(() -> new SemoException.ResourceNotFoundException("ProfileUser", "profileId", target.getProfileId()));
    }

    private List<ClubPositionSummaryResponse> loadAssignedPositions(Long clubId, Long clubMemberId) {
        return clubPositionService.getAssignedPositionSummaries(clubId, List.of(clubMemberId))
                .getOrDefault(clubMemberId, List.of());
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
