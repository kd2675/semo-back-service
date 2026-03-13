package semo.back.service.database.pub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import semo.back.service.database.pub.entity.ClubProfile;

import java.util.Optional;

public interface ClubProfileRepository extends JpaRepository<ClubProfile, Long> {
    Optional<ClubProfile> findByClubMemberId(Long clubMemberId);
}
