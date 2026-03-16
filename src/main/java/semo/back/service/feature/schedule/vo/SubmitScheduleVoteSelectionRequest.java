package semo.back.service.feature.schedule.vo;

import jakarta.validation.constraints.NotNull;

public record SubmitScheduleVoteSelectionRequest(
        @NotNull(message = "투표 항목은 필수입니다.")
        Long voteOptionId
) {
}
