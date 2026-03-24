package semo.back.service.support;

import semo.back.service.database.pub.entity.DashboardWidgetCatalog;
import semo.back.service.database.pub.entity.FeatureCatalog;
import semo.back.service.database.pub.repository.DashboardWidgetCatalogRepository;
import semo.back.service.database.pub.repository.FeatureCatalogRepository;

import java.util.List;

public final class TestCatalogSeeder {
    private TestCatalogSeeder() {
    }

    public static void seedFeatureCatalogs(FeatureCatalogRepository featureCatalogRepository) {
        featureCatalogRepository.deleteAll();
        featureCatalogRepository.saveAll(List.of(
                FeatureCatalog.builder()
                        .featureKey("ATTENDANCE")
                        .displayName("출석 체크")
                        .description("멤버 출석을 체크하고 출석 세션을 관리합니다.")
                        .iconName("fact_check")
                        .navigationScope("USER_AND_ADMIN")
                        .active(true)
                        .sortOrder(10)
                        .build(),
                FeatureCatalog.builder()
                        .featureKey("TIMELINE")
                        .displayName("타임라인")
                        .description("모임 전체 활동을 시간순 타임라인으로 확인합니다.")
                        .iconName("timeline")
                        .navigationScope("USER_AND_ADMIN")
                        .active(true)
                        .sortOrder(20)
                        .build(),
                FeatureCatalog.builder()
                        .featureKey("NOTICE")
                        .displayName("공지관리")
                        .description("공지 콘텐츠를 작성하고 게시판/캘린더에 공유합니다.")
                        .iconName("campaign")
                        .navigationScope("USER_AND_ADMIN")
                        .active(true)
                        .sortOrder(30)
                        .build(),
                FeatureCatalog.builder()
                        .featureKey("POLL")
                        .displayName("투표")
                        .description("모임 투표를 작성, 공유, 관리합니다.")
                        .iconName("poll")
                        .navigationScope("USER_AND_ADMIN")
                        .active(true)
                        .sortOrder(40)
                        .build(),
                FeatureCatalog.builder()
                        .featureKey("SCHEDULE_MANAGE")
                        .displayName("일정관리")
                        .description("일정 콘텐츠를 작성하고 게시판/캘린더에 공유합니다.")
                        .iconName("edit_calendar")
                        .navigationScope("USER_AND_ADMIN")
                        .active(true)
                        .sortOrder(50)
                        .build(),
                FeatureCatalog.builder()
                        .featureKey("ROLE_MANAGEMENT")
                        .displayName("직책관리")
                        .description("직책을 생성하고 하위 권한을 연결해 멤버 권한을 세밀하게 관리합니다.")
                        .iconName("manage_accounts")
                        .navigationScope("ADMIN_ONLY")
                        .active(true)
                        .sortOrder(60)
                        .build()
        ));
    }

    public static void seedDashboardWidgetCatalogs(DashboardWidgetCatalogRepository dashboardWidgetCatalogRepository) {
        dashboardWidgetCatalogRepository.deleteAll();
        dashboardWidgetCatalogRepository.saveAll(List.of(
                DashboardWidgetCatalog.builder()
                        .widgetKey("BOARD_NOTICE")
                        .displayName("Board Notice")
                        .description("Latest announcements from your board.")
                        .iconName("forum")
                        .requiredFeatureKey(null)
                        .defaultVisibilityScope("USER_HOME")
                        .defaultColumnSpan(2)
                        .defaultRowSpan(1)
                        .defaultSortOrder(10)
                        .active(true)
                        .build(),
                DashboardWidgetCatalog.builder()
                        .widgetKey("SCHEDULE_OVERVIEW")
                        .displayName("Schedule Overview")
                        .description("Upcoming schedules and next events.")
                        .iconName("calendar_month")
                        .requiredFeatureKey(null)
                        .defaultVisibilityScope("USER_HOME")
                        .defaultColumnSpan(1)
                        .defaultRowSpan(1)
                        .defaultSortOrder(20)
                        .active(true)
                        .build(),
                DashboardWidgetCatalog.builder()
                        .widgetKey("PROFILE_SUMMARY")
                        .displayName("My Profile")
                        .description("Quick access to your club profile.")
                        .iconName("person")
                        .requiredFeatureKey(null)
                        .defaultVisibilityScope("USER_HOME")
                        .defaultColumnSpan(1)
                        .defaultRowSpan(1)
                        .defaultSortOrder(30)
                        .active(true)
                        .build(),
                DashboardWidgetCatalog.builder()
                        .widgetKey("ATTENDANCE_STATUS")
                        .displayName("Attendance Check")
                        .description("Check in and review attendance status.")
                        .iconName("fact_check")
                        .requiredFeatureKey("ATTENDANCE")
                        .defaultVisibilityScope("USER_HOME")
                        .defaultColumnSpan(1)
                        .defaultRowSpan(1)
                        .defaultSortOrder(40)
                        .active(true)
                        .build()
        ));
    }
}
