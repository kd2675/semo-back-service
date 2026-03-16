package semo.back.service.database.pub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import semo.back.service.database.pub.entity.NoticeCategoryCatalog;

import java.util.List;

public interface NoticeCategoryCatalogRepository extends JpaRepository<NoticeCategoryCatalog, String> {
    List<NoticeCategoryCatalog> findByActiveTrueOrderBySortOrderAscCategoryKeyAsc();
}
