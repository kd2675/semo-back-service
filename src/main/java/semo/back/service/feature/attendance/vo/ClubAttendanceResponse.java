package semo.back.service.feature.attendance.vo;

import java.util.List;

public record ClubAttendanceResponse(
        Long clubId,
        String clubName,
        boolean featureEnabled,
        AttendanceTodayResponse todayAttendance,
        List<AttendanceDailyLogResponse> recentLogs
) {
}
