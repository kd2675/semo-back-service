package semo.back.service.feature.finance.vo;

public record ClubFinanceMemberOptionResponse(
        Long clubProfileId,
        String memberDisplayName,
        String memberRoleCode
) {
}
