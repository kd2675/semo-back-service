package semo.back.service.feature.attendance.vo;

import java.util.List;

public record ClubAdminAttendanceResponse(
        Long clubId,
        String clubName,
        boolean featureEnabled,
        AttendanceSessionResponse currentSession,
        List<AdminAttendanceMemberResponse> members,
        List<AttendanceHistoryItemResponse> recentSessions
) {
}
