package semo.back.service.feature.club.biz;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import semo.back.service.database.pub.entity.ClubMember;
import semo.back.service.database.pub.entity.ClubProfile;
import semo.back.service.database.pub.entity.ProfileUser;
import semo.back.service.database.pub.repository.ClubAttendanceCheckInRepository;
import semo.back.service.database.pub.repository.ClubAttendanceSessionRepository;
import semo.back.service.database.pub.repository.ClubEventParticipantRepository;
import semo.back.service.database.pub.repository.ClubFeatureRepository;
import semo.back.service.database.pub.repository.ClubMemberRepository;
import semo.back.service.database.pub.repository.ClubProfileRepository;
import semo.back.service.database.pub.repository.ClubRepository;
import semo.back.service.database.pub.repository.ClubScheduleEventRepository;
import semo.back.service.database.pub.repository.ClubScheduleVoteOptionRepository;
import semo.back.service.database.pub.repository.ClubScheduleVoteRepository;
import semo.back.service.database.pub.repository.ClubScheduleVoteSelectionRepository;
import semo.back.service.database.pub.repository.ProfileUserRepository;
import semo.back.service.feature.club.vo.ClubAdminMembersResponse;
import semo.back.service.feature.club.vo.CreateClubRequest;
import semo.back.service.feature.club.vo.UpdateClubAdminMemberRoleRequest;
import semo.back.service.feature.club.vo.UpdateClubAdminMemberStatusRequest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ClubAdminMemberServiceTest {

    @Autowired
    private ClubAdminMemberService clubAdminMemberService;

    @Autowired
    private ClubService clubService;

    @Autowired
    private ClubRepository clubRepository;

    @Autowired
    private ClubMemberRepository clubMemberRepository;

    @Autowired
    private ClubProfileRepository clubProfileRepository;

    @Autowired
    private ClubFeatureRepository clubFeatureRepository;

    @Autowired
    private ClubAttendanceCheckInRepository clubAttendanceCheckInRepository;

    @Autowired
    private ClubAttendanceSessionRepository clubAttendanceSessionRepository;

    @Autowired
    private ClubEventParticipantRepository clubEventParticipantRepository;

    @Autowired
    private ClubScheduleEventRepository clubScheduleEventRepository;

    @Autowired
    private ClubScheduleVoteOptionRepository clubScheduleVoteOptionRepository;

    @Autowired
    private ClubScheduleVoteRepository clubScheduleVoteRepository;

    @Autowired
    private ClubScheduleVoteSelectionRepository clubScheduleVoteSelectionRepository;

    @Autowired
    private ProfileUserRepository profileUserRepository;

    @BeforeEach
    void setUp() {
        clubScheduleVoteSelectionRepository.deleteAll();
        clubScheduleVoteOptionRepository.deleteAll();
        clubScheduleVoteRepository.deleteAll();
        clubEventParticipantRepository.deleteAll();
        clubScheduleEventRepository.deleteAll();
        clubAttendanceCheckInRepository.deleteAll();
        clubAttendanceSessionRepository.deleteAll();
        clubFeatureRepository.deleteAll();
        clubProfileRepository.deleteAll();
        clubMemberRepository.deleteAll();
        clubRepository.deleteAll();
        profileUserRepository.deleteAll();
    }

    @Test
    void getAdminMembersReturnsPendingMembersFirst() {
        Long clubId = clubService.createClub(
                "owner-members-001",
                "Owner Member",
                new CreateClubRequest("Member Club", null, "OTHER", "PUBLIC", "APPROVAL", null)
        ).clubId();

        createMember(clubId, "member-active-001", "활동 회원", "MEMBER", "ACTIVE", LocalDateTime.now().minusDays(10));
        createMember(clubId, "member-dormant-001", "휴면 회원", "MEMBER", "DORMANT", LocalDateTime.now().minusDays(20));
        createMember(clubId, "member-pending-001", "가입 대기 회원", "MEMBER", "PENDING", null);

        ClubAdminMembersResponse response = clubAdminMemberService.getAdminMembers(clubId, "owner-members-001");

        assertThat(response.members()).hasSize(4);
        assertThat(response.members().get(0).membershipStatus()).isEqualTo("PENDING");
        assertThat(response.members().get(0).canApprove()).isTrue();
        assertThat(response.members().stream().filter(member -> member.self()).findFirst()).isPresent();
    }

    @Test
    void approvePendingMemberActivatesMembershipAndCreatesClubProfile() {
        Long clubId = clubService.createClub(
                "owner-members-002",
                "Owner Member",
                new CreateClubRequest("Approval Club", null, "OTHER", "PUBLIC", "APPROVAL", null)
        ).clubId();

        ClubMember pendingMember = createMember(clubId, "member-pending-002", "승인 대기", "MEMBER", "PENDING", null);

        var response = clubAdminMemberService.approvePendingMember(
                clubId,
                pendingMember.getClubMemberId(),
                "owner-members-002"
        );

        ClubMember updated = clubMemberRepository.findById(pendingMember.getClubMemberId()).orElseThrow();
        assertThat(updated.getMembershipStatus()).isEqualTo("ACTIVE");
        assertThat(updated.getJoinedAt()).isNotNull();
        assertThat(clubProfileRepository.findByClubMemberId(pendingMember.getClubMemberId())).isPresent();
        assertThat(response.membershipStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void updateMemberRoleChangesStoredRoleCode() {
        Long clubId = clubService.createClub(
                "owner-members-003",
                "Owner Member",
                new CreateClubRequest("Role Club", null, "OTHER", "PUBLIC", "APPROVAL", null)
        ).clubId();

        ClubMember member = createMember(clubId, "member-role-003", "역할 변경 회원", "MEMBER", "ACTIVE", LocalDateTime.now().minusDays(3));

        var response = clubAdminMemberService.updateMemberRole(
                clubId,
                member.getClubMemberId(),
                "owner-members-003",
                new UpdateClubAdminMemberRoleRequest("ADMIN")
        );

        assertThat(clubMemberRepository.findById(member.getClubMemberId()).orElseThrow().getRoleCode()).isEqualTo("ADMIN");
        assertThat(response.roleCode()).isEqualTo("ADMIN");
    }

    @Test
    void updateMemberStatusChangesStoredMembershipStatus() {
        Long clubId = clubService.createClub(
                "owner-members-004",
                "Owner Member",
                new CreateClubRequest("Status Club", null, "OTHER", "PUBLIC", "APPROVAL", null)
        ).clubId();

        ClubMember member = createMember(clubId, "member-status-004", "상태 변경 회원", "MEMBER", "ACTIVE", LocalDateTime.now().minusDays(5));

        var response = clubAdminMemberService.updateMemberStatus(
                clubId,
                member.getClubMemberId(),
                "owner-members-004",
                new UpdateClubAdminMemberStatusRequest("DORMANT")
        );

        assertThat(clubMemberRepository.findById(member.getClubMemberId()).orElseThrow().getMembershipStatus()).isEqualTo("DORMANT");
        assertThat(response.membershipStatus()).isEqualTo("DORMANT");
    }

    private ClubMember createMember(
            Long clubId,
            String userKey,
            String displayName,
            String roleCode,
            String membershipStatus,
            LocalDateTime joinedAt
    ) {
        ProfileUser profileUser = profileUserRepository.save(ProfileUser.builder()
                .userKey(userKey)
                .displayName(displayName)
                .tagline(displayName + " 소개")
                .profileColor("#135bec")
                .build());

        ClubMember member = clubMemberRepository.save(ClubMember.builder()
                .clubId(clubId)
                .profileId(profileUser.getProfileId())
                .roleCode(roleCode)
                .membershipStatus(membershipStatus)
                .joinedAt(joinedAt)
                .lastActivityAt(joinedAt)
                .build());

        if (!"PENDING".equals(membershipStatus)) {
            clubProfileRepository.save(ClubProfile.builder()
                    .clubMemberId(member.getClubMemberId())
                    .displayName(displayName)
                    .tagline(displayName + " 태그라인")
                    .introText(null)
                    .avatarFileName(null)
                    .build());
        }

        return member;
    }
}
