package semo.back.service.feature.position.vo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateClubPositionRequest(
        @NotBlank(message = "직책 이름은 필수입니다.")
        @Size(max = 100, message = "직책 이름은 100자 이하여야 합니다.")
        String displayName,
        @NotBlank(message = "직책 코드는 필수입니다.")
        @Size(max = 50, message = "직책 코드는 50자 이하여야 합니다.")
        String positionCode,
        @Size(max = 255, message = "직책 설명은 255자 이하여야 합니다.")
        String description,
        @Size(max = 50, message = "직책 아이콘 이름은 50자 이하여야 합니다.")
        String iconName,
        @Size(max = 20, message = "직책 색상 값은 20자 이하여야 합니다.")
        String colorHex,
        @NotNull(message = "직책 버전은 필수입니다.")
        Long version,
        Boolean active,
        List<@Valid ClubPositionFeatureGrantRequest> featureGrants
) {
}
