package semo.back.service.feature.schedule.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpsertScheduleVoteRequest(
        @NotBlank(message = "투표 제목은 필수입니다.")
        @Size(max = 200, message = "투표 제목은 200자 이하여야 합니다.")
        String title,
        @NotBlank(message = "투표 시작일은 필수입니다.")
        String voteStartDate,
        @NotBlank(message = "투표 종료일은 필수입니다.")
        String voteEndDate,
        String voteStartTime,
        String voteEndTime,
        @Size(max = 8, message = "투표 항목은 최대 8개까지 등록할 수 있습니다.")
        List<
                @NotBlank(message = "투표 항목은 비어 있을 수 없습니다.")
                @Size(max = 120, message = "투표 항목은 120자 이하여야 합니다.")
                String
                > optionLabels,
        Boolean postToBoard
) {
}
