package semo.back.service.feature.club.biz;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import semo.back.service.database.pub.entity.ClubProfile;
import semo.back.service.database.pub.repository.ClubMemberRepository;
import semo.back.service.database.pub.repository.ClubProfileRepository;
import semo.back.service.database.pub.repository.ClubRepository;
import semo.back.service.database.pub.repository.ClubFeatureRepository;
import semo.back.service.database.pub.repository.ClubAttendanceCheckInRepository;
import semo.back.service.database.pub.repository.ClubAttendanceSessionRepository;
import semo.back.service.database.pub.repository.ClubEventParticipantRepository;
import semo.back.service.database.pub.repository.ProfileUserRepository;
import semo.back.service.database.pub.repository.ClubScheduleEventRepository;
import semo.back.service.database.pub.repository.ClubScheduleVoteOptionRepository;
import semo.back.service.database.pub.repository.ClubScheduleVoteRepository;
import semo.back.service.database.pub.repository.ClubScheduleVoteSelectionRepository;
import semo.back.service.feature.club.vo.ClubCreateResponse;
import semo.back.service.feature.club.vo.CreateClubRequest;
import semo.back.service.feature.club.vo.MyClubSummaryResponse;
import semo.back.service.feature.club.vo.ClubProfileResponse;
import semo.back.service.feature.club.vo.UpdateClubProfileRequest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ClubServiceTest {

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
    private ProfileUserRepository profileUserRepository;

    @Autowired
    private ClubScheduleEventRepository clubScheduleEventRepository;

    @Autowired
    private ClubScheduleVoteOptionRepository clubScheduleVoteOptionRepository;

    @Autowired
    private ClubScheduleVoteRepository clubScheduleVoteRepository;

    @Autowired
    private ClubScheduleVoteSelectionRepository clubScheduleVoteSelectionRepository;

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
    void createClubCreatesOwnerMembershipAndProfileWhenNeeded() {
        ClubCreateResponse response = clubService.createClub(
                "user-club-001",
                "Club Owner",
                new CreateClubRequest(
                        "Semo Tennis",
                        "서울대 테니스 멤버를 위한 클럽입니다.",
                        "TENNIS",
                        "PUBLIC",
                        "APPROVAL",
                        null
                )
        );

        assertThat(response.clubId()).isNotNull();
        assertThat(response.roleCode()).isEqualTo("OWNER");
        assertThat(response.summary()).isEqualTo("서울대 테니스 멤버를 위한 클럽입니다.");
        assertThat(response.fileName()).isNull();
        assertThat(profileUserRepository.count()).isOne();
        assertThat(clubRepository.count()).isOne();
        assertThat(clubMemberRepository.count()).isOne();
        assertThat(clubProfileRepository.count()).isOne();
        assertThat(clubMemberRepository.findByClubIdAndProfileId(
                response.clubId(),
                profileUserRepository.findByUserKey("user-club-001").orElseThrow().getProfileId()
        )).isPresent();
    }

    @Test
    void getMyClubsReturnsActiveMemberships() {
        clubService.createClub(
                "user-club-003",
                "Club Owner",
                new CreateClubRequest(
                        "Semo Hiking",
                        "등산 모임입니다.",
                        "HIKING",
                        "PUBLIC",
                        "APPROVAL",
                        null
                )
        );

        var clubs = clubService.getMyClubs("user-club-003");

        assertThat(clubs).hasSize(1);
        MyClubSummaryResponse club = clubs.getFirst();
        assertThat(club.name()).isEqualTo("Semo Hiking");
        assertThat(club.roleCode()).isEqualTo("OWNER");
        assertThat(club.admin()).isTrue();
    }

    @Test
    void getMyClubReturnsRequestedClubForMember() {
        ClubCreateResponse created = clubService.createClub(
                "user-club-005",
                "Club Owner",
                new CreateClubRequest(
                        "Semo Tennis Crew",
                        "테니스 모임입니다.",
                        "TENNIS",
                        "PUBLIC",
                        "APPROVAL",
                        null
                )
        );

        MyClubSummaryResponse club = clubService.getMyClub(created.clubId(), "user-club-005");

        assertThat(club.clubId()).isEqualTo(created.clubId());
        assertThat(club.name()).isEqualTo("Semo Tennis Crew");
    }

    @Test
    void getClubProfileReturnsSeparatedAppAndClubProfiles() {
        ClubCreateResponse created = clubService.createClub(
                "user-club-007",
                "Profile Owner",
                new CreateClubRequest(
                        "Semo Club Profile",
                        "클럽 프로필 테스트",
                        "OTHER",
                        "PUBLIC",
                        "APPROVAL",
                        null
                )
        );

        ClubProfileResponse response = clubService.getClubProfile(created.clubId(), "user-club-007");

        assertThat(response.appProfile().displayName()).isEqualTo("Profile Owner");
        assertThat(response.clubProfile().displayName()).isEqualTo("Profile Owner");
        assertThat(response.clubProfile().roleCode()).isEqualTo("OWNER");
        assertThat(response.clubProfile().joinedLabel()).isNotBlank();
    }

    @Test
    void updateClubProfileCanRemoveAvatar() {
        ClubCreateResponse created = clubService.createClub(
                "user-club-009",
                "Avatar Owner",
                new CreateClubRequest(
                        "Semo Avatar Club",
                        "아바타 제거 테스트",
                        "OTHER",
                        "PUBLIC",
                        "APPROVAL",
                        null
                )
        );

        ClubProfile current = clubProfileRepository.findAll().getFirst();
        clubProfileRepository.save(ClubProfile.builder()
                .clubProfileId(current.getClubProfileId())
                .clubMemberId(current.getClubMemberId())
                .displayName(current.getDisplayName())
                .tagline(current.getTagline())
                .introText(current.getIntroText())
                .avatarFileName("semo/club-profiles/existing-avatar.png")
                .build());

        ClubProfileResponse response = clubService.updateClubProfile(
                created.clubId(),
                "user-club-009",
                new UpdateClubProfileRequest(null, null, true)
        );

        assertThat(response.clubProfile().avatarFileName()).isNull();
        assertThat(response.clubProfile().avatarImageUrl()).isNull();
        assertThat(clubProfileRepository.findAll().getFirst().getAvatarFileName()).isNull();
    }

    @Test
    void updateClubProfileCanChangeDisplayName() {
        ClubCreateResponse created = clubService.createClub(
                "user-club-011",
                "Nickname Owner",
                new CreateClubRequest(
                        "Semo Nickname Club",
                        "닉네임 변경 테스트",
                        "OTHER",
                        "PUBLIC",
                        "APPROVAL",
                        null
                )
        );

        ClubProfileResponse response = clubService.updateClubProfile(
                created.clubId(),
                "user-club-011",
                new UpdateClubProfileRequest("테니스왕", null, false)
        );

        assertThat(response.clubProfile().displayName()).isEqualTo("테니스왕");
        assertThat(clubProfileRepository.findAll().getFirst().getDisplayName()).isEqualTo("테니스왕");
    }
}
