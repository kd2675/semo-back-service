package semo.back.service.feature.position.vo;

import jakarta.validation.constraints.Size;

public record DeleteClubPositionHistoryRequest(
        @Size(max = 500, message = "삭제 사유는 500자 이하여야 합니다.")
        String deleteReason
) {
}
