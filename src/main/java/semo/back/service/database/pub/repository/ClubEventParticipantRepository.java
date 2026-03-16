package semo.back.service.database.pub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import semo.back.service.database.pub.entity.ClubEventParticipant;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ClubEventParticipantRepository extends JpaRepository<ClubEventParticipant, Long> {
    List<ClubEventParticipant> findByEventIdIn(Collection<Long> eventIds);

    Optional<ClubEventParticipant> findByEventIdAndClubProfileId(Long eventId, Long clubProfileId);

    void deleteByEventId(Long eventId);
}
