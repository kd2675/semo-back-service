package semo.back.service.feature.club.biz.policy;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import semo.back.service.common.exception.SemoException;
import semo.back.service.database.pub.entity.Club;
import semo.back.service.database.pub.entity.ClubMember;
import semo.back.service.database.pub.entity.ClubProfile;
import semo.back.service.database.pub.entity.ProfileUser;
import semo.back.service.database.pub.repository.ClubMemberRepository;
import semo.back.service.database.pub.repository.ClubProfileRepository;
import semo.back.service.database.pub.repository.ClubRepository;
import semo.back.service.database.pub.repository.ProfileUserRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubAccessResolver {
    public static final String STATUS_ACTIVE = "ACTIVE";
    private static final List<String> ADMIN_ROLE_CODES = List.of("OWNER", "ADMIN");

    private final ClubRepository clubRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final ClubProfileRepository clubProfileRepository;
    private final ProfileUserRepository profileUserRepository;

    public ClubAccess requireActiveMember(Long clubId, String userKey) {
        ProfileUser profileUser = profileUserRepository.findByUserKey(userKey)
                .orElseThrow(() -> new SemoException.ResourceNotFoundException("ProfileUser", "userKey", userKey));
        Club club = clubRepository.findById(clubId)
                .filter(Club::isActive)
                .orElseThrow(() -> new SemoException.ResourceNotFoundException("Club", "clubId", clubId));
        ClubMember membership = clubMemberRepository.findByClubIdAndProfileId(clubId, profileUser.getProfileId())
                .filter(member -> STATUS_ACTIVE.equals(member.getMembershipStatus()))
                .orElseThrow(() -> new SemoException.ResourceNotFoundException("ClubMember", "clubId", clubId));
        ClubProfile clubProfile = requireClubProfile(membership.getClubMemberId());
        return new ClubAccess(club, membership, clubProfile, profileUser);
    }

    public ClubAccess requireAdmin(Long clubId, String userKey) {
        ClubAccess access = requireActiveMember(clubId, userKey);
        if (!ADMIN_ROLE_CODES.contains(access.membership().getRoleCode())) {
            throw new SemoException.ForbiddenException("ADMIN role required");
        }
        return access;
    }

    public List<ClubMemberSnapshot> getActiveMemberSnapshots(Long clubId) {
        List<ClubMember> memberships = clubMemberRepository
                .findByClubIdAndMembershipStatusOrderByJoinedAtAscClubMemberIdAsc(clubId, STATUS_ACTIVE);
        if (memberships.isEmpty()) {
            return List.of();
        }

        Map<Long, ClubProfile> clubProfileByMemberId = clubProfileRepository.findByClubMemberIdIn(
                        memberships.stream().map(ClubMember::getClubMemberId).toList()
                ).stream()
                .collect(Collectors.toMap(ClubProfile::getClubMemberId, Function.identity()));

        Map<Long, ProfileUser> profileUserById = profileUserRepository.findAllById(
                        memberships.stream().map(ClubMember::getProfileId).toList()
                ).stream()
                .collect(Collectors.toMap(ProfileUser::getProfileId, Function.identity()));

        return memberships.stream()
                .map(membership -> {
                    ProfileUser profileUser = requireProfileUser(
                            membership.getProfileId(),
                            profileUserById.get(membership.getProfileId())
                    );
                    ClubProfile clubProfile = requireClubProfile(
                            membership.getClubMemberId(),
                            clubProfileByMemberId.get(membership.getClubMemberId())
                    );
                    return new ClubMemberSnapshot(
                            membership,
                            clubProfile,
                            profileUser
                    );
                })
                .sorted(Comparator.comparing(snapshot -> snapshot.clubProfile().getDisplayName()))
                .toList();
    }

    private ClubProfile requireClubProfile(Long clubMemberId) {
        return requireClubProfile(
                clubMemberId,
                clubProfileRepository.findByClubMemberId(clubMemberId).orElse(null)
        );
    }

    private ClubProfile requireClubProfile(Long clubMemberId, ClubProfile clubProfile) {
        if (clubProfile == null) {
            throw new IllegalStateException("Active club member profile is missing: clubMemberId=" + clubMemberId);
        }
        return clubProfile;
    }

    private ProfileUser requireProfileUser(Long profileId, ProfileUser profileUser) {
        if (profileUser == null) {
            throw new IllegalStateException("Active club member user profile is missing: profileId=" + profileId);
        }
        return profileUser;
    }

    public record ClubAccess(Club club, ClubMember membership, ClubProfile clubProfile, ProfileUser profileUser) {
        public boolean isAdmin() {
            return ADMIN_ROLE_CODES.contains(membership.getRoleCode());
        }
    }

    public record ClubMemberSnapshot(ClubMember membership, ClubProfile clubProfile, ProfileUser profileUser) {
    }
}
