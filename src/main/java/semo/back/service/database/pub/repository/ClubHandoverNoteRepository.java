package semo.back.service.database.pub.repository;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import semo.back.service.database.pub.entity.ClubHandoverNote;

public interface ClubHandoverNoteRepository extends JpaRepository<ClubHandoverNote, Long> {
    @Query("""
            select note
            from ClubHandoverNote note
            where note.clubId = :clubId
              and note.deleted = false
              and (
                    :termId is null
                    or note.fromTermId = :termId
                    or note.toTermId = :termId
                  )
            order by
              case note.statusCode when 'READY' then 0 when 'DRAFT' then 1 else 2 end,
              case when note.dueAt is null then 1 else 0 end,
              note.dueAt asc,
              note.clubHandoverNoteId desc
            """)
    List<ClubHandoverNote> findFeed(Long clubId, Long termId);

    Optional<ClubHandoverNote> findByClubHandoverNoteIdAndClubIdAndDeletedFalse(
            Long clubHandoverNoteId,
            Long clubId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select note
            from ClubHandoverNote note
            where note.clubHandoverNoteId = :clubHandoverNoteId
              and note.clubId = :clubId
              and note.deleted = false
            """)
    Optional<ClubHandoverNote> findForUpdate(Long clubHandoverNoteId, Long clubId);

    long countByClubIdAndDeletedFalseAndStatusCodeIn(Long clubId, List<String> statusCodes);
}
