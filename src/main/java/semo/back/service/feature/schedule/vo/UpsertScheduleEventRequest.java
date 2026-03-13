package semo.back.service.feature.schedule.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpsertScheduleEventRequest(
        @NotBlank(message = "일정 제목은 필수입니다.")
        @Size(max = 200, message = "일정 제목은 200자 이하여야 합니다.")
        String title,
        String description,
        String categoryKey,
        String locationLabel,
        @NotBlank(message = "시작 시간은 필수입니다.")
        String startAt,
        String endAt,
        Boolean postToBoard
) {
}
