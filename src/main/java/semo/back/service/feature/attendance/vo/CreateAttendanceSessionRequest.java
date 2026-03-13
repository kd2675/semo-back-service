package semo.back.service.feature.attendance.vo;

import java.time.LocalDate;

public record CreateAttendanceSessionRequest(
        String title,
        LocalDate attendanceDate
) {
}
