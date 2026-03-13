package semo.back.service.feature.club.vo;

import java.util.List;

public record ClubScheduleResponse(
        Long clubId,
        String clubName,
        boolean admin,
        List<ClubScheduleMonthResponse> months
) {
}
