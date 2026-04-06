package semo.back.service.feature.club.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateClubRequest(
        @NotBlank(message = "클럽 이름은 필수입니다.")
        @Size(max = 120, message = "클럽 이름은 120자 이하여야 합니다.")
        String name,

        @Size(max = 2000, message = "클럽 설명은 2000자 이하여야 합니다.")
        String description,

        @Size(max = 40, message = "카테고리 키는 40자 이하여야 합니다.")
        String categoryKey,

        @Size(max = 30, message = "활동 카테고리 값이 올바르지 않습니다.")
        String activityCategory,

        List<String> activityTags,

        @Size(max = 30, message = "소속 유형 값이 올바르지 않습니다.")
        String affiliationType,

        @Size(max = 20, message = "공개 범위 값이 올바르지 않습니다.")
        String visibilityStatus,

        @Size(max = 20, message = "가입 방식 값이 올바르지 않습니다.")
        String membershipPolicy,

        @Size(max = 20, message = "지역 범위 값이 올바르지 않습니다.")
        String regionScope,

        @Size(max = 10, message = "시도 코드는 10자 이하여야 합니다.")
        String regionDepth1Code,

        @Size(max = 10, message = "시군구 코드는 10자 이하여야 합니다.")
        String regionDepth2Code,

        @Size(max = 60, message = "시도명은 60자 이하여야 합니다.")
        String regionDepth1Name,

        @Size(max = 60, message = "시군구명은 60자 이하여야 합니다.")
        String regionDepth2Name,

        @Size(max = 255, message = "이미지 파일 이름이 너무 깁니다.")
        String fileName
) {
    public CreateClubRequest(
            String name,
            String description,
            String categoryKey,
            String visibilityStatus,
            String membershipPolicy,
            String fileName
    ) {
        this(name, description, categoryKey, null, null, null, visibilityStatus, membershipPolicy, null, null, null, null, null, fileName);
    }

    public CreateClubRequest(
            String name,
            String description,
            String categoryKey,
            String visibilityStatus,
            String membershipPolicy,
            String regionScope,
            String regionDepth1Name,
            String regionDepth2Name,
            String fileName
    ) {
        this(name, description, categoryKey, null, null, null, visibilityStatus, membershipPolicy, regionScope, null, null, regionDepth1Name, regionDepth2Name, fileName);
    }
}
