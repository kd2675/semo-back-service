package semo.back.service.feature.tournament.vo;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpsertTournamentRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 500) String summaryText,
        String detailText,
        @NotBlank String applicationStartAt,
        @NotBlank String applicationEndAt,
        @NotBlank String startDate,
        @NotBlank String endDate,
        @Size(max = 200) String locationLabel,
        @NotBlank @Size(max = 20) String matchFormat,
        Integer teamMemberLimit,
        Integer participantLimit,
        @NotNull Boolean feeRequired,
        Integer feeAmount,
        @Size(max = 10) String feeCurrencyCode,
        Boolean postToBoard,
        Boolean postToCalendar,
        Boolean pinned,
        @NotBlank @Size(max = 20) String bracketMode
) {
}
