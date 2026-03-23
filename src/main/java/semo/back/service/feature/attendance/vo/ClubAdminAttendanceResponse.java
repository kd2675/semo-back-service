package semo.back.service.feature.attendance.vo;

import java.util.List;

public record ClubAdminAttendanceResponse(
        Long clubId,
        String clubName,
        boolean featureEnabled,
        AttendanceTodayResponse todayAttendance,
        List<AdminAttendanceMemberResponse> members,
        List<AttendanceDailyLogResponse> recentLogs
) {
}
