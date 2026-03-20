package semo.back.service.feature.schedule.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record UpsertScheduleEventRequest(
        @NotBlank(message = "일정 제목은 필수입니다.")
        @Size(max = 200, message = "일정 제목은 200자 이하여야 합니다.")
        String title,
        @NotBlank(message = "시작 날짜는 필수입니다.")
        String startDate,
        String endDate,
        String startTime,
        String endTime,
        @Positive(message = "인원수는 1명 이상이어야 합니다.")
        Integer attendeeLimit,
        @Size(max = 200, message = "장소는 200자 이하여야 합니다.")
        String locationLabel,
        @Size(max = 1000, message = "참여 조건은 1000자 이하여야 합니다.")
        String participationConditionText,
        Boolean participationEnabled,
        Boolean feeRequired,
        @Positive(message = "참가비 금액은 1원 이상이어야 합니다.")
        Integer feeAmount,
        Boolean feeAmountUndecided,
        Boolean feeNWaySplit,
        Boolean postToBoard,
        Boolean postToCalendar
) {
}
