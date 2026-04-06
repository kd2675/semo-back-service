package semo.back.service.feature.club.vo;

import jakarta.validation.constraints.Size;

public record UpdateClubSettingsRequest(
        @Size(max = 20, message = "지역 범위 값이 올바르지 않습니다.")
        String regionScope,

        @Size(max = 10, message = "시도 코드는 10자 이하여야 합니다.")
        String regionDepth1Code,

        @Size(max = 10, message = "시군구 코드는 10자 이하여야 합니다.")
        String regionDepth2Code,

        @Size(max = 60, message = "시도명은 60자 이하여야 합니다.")
        String regionDepth1Name,

        @Size(max = 60, message = "시군구명은 60자 이하여야 합니다.")
        String regionDepth2Name
) {
}
