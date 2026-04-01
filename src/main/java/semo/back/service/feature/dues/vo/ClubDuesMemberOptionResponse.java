package semo.back.service.feature.dues.vo;

public record ClubDuesMemberOptionResponse(
        Long clubProfileId,
        String memberDisplayName,
        String memberRoleCode
) {
}
