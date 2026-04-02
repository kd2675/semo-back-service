package semo.back.service.feature.feedback.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateClubFeedbackRequest(
        @NotBlank(message = "피드백 분류는 필수입니다.")
        @Size(max = 40, message = "피드백 분류 길이를 확인해주세요.")
        String feedbackType,

        @NotBlank(message = "제목은 필수입니다.")
        @Size(max = 200, message = "제목은 200자 이하여야 합니다.")
        String title,

        @NotBlank(message = "내용은 필수입니다.")
        @Size(max = 2000, message = "내용은 2000자 이하여야 합니다.")
        String content,

        Boolean anonymous
) {
}
