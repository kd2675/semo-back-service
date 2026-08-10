package semo.back.service.database.pub.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import semo.back.service.database.pub.entity.ClubProfile;

public interface ClubProfileRepository extends JpaRepository<ClubProfile, Long> {
    Optional<ClubProfile> findByClubMemberId(Long clubMemberId);

    List<ClubProfile> findByClubMemberIdIn(Collection<Long> clubMemberIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select profile from ClubProfile profile where profile.clubProfileId = :clubProfileId")
    Optional<ClubProfile> findForUpdateByClubProfileId(Long clubProfileId);
}
