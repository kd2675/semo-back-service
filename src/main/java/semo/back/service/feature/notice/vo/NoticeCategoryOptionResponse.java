package semo.back.service.feature.notice.vo;

public record NoticeCategoryOptionResponse(
        String categoryKey,
        String displayName,
        String iconName,
        String accentTone
) {
}
