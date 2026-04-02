package semo.back.service.feature.memberdirectory.vo;

public record MemberDirectoryRecentActivityResponse(
        String subject,
        String detail,
        String createdAt,
        String createdAtLabel
) {
}
