package semo.back.service.database.pub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import semo.back.service.database.pub.entity.ClubFeature;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ClubFeatureRepository extends JpaRepository<ClubFeature, Long> {
    List<ClubFeature> findByClubId(Long clubId);

    List<ClubFeature> findByClubIdAndFeatureKeyIn(Long clubId, Collection<String> featureKeys);

    Optional<ClubFeature> findByClubIdAndFeatureKey(Long clubId, String featureKey);
}
