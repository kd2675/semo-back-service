package semo.back.service.feature.position.vo;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record UpdateClubPositionRequest(
        @NotBlank(message = "직책 이름은 필수입니다.")
        String displayName,
        @NotBlank(message = "직책 코드는 필수입니다.")
        String positionCode,
        String description,
        String iconName,
        String colorHex,
        Boolean active,
        List<String> permissionKeys
) {
}
