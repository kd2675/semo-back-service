package semo.back.service.feature.tournament.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TournamentEntryDraftMemberRequest(
        @NotNull Long clubProfileId,
        @NotBlank @Size(max = 20) String memberRole
) {
}
