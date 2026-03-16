package semo.back.service.feature.timeline.vo;

import java.util.List;

public record UpdateClubTimelineRequest(
        List<String> visibleCategoryKeys
) {
}
