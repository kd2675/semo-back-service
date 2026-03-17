package semo.back.service.feature.club.vo;

import jakarta.validation.constraints.NotBlank;

public record UpdateClubAdminMemberRoleRequest(
        @NotBlank
        String roleCode
) {
}
