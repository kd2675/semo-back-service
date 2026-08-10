package semo.back.service.feature.tournament.vo;

import jakarta.validation.constraints.Size;

import java.util.List;

public record SubmitTournamentApplicationRequest(
        @Size(max = 500) String applicationNote,
        @Size(max = 100) String teamName,
        List<Long> rosterClubProfileIds
) {
    public SubmitTournamentApplicationRequest(String applicationNote) {
        this(applicationNote, null, null);
    }
}
