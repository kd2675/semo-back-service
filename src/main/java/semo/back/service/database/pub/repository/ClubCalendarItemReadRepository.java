package semo.back.service.database.pub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import semo.back.service.database.pub.entity.ClubCalendarItemRead;

import java.util.List;
import java.util.Optional;

public interface ClubCalendarItemReadRepository extends JpaRepository<ClubCalendarItemRead, Long> {
    Optional<ClubCalendarItemRead> findByCalendarItemIdAndClubProfileId(Long calendarItemId, Long clubProfileId);

    long countByCalendarItemId(Long calendarItemId);

    List<ClubCalendarItemRead> findAllByCalendarItemIdOrderByLastReadAtDescClubCalendarItemReadIdDesc(Long calendarItemId);

    @Query("""
            select new semo.back.service.database.pub.repository.CalendarItemReadCountRow(
                read.calendarItemId,
                count(read)
            )
            from ClubCalendarItemRead read
            where read.calendarItemId in :calendarItemIds
            group by read.calendarItemId
            """)
    List<CalendarItemReadCountRow> findReadCountsByCalendarItemIdIn(List<Long> calendarItemIds);
}
