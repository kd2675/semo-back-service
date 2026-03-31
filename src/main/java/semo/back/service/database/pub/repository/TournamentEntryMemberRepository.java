package semo.back.service.database.pub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import semo.back.service.database.pub.entity.TournamentEntryMember;

import java.util.Collection;
import java.util.List;

public interface TournamentEntryMemberRepository extends JpaRepository<TournamentEntryMember, Long> {
    List<TournamentEntryMember> findByTournamentEntryIdIn(Collection<Long> tournamentEntryIds);

    List<TournamentEntryMember> findByClubProfileId(Long clubProfileId);

    void deleteByTournamentEntryIdIn(Collection<Long> tournamentEntryIds);
}
