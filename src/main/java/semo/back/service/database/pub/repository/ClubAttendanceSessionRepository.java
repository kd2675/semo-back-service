package semo.back.service.database.pub.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import semo.back.service.database.pub.entity.ClubAttendanceSession;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ClubAttendanceSessionRepository extends JpaRepository<ClubAttendanceSession, Long> {
    Optional<ClubAttendanceSession> findByClubIdAndAttendanceDate(Long clubId, LocalDate attendanceDate);

    @Query("""
            select s
            from ClubAttendanceSession s
            where s.clubId = :clubId
            order by s.attendanceDate desc, s.attendanceSessionId desc
            """)
    List<ClubAttendanceSession> findRecentSessions(Long clubId, Pageable pageable);
}
