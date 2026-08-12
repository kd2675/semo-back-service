package semo.back.service.feature.tournament.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelTournamentRequest(
        @NotBlank(message = "대회 취소 사유를 입력해야 합니다.")
        @Size(max = 500, message = "대회 취소 사유는 500자 이하여야 합니다.")
        String cancelReason
) {
}
