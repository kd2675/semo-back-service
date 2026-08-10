package semo.back.service.database.pub.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import semo.back.service.database.pub.entity.TournamentRosterMember;

public interface TournamentRosterMemberRepository extends JpaRepository<TournamentRosterMember, Long> {
    List<TournamentRosterMember> findByTournamentApplicationIdInOrderBySortOrderAscTournamentRosterMemberIdAsc(
            Collection<Long> tournamentApplicationIds
    );

    void deleteByTournamentApplicationId(Long tournamentApplicationId);

    void deleteByTournamentApplicationIdIn(Collection<Long> tournamentApplicationIds);
}
