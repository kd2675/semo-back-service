package semo.back.service.feature.notice.vo;

public record NoticeCategorySettingResponse(
        String categoryKey,
        String displayName,
        String iconName,
        String accentTone,
        boolean visibleInTimeline
) {
}
