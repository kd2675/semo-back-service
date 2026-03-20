package semo.back.service.feature.poll.vo;

import jakarta.validation.constraints.NotNull;

public record UpdateClubAdminPollSettingsRequest(
        @NotNull Boolean allowMemberCreate,
        @NotNull Boolean allowMemberUpdate,
        @NotNull Boolean allowMemberDelete
) {
}
