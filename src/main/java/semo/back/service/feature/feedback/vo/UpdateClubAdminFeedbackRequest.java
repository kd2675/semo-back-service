package semo.back.service.feature.feedback.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateClubAdminFeedbackRequest(
        @NotBlank(message = "피드백 분류는 필수입니다.")
        @Size(max = 40, message = "피드백 분류 길이를 확인해주세요.")
        String feedbackType,

        @NotBlank(message = "처리 상태는 필수입니다.")
        @Size(max = 20, message = "처리 상태 길이를 확인해주세요.")
        String statusCode,

        @NotBlank(message = "공개 범위는 필수입니다.")
        @Size(max = 20, message = "공개 범위 길이를 확인해주세요.")
        String visibilityScope,

        @Size(max = 2000, message = "답변은 2000자 이하여야 합니다.")
        String adminAnswer
) {
}
