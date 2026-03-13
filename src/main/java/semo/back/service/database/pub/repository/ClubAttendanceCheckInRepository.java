package semo.back.service.database.pub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import semo.back.service.database.pub.entity.ClubAttendanceCheckIn;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ClubAttendanceCheckInRepository extends JpaRepository<ClubAttendanceCheckIn, Long> {
    Optional<ClubAttendanceCheckIn> findByAttendanceSessionIdAndClubProfileId(Long attendanceSessionId, Long clubProfileId);

    List<ClubAttendanceCheckIn> findByAttendanceSessionId(Long attendanceSessionId);

    List<ClubAttendanceCheckIn> findByAttendanceSessionIdIn(Collection<Long> attendanceSessionIds);
}
