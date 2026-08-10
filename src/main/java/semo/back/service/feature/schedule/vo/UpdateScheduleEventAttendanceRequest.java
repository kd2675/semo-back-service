package semo.back.service.feature.schedule.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateScheduleEventAttendanceRequest(
        @NotBlank String attendanceStatus,
        @Size(max = 500) String note
) {
}
