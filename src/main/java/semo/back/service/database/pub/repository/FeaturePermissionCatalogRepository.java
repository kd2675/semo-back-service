package semo.back.service.database.pub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import semo.back.service.database.pub.entity.FeaturePermissionCatalog;

import java.util.List;

public interface FeaturePermissionCatalogRepository extends JpaRepository<FeaturePermissionCatalog, String> {
    List<FeaturePermissionCatalog> findByActiveTrueOrderByFeatureKeyAscSortOrderAscPermissionKeyAsc();
}
