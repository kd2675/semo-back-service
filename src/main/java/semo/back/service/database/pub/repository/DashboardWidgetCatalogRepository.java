package semo.back.service.database.pub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import semo.back.service.database.pub.entity.DashboardWidgetCatalog;

import java.util.List;

public interface DashboardWidgetCatalogRepository extends JpaRepository<DashboardWidgetCatalog, String> {
    List<DashboardWidgetCatalog> findByActiveTrueOrderByDefaultSortOrderAscWidgetKeyAsc();
}
