package semo.back.service.feature.tournament.vo;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpsertTournamentEntryDraftRequest(
        @NotBlank @Size(max = 150) String displayName,
        Integer seedNumber,
        @Valid List<TournamentEntryDraftMemberRequest> members
) {
}
