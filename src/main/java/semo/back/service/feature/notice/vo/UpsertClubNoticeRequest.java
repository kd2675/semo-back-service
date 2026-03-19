package semo.back.service.feature.notice.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpsertClubNoticeRequest(
        @NotBlank(message = "공지 제목은 필수입니다.")
        @Size(max = 200, message = "공지 제목은 200자 이하여야 합니다.")
        String title,
        @NotBlank(message = "공지 내용은 필수입니다.")
        String content,
        String fileName,
        String locationLabel,
        String scheduleAt,
        String scheduleEndAt,
        Boolean postToSchedule,
        Boolean pinned
) {
}
