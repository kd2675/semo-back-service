package semo.back.service.database.pub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import semo.back.service.database.pub.entity.ClubNoticeCategorySetting;

import java.util.List;
import java.util.Optional;

public interface ClubNoticeCategorySettingRepository extends JpaRepository<ClubNoticeCategorySetting, Long> {
    List<ClubNoticeCategorySetting> findByClubId(Long clubId);

    Optional<ClubNoticeCategorySetting> findByClubIdAndCategoryKey(Long clubId, String categoryKey);
}
