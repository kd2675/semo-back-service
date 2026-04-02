package semo.back.service.feature.club.vo;

import jakarta.validation.constraints.Size;

public record SubmitClubJoinRequestRequest(
        @Size(max = 500) String requestMessage
) {
}
