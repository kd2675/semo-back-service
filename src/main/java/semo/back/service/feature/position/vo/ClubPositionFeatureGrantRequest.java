package semo.back.service.feature.position.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ClubPositionFeatureGrantRequest(
        @NotBlank(message = "기능 키는 필수입니다.")
        @Size(max = 50, message = "기능 키는 50자 이하여야 합니다.")
        String featureKey,
        @NotBlank(message = "운영 수준은 필수입니다.")
        @Size(max = 20, message = "운영 수준은 20자 이하여야 합니다.")
        String accessLevel,
        Integer policyVersion,
        List<@Size(max = 80, message = "권한 키는 80자 이하여야 합니다.") String> sensitivePermissionKeys
) {
}
