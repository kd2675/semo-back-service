package semo.back.service.feature.tournament.vo;

public record TournamentScheduleSlotResponse(
        Long tournamentScheduleSlotId,
        String title,
        String courtLabel,
        String startAt,
        String startAtLabel,
        String endAt,
        String endAtLabel,
        String note
) {
}
