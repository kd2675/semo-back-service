package semo.back.service.feature.notice.vo;

import jakarta.validation.constraints.NotNull;

public record UpdateClubAdminNoticeSettingsRequest(
        @NotNull(message = "공지 생성 허용 여부는 필수입니다.")
        Boolean allowMemberCreate,
        @NotNull(message = "공지 수정 허용 여부는 필수입니다.")
        Boolean allowMemberUpdate,
        @NotNull(message = "공지 삭제 허용 여부는 필수입니다.")
        Boolean allowMemberDelete
) {
}
