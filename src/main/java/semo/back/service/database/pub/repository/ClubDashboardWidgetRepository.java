package semo.back.service.database.pub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import semo.back.service.database.pub.entity.ClubDashboardWidget;

import java.util.List;
import java.util.Optional;

public interface ClubDashboardWidgetRepository extends JpaRepository<ClubDashboardWidget, Long> {
    List<ClubDashboardWidget> findByClubIdOrderBySortOrderAscWidgetKeyAsc(Long clubId);

    Optional<ClubDashboardWidget> findByClubIdAndWidgetKey(Long clubId, String widgetKey);
}
