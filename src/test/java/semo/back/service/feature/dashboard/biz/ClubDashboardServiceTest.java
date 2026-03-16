package semo.back.service.feature.dashboard.biz;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import semo.back.service.common.exception.SemoException;
import semo.back.service.database.pub.repository.ClubAttendanceCheckInRepository;
import semo.back.service.database.pub.repository.ClubAttendanceSessionRepository;
import semo.back.service.database.pub.repository.ClubDashboardWidgetRepository;
import semo.back.service.database.pub.repository.ClubEventParticipantRepository;
import semo.back.service.database.pub.repository.ClubFeatureRepository;
import semo.back.service.database.pub.repository.ClubMemberRepository;
import semo.back.service.database.pub.repository.ClubProfileRepository;
import semo.back.service.database.pub.repository.ClubRepository;
import semo.back.service.database.pub.repository.ClubScheduleEventRepository;
import semo.back.service.database.pub.repository.ClubScheduleVoteOptionRepository;
import semo.back.service.database.pub.repository.ClubScheduleVoteRepository;
import semo.back.service.database.pub.repository.ClubScheduleVoteSelectionRepository;
import semo.back.service.database.pub.repository.DashboardWidgetCatalogRepository;
import semo.back.service.database.pub.repository.ProfileUserRepository;
import semo.back.service.feature.club.biz.ClubService;
import semo.back.service.feature.club.vo.CreateClubRequest;
import semo.back.service.feature.clubfeature.biz.ClubFeatureService;
import semo.back.service.feature.clubfeature.vo.UpdateClubFeaturesRequest;
import semo.back.service.feature.dashboard.vo.UpdateClubDashboardLayoutRequest;
import semo.back.service.feature.dashboard.vo.UpdateClubDashboardWidgetItemRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class ClubDashboardServiceTest {

    @Autowired
    private ClubDashboardService clubDashboardService;

    @Autowired
    private ClubService clubService;

    @Autowired
    private ClubFeatureService clubFeatureService;

    @Autowired
    private ClubAttendanceCheckInRepository clubAttendanceCheckInRepository;

    @Autowired
    private ClubAttendanceSessionRepository clubAttendanceSessionRepository;

    @Autowired
    private ClubDashboardWidgetRepository clubDashboardWidgetRepository;

    @Autowired
    private ClubEventParticipantRepository clubEventParticipantRepository;

    @Autowired
    private DashboardWidgetCatalogRepository dashboardWidgetCatalogRepository;

    @Autowired
    private ClubFeatureRepository clubFeatureRepository;

    @Autowired
    private ClubProfileRepository clubProfileRepository;

    @Autowired
    private ClubMemberRepository clubMemberRepository;

    @Autowired
    private ClubRepository clubRepository;

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
        clubDashboardWidgetRepository.deleteAll();
        clubFeatureRepository.deleteAll();
        dashboardWidgetCatalogRepository.deleteAll();
        clubProfileRepository.deleteAll();
        clubMemberRepository.deleteAll();
        clubRepository.deleteAll();
        profileUserRepository.deleteAll();
    }

    @Test
    void dashboardWidgetsIncludeFeatureWidgetAfterFeatureEnabledAndAdded() {
        String ownerUserKey = "dashboard-owner-001";
        Long clubId = clubService.createClub(
                ownerUserKey,
                "Dashboard Owner",
                new CreateClubRequest(
                        "Dashboard Club",
                        "홈 위젯 테스트",
                        "OTHER",
                        "PUBLIC",
                        "APPROVAL",
                        null
                )
        ).clubId();

        var initialWidgets = clubDashboardService.getDashboardWidgets(clubId, ownerUserKey, "USER_HOME");
        assertThat(initialWidgets).extracting("widgetKey")
                .containsExactly("BOARD_NOTICE", "SCHEDULE_OVERVIEW", "PROFILE_SUMMARY");

        clubFeatureService.updateClubFeatures(
                clubId,
                ownerUserKey,
                new UpdateClubFeaturesRequest(List.of("ATTENDANCE"))
        );

        var editorBeforeAdd = clubDashboardService.getDashboardWidgetEditor(clubId, ownerUserKey, "USER_HOME");
        assertThat(editorBeforeAdd.widgets())
                .filteredOn(widget -> widget.widgetKey().equals("ATTENDANCE_STATUS"))
                .singleElement()
                .satisfies(widget -> {
                    assertThat(widget.available()).isTrue();
                    assertThat(widget.enabled()).isFalse();
                });

        List<UpdateClubDashboardWidgetItemRequest> updateItems = editorBeforeAdd.widgets().stream()
                .map(widget -> new UpdateClubDashboardWidgetItemRequest(
                        widget.widgetKey(),
                        widget.widgetKey().equals("ATTENDANCE_STATUS") || widget.enabled(),
                        widget.sortOrder(),
                        widget.columnSpan(),
                        widget.rowSpan(),
                        widget.title()
                ))
                .toList();

        clubDashboardService.updateDashboardWidgetLayout(
                clubId,
                ownerUserKey,
                new UpdateClubDashboardLayoutRequest("USER_HOME", updateItems)
        );

        var updatedWidgets = clubDashboardService.getDashboardWidgets(clubId, ownerUserKey, "USER_HOME");
        assertThat(updatedWidgets).extracting("widgetKey")
                .containsExactly("BOARD_NOTICE", "SCHEDULE_OVERVIEW", "PROFILE_SUMMARY", "ATTENDANCE_STATUS");
    }

    @Test
    void enablingFeatureWidgetFailsWhenFeatureIsDisabled() {
        String ownerUserKey = "dashboard-owner-002";
        Long clubId = clubService.createClub(
                ownerUserKey,
                "Dashboard Owner 2",
                new CreateClubRequest(
                        "Dashboard Club 2",
                        "홈 위젯 테스트",
                        "OTHER",
                        "PUBLIC",
                        "APPROVAL",
                        null
                )
        ).clubId();

        var editor = clubDashboardService.getDashboardWidgetEditor(clubId, ownerUserKey, "USER_HOME");
        var attendanceWidget = editor.widgets().stream()
                .filter(widget -> widget.widgetKey().equals("ATTENDANCE_STATUS"))
                .findFirst()
                .orElseThrow();

        assertThatThrownBy(() -> clubDashboardService.updateDashboardWidgetLayout(
                clubId,
                ownerUserKey,
                new UpdateClubDashboardLayoutRequest(
                        "USER_HOME",
                        List.of(new UpdateClubDashboardWidgetItemRequest(
                                "ATTENDANCE_STATUS",
                                true,
                                attendanceWidget.sortOrder(),
                                attendanceWidget.columnSpan(),
                                attendanceWidget.rowSpan(),
                                attendanceWidget.title()
                        ))
                )
        )).isInstanceOf(SemoException.ValidationException.class);
    }

    @Test
    void featureDisableHidesPreviouslyAddedFeatureWidgetImmediately() {
        String ownerUserKey = "dashboard-owner-003";
        Long clubId = clubService.createClub(
                ownerUserKey,
                "Dashboard Owner 3",
                new CreateClubRequest(
                        "Dashboard Club 3",
                        "홈 위젯 테스트",
                        "OTHER",
                        "PUBLIC",
                        "APPROVAL",
                        null
                )
        ).clubId();

        clubFeatureService.updateClubFeatures(
                clubId,
                ownerUserKey,
                new UpdateClubFeaturesRequest(List.of("ATTENDANCE"))
        );

        var editor = clubDashboardService.getDashboardWidgetEditor(clubId, ownerUserKey, "USER_HOME");
        List<UpdateClubDashboardWidgetItemRequest> enableAttendance = editor.widgets().stream()
                .map(widget -> new UpdateClubDashboardWidgetItemRequest(
                        widget.widgetKey(),
                        widget.widgetKey().equals("ATTENDANCE_STATUS") || widget.enabled(),
                        widget.sortOrder(),
                        widget.columnSpan(),
                        widget.rowSpan(),
                        widget.title()
                ))
                .toList();

        clubDashboardService.updateDashboardWidgetLayout(
                clubId,
                ownerUserKey,
                new UpdateClubDashboardLayoutRequest("USER_HOME", enableAttendance)
        );

        clubFeatureService.updateClubFeatures(
                clubId,
                ownerUserKey,
                new UpdateClubFeaturesRequest(List.of())
        );

        var userWidgets = clubDashboardService.getDashboardWidgets(clubId, ownerUserKey, "USER_HOME");
        assertThat(userWidgets).extracting("widgetKey")
                .doesNotContain("ATTENDANCE_STATUS");

        var editorAfterDisable = clubDashboardService.getDashboardWidgetEditor(clubId, ownerUserKey, "USER_HOME");
        assertThat(editorAfterDisable.widgets())
                .filteredOn(widget -> widget.widgetKey().equals("ATTENDANCE_STATUS"))
                .singleElement()
                .satisfies(widget -> {
                    assertThat(widget.available()).isFalse();
                    assertThat(widget.enabled()).isFalse();
                });
    }
}
