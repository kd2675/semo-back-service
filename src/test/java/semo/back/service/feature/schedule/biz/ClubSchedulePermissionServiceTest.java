package semo.back.service.feature.schedule.biz;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import semo.back.service.common.exception.SemoException;
import semo.back.service.database.pub.entity.ClubMember;
import semo.back.service.database.pub.entity.ClubMemberPosition;
import semo.back.service.database.pub.entity.ClubPosition;
import semo.back.service.database.pub.entity.ClubPositionPermission;
import semo.back.service.database.pub.entity.ProfileUser;
import semo.back.service.database.pub.repository.ClubFeatureRepository;
import semo.back.service.database.pub.repository.ClubEventParticipantRepository;
import semo.back.service.database.pub.repository.ClubMemberRepository;
import semo.back.service.database.pub.repository.ClubMemberPositionRepository;
import semo.back.service.database.pub.repository.ClubProfileRepository;
import semo.back.service.database.pub.repository.ClubPositionPermissionRepository;
import semo.back.service.database.pub.repository.ClubPositionRepository;
import semo.back.service.database.pub.repository.ClubRepository;
import semo.back.service.database.pub.repository.ClubScheduleEventRepository;
import semo.back.service.database.pub.repository.FeatureCatalogRepository;
import semo.back.service.database.pub.repository.ProfileUserRepository;
import semo.back.service.feature.club.biz.ClubService;
import semo.back.service.feature.club.vo.CreateClubRequest;
import semo.back.service.feature.clubfeature.biz.ClubFeatureService;
import semo.back.service.feature.clubfeature.vo.UpdateClubFeaturesRequest;
import semo.back.service.feature.position.biz.ClubPositionPermissionEvaluator;
import semo.back.service.feature.position.biz.ClubCapability;
import semo.back.service.feature.schedule.vo.UpsertScheduleEventRequest;
import semo.back.service.feature.schedule.vo.UpdateScheduleEventAttendanceRequest;
import semo.back.service.feature.schedule.vo.UpdateScheduleEventParticipationRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static semo.back.service.support.TestCatalogSeeder.seedFeatureCatalogs;

@SpringBootTest
@ActiveProfiles("test")
class ClubSchedulePermissionServiceTest {

    @Autowired
    private ClubService clubService;

    @Autowired
    private ClubFeatureService clubFeatureService;

    @Autowired
    private ClubScheduleService clubScheduleService;

    @Autowired
    private ClubScheduleAttendanceService clubScheduleAttendanceService;

    @Autowired
    private ClubEventParticipantRepository clubEventParticipantRepository;

    @Autowired
    private ClubScheduleEventRepository clubScheduleEventRepository;

    @Autowired
    private ClubMemberPositionRepository clubMemberPositionRepository;

    @Autowired
    private ClubPositionRepository clubPositionRepository;

    @Autowired
    private ClubPositionPermissionRepository clubPositionPermissionRepository;

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

    @Autowired
    private FeatureCatalogRepository featureCatalogRepository;

    @BeforeEach
    void setUp() {
        clubEventParticipantRepository.deleteAll();
        clubScheduleEventRepository.deleteAll();
        clubMemberPositionRepository.deleteAll();
        clubPositionPermissionRepository.deleteAll();
        clubPositionRepository.deleteAll();
        clubFeatureRepository.deleteAll();
        clubProfileRepository.deleteAll();
        clubMemberRepository.deleteAll();
        clubRepository.deleteAll();
        profileUserRepository.deleteAll();
        seedFeatureCatalogs(featureCatalogRepository);
    }

    @AfterEach
    void tearDown() {
        clubEventParticipantRepository.deleteAll();
        clubScheduleEventRepository.deleteAll();
        clubMemberPositionRepository.deleteAll();
        clubPositionPermissionRepository.deleteAll();
        clubPositionRepository.deleteAll();
        clubFeatureRepository.deleteAll();
        clubProfileRepository.deleteAll();
        clubMemberRepository.deleteAll();
        clubRepository.deleteAll();
        profileUserRepository.deleteAll();
    }

    @Test
    void memberScheduleCreateUpdateDeleteFollowAssignedPositionPermissions() {
        String ownerUserKey = "schedule-policy-owner-001";
        String memberUserKey = "schedule-policy-member-001";
        Long clubId = createScheduleClub(ownerUserKey, "Schedule Permission Lab");
        ClubMember member = addActiveMember(clubId, memberUserKey, "Schedule Member");

        assertThatThrownBy(() -> clubScheduleService.createScheduleEvent(
                clubId,
                memberUserKey,
                eventRequest("멤버 일정", "기본 설정에서는 작성 불가")
        ))
                .isInstanceOf(SemoException.ForbiddenException.class)
                .hasMessageContaining("일정 작성 권한");

        assignPositionPermissions(
                clubId,
                member,
                ClubPositionPermissionEvaluator.PERMISSION_SCHEDULE_CREATE
        );

        var created = clubScheduleService.createScheduleEvent(
                clubId,
                memberUserKey,
                eventRequest("멤버 일정", "생성 허용 후 작성")
        );

        assertThat(created.eventId()).isNotNull();

        assertThatThrownBy(() -> clubScheduleService.updateScheduleEvent(
                clubId,
                created.eventId(),
                memberUserKey,
                eventRequest("멤버 일정 수정", "수정은 아직 불가")
        ))
                .isInstanceOf(SemoException.ForbiddenException.class)
                .hasMessageContaining("일정 수정 권한");

        assertThatThrownBy(() -> clubScheduleService.deleteScheduleEvent(
                clubId,
                created.eventId(),
                memberUserKey
        ))
                .isInstanceOf(SemoException.ForbiddenException.class)
                .hasMessageContaining("일정 삭제 권한");

        var adminUpdated = clubScheduleService.updateScheduleEvent(
                clubId,
                created.eventId(),
                ownerUserKey,
                eventRequest("관리자가 수정한 일정", "관리자는 계속 수정 가능")
        );

        assertThat(adminUpdated.title()).isEqualTo("관리자가 수정한 일정");

        assignPositionPermissions(
                clubId,
                member,
                ClubPositionPermissionEvaluator.PERMISSION_SCHEDULE_CREATE,
                ClubPositionPermissionEvaluator.PERMISSION_SCHEDULE_UPDATE_SELF,
                ClubPositionPermissionEvaluator.PERMISSION_SCHEDULE_DELETE_SELF
        );

        var memberUpdated = clubScheduleService.updateScheduleEvent(
                clubId,
                created.eventId(),
                memberUserKey,
                eventRequest("멤버가 수정한 일정", "권한 허용 후 수정")
        );

        assertThat(memberUpdated.title()).isEqualTo("멤버가 수정한 일정");

        clubScheduleService.deleteScheduleEvent(clubId, created.eventId(), memberUserKey);

        assertThat(clubScheduleEventRepository.findByEventIdAndClubId(created.eventId(), clubId)).isEmpty();
    }

    @Test
    void delegatedAttendanceManagerCanVerifyEventAttendance() {
        String ownerUserKey = "schedule-attendance-policy-owner-001";
        String memberUserKey = "schedule-attendance-policy-member-001";
        Long clubId = createScheduleClub(ownerUserKey, "Schedule Attendance Permission Lab");
        ClubMember member = addActiveMember(clubId, memberUserKey, "Attendance Manager");
        var created = clubScheduleService.createScheduleEvent(
                clubId,
                ownerUserKey,
                attendanceEventRequest("권한 위임 출석 일정")
        );
        clubScheduleService.updateScheduleEventParticipation(
                clubId,
                created.eventId(),
                memberUserKey,
                new UpdateScheduleEventParticipationRequest("GOING")
        );
        Long memberClubProfileId = clubEventParticipantRepository
                .findByEventIdIn(List.of(created.eventId()))
                .getFirst()
                .getClubProfileId();

        assertThatThrownBy(() -> clubScheduleAttendanceService.getEventAttendance(
                clubId,
                created.eventId(),
                memberUserKey
        ))
                .isInstanceOf(SemoException.ForbiddenException.class)
                .hasMessageContaining("출석 관리 권한");

        assignPositionPermissions(
                clubId,
                member,
                ClubPositionPermissionEvaluator.PERMISSION_ATTENDANCE_MANAGE
        );

        var attendance = clubScheduleAttendanceService.updateEventAttendance(
                clubId,
                created.eventId(),
                memberClubProfileId,
                memberUserKey,
                new UpdateScheduleEventAttendanceRequest("LATE", "교통 지연 확인")
        );

        assertThat(attendance.canManage()).isTrue();
        assertThat(attendance.summary().lateCount()).isEqualTo(1);
        assertThat(attendance.members())
                .filteredOn(item -> item.clubProfileId().equals(memberClubProfileId))
                .singleElement()
                .satisfies(item -> {
                    assertThat(item.attendanceStatus()).isEqualTo("LATE");
                    assertThat(item.attendanceNote()).isEqualTo("교통 지연 확인");
                });
    }

    private Long createScheduleClub(String ownerUserKey, String clubName) {
        Long clubId = clubService.createClub(
                ownerUserKey,
                "Schedule Owner",
                new CreateClubRequest(
                        clubName,
                        "일정 권한 테스트",
                        "OTHER",
                        "PUBLIC",
                        "APPROVAL",
                        null
                )
        ).clubId();

        clubFeatureService.updateClubFeatures(
                clubId,
                ownerUserKey,
            new UpdateClubFeaturesRequest(List.of("SCHEDULE_MANAGE", "ATTENDANCE", "ROLE_MANAGEMENT"))
        );
        return clubId;
    }

    private ClubMember addActiveMember(Long clubId, String userKey, String displayName) {
        ProfileUser profileUser = profileUserRepository.save(ProfileUser.builder()
                .userKey(userKey)
                .displayName(displayName)
                .tagline(displayName + " 소개")
                .profileColor("#135bec")
                .build());

        ClubMember member = clubMemberRepository.save(ClubMember.builder()
                .clubId(clubId)
                .profileId(profileUser.getProfileId())
                .roleCode("MEMBER")
                .membershipStatus("ACTIVE")
                .joinedAt(LocalDateTime.now())
                .lastActivityAt(LocalDateTime.now())
                .build());
        clubProfileRepository.save(semo.back.service.database.pub.entity.ClubProfile.builder()
                .clubMemberId(member.getClubMemberId())
                .displayName(displayName)
                .build());
        return member;
    }

    private void assignPositionPermissions(Long clubId, ClubMember member, ClubCapability... capabilities) {
        long nextCodeSuffix = clubPositionRepository.count() + 1;
        ClubPosition position = clubPositionRepository.save(ClubPosition.builder()
                .clubId(clubId)
                .positionCode("SCHEDULE_EDITOR_" + nextCodeSuffix)
                .displayName("일정 담당")
                .description("일정 생성 및 본인 일정 관리")
                .iconName("calendar_month")
                .colorHex("#0053dd")
                .active(true)
                .build());

        for (ClubCapability capability : capabilities) {
            clubPositionPermissionRepository.save(ClubPositionPermission.builder()
                    .clubPositionId(position.getClubPositionId())
                    .permissionKey(capability.permissionKey())
                    .build());
        }

        clubMemberPositionRepository.save(ClubMemberPosition.builder()
                .clubMemberId(member.getClubMemberId())
                .clubPositionId(position.getClubPositionId())
                .assignedAt(LocalDateTime.now())
                .build());
    }

    private UpsertScheduleEventRequest eventRequest(String title, String locationLabel) {
        return new UpsertScheduleEventRequest(
                title,
                "2030-05-01",
                null,
                null,
                null,
                null,
                locationLabel,
                null,
                false,
                false,
                null,
                false,
                false,
                false,
                true,
                false
        );
    }

    private UpsertScheduleEventRequest attendanceEventRequest(String title) {
        return new UpsertScheduleEventRequest(
                title,
                "2030-05-01",
                null,
                null,
                null,
                null,
                "체육관",
                null,
                true,
                false,
                null,
                false,
                false,
                false,
                true,
                false
        );
    }
}
