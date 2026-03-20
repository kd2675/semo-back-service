package semo.back.service.feature.schedule.vo;

import jakarta.validation.constraints.NotNull;

public record UpdateClubAdminScheduleSettingsRequest(
        @NotNull(message = "일정 생성 허용 여부는 필수입니다.")
        Boolean allowMemberCreate,
        @NotNull(message = "일정 수정 허용 여부는 필수입니다.")
        Boolean allowMemberUpdate,
        @NotNull(message = "일정 삭제 허용 여부는 필수입니다.")
        Boolean allowMemberDelete
) {
}
