package semo.back.service.feature.club.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReviewClubJoinRequestRequest(
        @NotBlank @Size(max = 20) String requestStatus
) {
}
