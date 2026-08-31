package semo.back.service.feature.club.biz.policy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import semo.back.service.database.pub.entity.Club;
import semo.back.service.database.pub.entity.ClubMember;
import semo.back.service.database.pub.entity.ClubProfile;
import semo.back.service.database.pub.entity.ProfileUser;
import semo.back.service.database.pub.repository.ClubMemberRepository;
import semo.back.service.database.pub.repository.ClubProfileRepository;
import semo.back.service.database.pub.repository.ClubRepository;
import semo.back.service.database.pub.repository.ProfileUserRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClubAccessResolverTest {

    @Mock
    private ClubRepository clubRepository;

    @Mock
    private ClubMemberRepository clubMemberRepository;

    @Mock
    private ClubProfileRepository clubProfileRepository;

    @Mock
    private ProfileUserRepository profileUserRepository;

    @InjectMocks
    private ClubAccessResolver clubAccessResolver;

    @Test
    void requireActiveMember_existingClubProfile_returnsAccessWithoutWrite() {
        ProfileUser profileUser = profileUser();
        Club club = club();
        ClubMember membership = membership();
        ClubProfile clubProfile = clubProfile();
        when(profileUserRepository.findByUserKey("member-key")).thenReturn(Optional.of(profileUser));
        when(clubRepository.findById(10L)).thenReturn(Optional.of(club));
        when(clubMemberRepository.findByClubIdAndProfileId(10L, 20L)).thenReturn(Optional.of(membership));
        when(clubProfileRepository.findByClubMemberId(30L)).thenReturn(Optional.of(clubProfile));

        ClubAccessResolver.ClubAccess access = clubAccessResolver.requireActiveMember(10L, "member-key");

        assertThat(access.clubProfile()).isSameAs(clubProfile);
        verify(clubProfileRepository, never()).save(any());
    }

    @Test
    void requireActiveMember_missingClubProfile_throwsInvariantViolationWithoutWrite() {
        when(profileUserRepository.findByUserKey("member-key")).thenReturn(Optional.of(profileUser()));
        when(clubRepository.findById(10L)).thenReturn(Optional.of(club()));
        when(clubMemberRepository.findByClubIdAndProfileId(10L, 20L)).thenReturn(Optional.of(membership()));
        when(clubProfileRepository.findByClubMemberId(30L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clubAccessResolver.requireActiveMember(10L, "member-key"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("clubMemberId=30");
        verify(clubProfileRepository, never()).save(any());
    }

    @Test
    void getActiveMemberSnapshots_missingClubProfile_throwsInvariantViolationWithoutWrite() {
        when(clubMemberRepository.findByClubIdAndMembershipStatusOrderByJoinedAtAscClubMemberIdAsc(
                10L,
                ClubAccessResolver.STATUS_ACTIVE
        )).thenReturn(List.of(membership()));
        when(clubProfileRepository.findByClubMemberIdIn(List.of(30L))).thenReturn(List.of());
        when(profileUserRepository.findAllById(List.of(20L))).thenReturn(List.of(profileUser()));

        assertThatThrownBy(() -> clubAccessResolver.getActiveMemberSnapshots(10L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("clubMemberId=30");
        verify(clubProfileRepository, never()).save(any());
    }

    private ProfileUser profileUser() {
        return ProfileUser.builder()
                .profileId(20L)
                .userKey("member-key")
                .displayName("멤버")
                .build();
    }

    private Club club() {
        return Club.builder()
                .clubId(10L)
                .name("SEMO Club")
                .active(true)
                .build();
    }

    private ClubMember membership() {
        return ClubMember.builder()
                .clubMemberId(30L)
                .clubId(10L)
                .profileId(20L)
                .roleCode("MEMBER")
                .membershipStatus(ClubAccessResolver.STATUS_ACTIVE)
                .build();
    }

    private ClubProfile clubProfile() {
        return ClubProfile.builder()
                .clubProfileId(40L)
                .clubMemberId(30L)
                .displayName("멤버")
                .build();
    }
}
