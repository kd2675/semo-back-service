package semo.back.service.feature.schedule.vo;

import jakarta.validation.constraints.NotBlank;

public record UpdateScheduleEventParticipationRequest(
        @NotBlank(message = "참석 상태는 필수입니다.")
        String participationStatus
) {
}
