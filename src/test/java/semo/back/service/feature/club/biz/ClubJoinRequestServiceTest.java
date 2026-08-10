package semo.back.service.feature.club.biz;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import semo.back.service.common.exception.SemoException;
import semo.back.service.database.pub.entity.ClubJoinRequest;
import semo.back.service.database.pub.entity.ClubMember;
import semo.back.service.database.pub.entity.ClubProfile;
import semo.back.service.database.pub.entity.ProfileUser;
import semo.back.service.database.pub.repository.ClubAttendanceCheckInRepository;
import semo.back.service.database.pub.repository.ClubAttendanceSessionRepository;
import semo.back.service.database.pub.repository.ClubEventParticipantRepository;
import semo.back.service.database.pub.repository.ClubFeatureRepository;
import semo.back.service.database.pub.repository.ClubJoinRequestRepository;
import semo.back.service.database.pub.repository.ClubMemberRepository;
import semo.back.service.database.pub.repository.ClubProfileRepository;
import semo.back.service.database.pub.repository.ClubRepository;
import semo.back.service.database.pub.repository.ClubScheduleEventRepository;
import semo.back.service.database.pub.repository.ClubScheduleVoteOptionRepository;
import semo.back.service.database.pub.repository.ClubScheduleVoteRepository;
import semo.back.service.database.pub.repository.ClubScheduleVoteSelectionRepository;
import semo.back.service.database.pub.repository.ProfileUserRepository;
import semo.back.service.feature.club.vo.ClubDiscoverResponse;
import semo.back.service.feature.club.vo.ClubJoinActionResponse;
import semo.back.service.feature.club.vo.ClubJoinRequestInboxResponse;
import semo.back.service.feature.club.vo.CreateClubRequest;
import semo.back.service.feature.club.vo.ReviewClubJoinRequestRequest;
import semo.back.service.feature.club.vo.SubmitClubJoinRequestRequest;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class ClubJoinRequestServiceTest {

    @Autowired
    private ClubJoinRequestService clubJoinRequestService;

    @Autowired
    private ClubService clubService;

    @Autowired
    private ClubJoinRequestRepository clubJoinRequestRepository;

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
        cleanup();
    }

    @AfterEach
    void tearDown() {
        cleanup();
    }

    @Test
    void submitJoinRequestCreatesPendingRequestForApprovalClub() {
        Long clubId = clubService.createClub(
                "owner-join-001",
                "Owner Join",
                new CreateClubRequest("Approval Club", null, "OTHER", "PUBLIC", "APPROVAL", null)
        ).clubId();
        ProfileUser applicant = createProfileUser("applicant-join-001", "가입 신청자");

        ClubJoinActionResponse response = clubJoinRequestService.submitJoinRequest(
                clubId,
                applicant.getUserKey(),
                new SubmitClubJoinRequestRequest("테니스 모임 활동에 참여하고 싶습니다.")
        );

        Optional<ClubJoinRequest> storedRequest = clubJoinRequestRepository.findByClubIdAndProfileId(clubId, applicant.getProfileId());
        assertThat(response.actionType()).isEqualTo("REQUESTED");
        assertThat(response.joinStatus()).isEqualTo("PENDING");
        assertThat(storedRequest).isPresent();
        assertThat(storedRequest.orElseThrow().getRequestStatus()).isEqualTo("PENDING");
        assertThat(storedRequest.orElseThrow().getRequestMessage()).isEqualTo("테니스 모임 활동에 참여하고 싶습니다.");
        assertThat(clubMemberRepository.findByClubIdAndProfileId(clubId, applicant.getProfileId())).isEmpty();
    }

    @Test
    void submitJoinRequestJoinsImmediatelyForOpenClub() {
        Long clubId = clubService.createClub(
                "owner-join-002",
                "Owner Open",
                new CreateClubRequest("Open Club", null, "RUNNING", "PUBLIC", "OPEN", null)
        ).clubId();
        ProfileUser applicant = createProfileUser("applicant-join-002", "즉시 가입자");

        ClubJoinActionResponse response = clubJoinRequestService.submitJoinRequest(
                clubId,
                applicant.getUserKey(),
                new SubmitClubJoinRequestRequest("바로 가입 테스트")
        );

        ClubMember membership = clubMemberRepository.findByClubIdAndProfileId(clubId, applicant.getProfileId()).orElseThrow();
        assertThat(response.actionType()).isEqualTo("JOINED");
        assertThat(response.joinStatus()).isEqualTo("ACTIVE");
        assertThat(response.clubMemberId()).isEqualTo(membership.getClubMemberId());
        assertThat(membership.getMembershipStatus()).isEqualTo("ACTIVE");
        assertThat(clubProfileRepository.findByClubMemberId(membership.getClubMemberId())).isPresent();
        assertThat(clubJoinRequestRepository.findByClubIdAndProfileId(clubId, applicant.getProfileId())).isEmpty();
    }

    @Test
    void cancelMyJoinRequestMarksPendingRequestCanceled() {
        Long clubId = clubService.createClub(
                "owner-join-003",
                "Owner Cancel",
                new CreateClubRequest("Cancel Club", null, "OTHER", "PUBLIC", "APPROVAL", null)
        ).clubId();
        ProfileUser applicant = createProfileUser("applicant-join-003", "취소 신청자");

        clubJoinRequestService.submitJoinRequest(
                clubId,
                applicant.getUserKey(),
                new SubmitClubJoinRequestRequest("가입 취소 테스트")
        );

        ClubJoinActionResponse response = clubJoinRequestService.cancelMyJoinRequest(clubId, applicant.getUserKey());

        ClubJoinRequest storedRequest = clubJoinRequestRepository.findByClubIdAndProfileId(clubId, applicant.getProfileId()).orElseThrow();
        assertThat(response.actionType()).isEqualTo("CANCELED");
        assertThat(response.joinStatus()).isEqualTo("CANCELED");
        assertThat(storedRequest.getRequestStatus()).isEqualTo("CANCELED");
    }

    @Test
    void reviewJoinRequestApprovesApplicantAndCreatesMembership() {
        Long clubId = clubService.createClub(
                "owner-join-004",
                "Owner Approver",
                new CreateClubRequest("Review Club", null, "OTHER", "PUBLIC", "APPROVAL", null)
        ).clubId();
        ProfileUser owner = profileUserRepository.findByUserKey("owner-join-004").orElseThrow();
        ProfileUser applicant = createProfileUser("applicant-join-004", "승인 신청자");

        clubJoinRequestService.submitJoinRequest(
                clubId,
                applicant.getUserKey(),
                new SubmitClubJoinRequestRequest("승인 부탁드립니다.")
        );
        ClubJoinRequest joinRequest = clubJoinRequestRepository.findByClubIdAndProfileId(clubId, applicant.getProfileId()).orElseThrow();

        ClubJoinActionResponse response = clubJoinRequestService.reviewJoinRequest(
                clubId,
                joinRequest.getClubJoinRequestId(),
                owner.getUserKey(),
                new ReviewClubJoinRequestRequest("APPROVED")
        );

        ClubJoinRequest reviewedRequest = clubJoinRequestRepository.findById(joinRequest.getClubJoinRequestId()).orElseThrow();
        ClubMember membership = clubMemberRepository.findByClubIdAndProfileId(clubId, applicant.getProfileId()).orElseThrow();
        assertThat(response.actionType()).isEqualTo("APPROVED");
        assertThat(response.joinStatus()).isEqualTo("ACTIVE");
        assertThat(reviewedRequest.getRequestStatus()).isEqualTo("APPROVED");
        assertThat(reviewedRequest.getReviewedByProfileId()).isEqualTo(owner.getProfileId());
        assertThat(membership.getMembershipStatus()).isEqualTo("ACTIVE");
        assertThat(clubProfileRepository.findByClubMemberId(membership.getClubMemberId())).isPresent();
    }

    @Test
    void getDiscoverClubsExcludesJoinedClubsAndReflectsPendingRequestStatus() {
        clubService.createClub(
                "discover-user-001",
                "Discover Owner",
                new CreateClubRequest("My Tennis Club", null, "TENNIS", "PUBLIC", "APPROVAL", null)
        );

        Long pendingClubId = clubService.createClub(
                "owner-discover-002",
                "Owner Candidate",
                new CreateClubRequest("Tennis Friends", "비슷한 테니스 모임", "TENNIS", "PUBLIC", "APPROVAL", null)
        ).clubId();

        Long openClubId = clubService.createClub(
                "owner-discover-003",
                "Owner Open",
                new CreateClubRequest("Running Crew", "러닝 모임", "RUNNING", "PUBLIC", "OPEN", null)
        ).clubId();

        clubService.createClub(
                "owner-discover-004",
                "Owner Private",
                new CreateClubRequest("Private Tennis", "비공개 모임", "TENNIS", "PRIVATE", "APPROVAL", null)
        );

        clubJoinRequestService.submitJoinRequest(
                pendingClubId,
                "discover-user-001",
                new SubmitClubJoinRequestRequest("같은 카테고리라 신청합니다.")
        );

        ClubDiscoverResponse response = clubJoinRequestService.getDiscoverClubs("discover-user-001", null);

        assertThat(response.recommended()).isTrue();
        assertThat(response.recommendationLabel()).isEqualTo("내 클럽과 비슷한 활동 태그 우선");
        assertThat(response.clubs()).hasSize(2);
        assertThat(response.clubs()).noneMatch(club -> "My Tennis Club".equals(club.name()));
        assertThat(response.clubs()).noneMatch(club -> "Private Tennis".equals(club.name()));
        assertThat(response.clubs().getFirst().clubId()).isEqualTo(pendingClubId);
        assertThat(response.clubs().getFirst().joinStatus()).isEqualTo("PENDING");
        assertThat(response.clubs().getFirst().recommendedByCategory()).isTrue();
        assertThat(response.clubs().getFirst().recommendedByTags()).isTrue();
        assertThat(response.clubs().stream().anyMatch(club -> club.clubId().equals(openClubId) && "NONE".equals(club.joinStatus()))).isTrue();
    }

    @Test
    void getDiscoverClubsCanSearchByRegionLabel() {
        clubService.createClub(
                "owner-discover-101",
                "Region Owner",
                new CreateClubRequest(
                        "Han River Runners",
                        "서울 동남권 러닝 모임",
                        "RUNNING",
                        null,
                        null,
                        null,
                        "PUBLIC",
                        "APPROVAL",
                        "OFFLINE",
                        "11",
                        "11710",
                        "서울특별시",
                        "송파구",
                        null
                )
        );
        createProfileUser("discover-user-101", "지역 검색 사용자");

        ClubDiscoverResponse response = clubJoinRequestService.getDiscoverClubs("discover-user-101", "서울");

        assertThat(response.query()).isEqualTo("서울");
        assertThat(response.clubs()).hasSize(1);
        assertThat(response.clubs().getFirst().name()).isEqualTo("Han River Runners");
        assertThat(response.clubs().getFirst().regionDepth1Code()).isEqualTo("11");
        assertThat(response.clubs().getFirst().regionDepth2Code()).isEqualTo("11710");
        assertThat(response.clubs().getFirst().regionLabel()).isEqualTo("서울특별시 송파구");
        assertThat(response.clubs().getFirst().activityCategory()).isEqualTo("SPORTS");
        assertThat(response.clubs().getFirst().activityTags()).containsExactly("RUNNING");
    }

    @Test
    void getJoinRequestInboxRejectsRegularMembers() {
        Long clubId = clubService.createClub(
                "owner-join-201",
                "Owner Queue",
                new CreateClubRequest("Queue Club", null, "OTHER", "PUBLIC", "APPROVAL", null)
        ).clubId();
        createActiveMember(clubId, "member-join-201", "열람 멤버");
        ProfileUser applicant = createProfileUser("applicant-join-201", "신규 신청자");

        clubJoinRequestService.submitJoinRequest(
                clubId,
                applicant.getUserKey(),
                new SubmitClubJoinRequestRequest("가입 대기열 테스트")
        );

        assertThatThrownBy(() -> clubJoinRequestService.getJoinRequestInbox(
                clubId,
                "member-join-201"
        ))
                .isInstanceOf(SemoException.ForbiddenException.class);
    }

    @Test
    void getAdminJoinRequestInboxMarksAdminViewer() {
        Long clubId = clubService.createClub(
                "owner-join-202",
                "Owner Queue Admin",
                new CreateClubRequest("Queue Admin Club", null, "OTHER", "PUBLIC", "APPROVAL", null)
        ).clubId();
        ProfileUser applicant = createProfileUser("applicant-join-202", "관리자 확인 신청자");

        clubJoinRequestService.submitJoinRequest(
                clubId,
                applicant.getUserKey(),
                new SubmitClubJoinRequestRequest("운영진 검토 부탁드립니다.")
        );

        ClubJoinRequestInboxResponse response = clubJoinRequestService.getAdminJoinRequestInbox(
                clubId,
                "owner-join-202"
        );

        assertThat(response.admin()).isTrue();
        assertThat(response.pendingRequestCount()).isEqualTo(1);
        assertThat(response.latestRequestedAtLabel()).isNotBlank();
    }

    private void cleanup() {
        clubJoinRequestRepository.deleteAll();
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

    private ProfileUser createProfileUser(String userKey, String displayName) {
        return profileUserRepository.save(ProfileUser.builder()
                .userKey(userKey)
                .displayName(displayName)
                .tagline(displayName + " 소개")
                .profileColor("#135bec")
                .build());
    }

    private void createActiveMember(Long clubId, String userKey, String displayName) {
        ProfileUser profileUser = createProfileUser(userKey, displayName);
        ClubMember member = clubMemberRepository.save(ClubMember.builder()
                .clubId(clubId)
                .profileId(profileUser.getProfileId())
                .roleCode("MEMBER")
                .membershipStatus("ACTIVE")
                .joinedAt(LocalDateTime.now().minusDays(3))
                .lastActivityAt(LocalDateTime.now().minusHours(4))
                .build());
        clubProfileRepository.save(ClubProfile.builder()
                .clubMemberId(member.getClubMemberId())
                .displayName(displayName)
                .tagline(displayName + " 태그라인")
                .introText(null)
                .avatarFileName(null)
                .build());
    }
}
