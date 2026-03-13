package semo.back.service.feature.attendance.biz;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import semo.back.service.database.pub.entity.ClubMember;
import semo.back.service.database.pub.repository.ClubAttendanceCheckInRepository;
import semo.back.service.database.pub.repository.ClubAttendanceSessionRepository;
import semo.back.service.database.pub.repository.ClubFeatureRepository;
import semo.back.service.database.pub.repository.ClubMemberRepository;
import semo.back.service.database.pub.repository.ClubProfileRepository;
import semo.back.service.database.pub.repository.ClubRepository;
import semo.back.service.database.pub.repository.ProfileUserRepository;
import semo.back.service.feature.attendance.vo.AttendanceCheckInRequest;
import semo.back.service.feature.attendance.vo.CreateAttendanceSessionRequest;
import semo.back.service.feature.club.biz.ClubService;
import semo.back.service.feature.club.vo.CreateClubRequest;
import semo.back.service.feature.clubfeature.biz.ClubFeatureService;
import semo.back.service.feature.clubfeature.vo.UpdateClubFeaturesRequest;
import semo.back.service.feature.profile.biz.ProfileUserService;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ClubAttendanceServiceTest {

    @Autowired
    private ClubAttendanceService clubAttendanceService;

    @Autowired
    private ClubService clubService;

    @Autowired
    private ClubFeatureService clubFeatureService;

    @Autowired
    private ProfileUserService profileUserService;

    @Autowired
    private ClubAttendanceCheckInRepository clubAttendanceCheckInRepository;

    @Autowired
    private ClubAttendanceSessionRepository clubAttendanceSessionRepository;

    @Autowired
    private ClubFeatureRepository clubFeatureRepository;

    @Autowired
    private ClubProfileRepository clubProfileRepository;

    @Autowired
    private ClubMemberRepository clubMemberRepository;

    @Autowired
    private ClubRepository clubRepository;

    @Autowired
    private ProfileUserRepository profileUserRepository;

    @BeforeEach
    void setUp() {
        clubAttendanceCheckInRepository.deleteAll();
        clubAttendanceSessionRepository.deleteAll();
        clubFeatureRepository.deleteAll();
        clubProfileRepository.deleteAll();
        clubMemberRepository.deleteAll();
        clubRepository.deleteAll();
        profileUserRepository.deleteAll();
    }

    @Test
    void memberCanCheckInToOpenAttendanceSession() {
        Long clubId = clubService.createClub(
                "attendance-owner-001",
                "Attendance Owner",
                new CreateClubRequest(
                        "Attendance Club",
                        "출석 테스트",
                        "OTHER",
                        "PUBLIC",
                        "APPROVAL",
                        null
                )
        ).clubId();

        clubFeatureService.updateClubFeatures(
                clubId,
                "attendance-owner-001",
                new UpdateClubFeaturesRequest(List.of("ATTENDANCE"))
        );

        Long memberProfileId = profileUserService.resolveProfileId("attendance-member-001", "Attendance Member");
        clubMemberRepository.save(ClubMember.builder()
                .clubId(clubId)
                .profileId(memberProfileId)
                .roleCode("MEMBER")
                .membershipStatus("ACTIVE")
                .joinedAt(LocalDateTime.now())
                .lastActivityAt(LocalDateTime.now())
                .build());

        var session = clubAttendanceService.createAttendanceSession(
                clubId,
                "attendance-owner-001",
                new CreateAttendanceSessionRequest("오늘 출석체크", null)
        );

        var checkedIn = clubAttendanceService.checkIn(
                clubId,
                "attendance-member-001",
                new AttendanceCheckInRequest(session.sessionId())
        );

        assertThat(checkedIn.checkedIn()).isTrue();
        assertThat(checkedIn.checkedInCount()).isEqualTo(1);
        assertThat(clubAttendanceCheckInRepository.count()).isOne();
    }
}
