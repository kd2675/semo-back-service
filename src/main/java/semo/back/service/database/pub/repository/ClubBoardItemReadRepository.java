package semo.back.service.database.pub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import semo.back.service.database.pub.entity.ClubBoardItemRead;

import java.util.List;
import java.util.Optional;

public interface ClubBoardItemReadRepository extends JpaRepository<ClubBoardItemRead, Long> {
    Optional<ClubBoardItemRead> findByBoardItemIdAndClubProfileId(Long boardItemId, Long clubProfileId);

    void deleteByBoardItemId(Long boardItemId);

    long countByBoardItemId(Long boardItemId);

    List<ClubBoardItemRead> findAllByBoardItemIdOrderByLastReadAtDescClubBoardItemReadIdDesc(Long boardItemId);

    @Query("""
            select new semo.back.service.database.pub.repository.BoardItemReadCountRow(
                read.boardItemId,
                count(read)
            )
            from ClubBoardItemRead read
            where read.boardItemId in :boardItemIds
            group by read.boardItemId
            """)
    List<BoardItemReadCountRow> findReadCountsByBoardItemIdIn(List<Long> boardItemIds);
}
