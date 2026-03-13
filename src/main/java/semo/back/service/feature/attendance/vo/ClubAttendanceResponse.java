package semo.back.service.feature.attendance.vo;

import java.util.List;

public record ClubAttendanceResponse(
        Long clubId,
        String clubName,
        boolean featureEnabled,
        AttendanceSessionResponse currentSession,
        List<AttendanceHistoryItemResponse> recentSessions
) {
}
