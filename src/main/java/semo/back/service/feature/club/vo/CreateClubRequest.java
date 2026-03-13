package semo.back.service.feature.club.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateClubRequest(
        @NotBlank(message = "클럽 이름은 필수입니다.")
        @Size(max = 120, message = "클럽 이름은 120자 이하여야 합니다.")
        String name,

        @Size(max = 2000, message = "클럽 설명은 2000자 이하여야 합니다.")
        String description,

        @Size(max = 40, message = "카테고리 키는 40자 이하여야 합니다.")
        String categoryKey,

        @Size(max = 20, message = "공개 범위 값이 올바르지 않습니다.")
        String visibilityStatus,

        @Size(max = 20, message = "가입 방식 값이 올바르지 않습니다.")
        String membershipPolicy,

        @Size(max = 255, message = "이미지 파일 이름이 너무 깁니다.")
        String fileName
) {
}
