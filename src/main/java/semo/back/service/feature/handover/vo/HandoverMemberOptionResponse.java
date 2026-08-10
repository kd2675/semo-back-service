package semo.back.service.feature.handover.vo;

public record HandoverMemberOptionResponse(
        Long clubMemberId,
        Long clubProfileId,
        String displayName,
        String avatarFileName
) {
}
