package semo.back.service.database.pub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import semo.back.service.database.pub.entity.FeatureCatalog;

import java.util.List;

public interface FeatureCatalogRepository extends JpaRepository<FeatureCatalog, String> {
    List<FeatureCatalog> findByActiveTrueOrderBySortOrderAscFeatureKeyAsc();
}
