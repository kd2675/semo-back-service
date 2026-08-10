package semo.back.service.feature.clubfeature.vo;

import jakarta.validation.constraints.NotNull;

public record UpdateClubMorePreferenceRequest(
        @NotNull Boolean favorite
) {
}
