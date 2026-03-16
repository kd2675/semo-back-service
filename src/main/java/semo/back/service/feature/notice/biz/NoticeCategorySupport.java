package semo.back.service.feature.notice.biz;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import semo.back.service.common.exception.SemoException;
import semo.back.service.database.pub.entity.ClubNoticeCategorySetting;
import semo.back.service.database.pub.entity.NoticeCategoryCatalog;
import semo.back.service.database.pub.repository.ClubNoticeCategorySettingRepository;
import semo.back.service.database.pub.repository.NoticeCategoryCatalogRepository;
import semo.back.service.feature.notice.vo.NoticeCategoryOptionResponse;
import semo.back.service.feature.notice.vo.NoticeCategorySettingResponse;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class NoticeCategorySupport {
    private static final List<NoticeCategoryCatalog> DEFAULT_CATEGORIES = List.of(
            NoticeCategoryCatalog.builder()
                    .categoryKey("ANNOUNCEMENT")
                    .displayName("Announcement")
                    .iconName("campaign")
                    .accentTone("blue")
                    .active(true)
                    .sortOrder(10)
                    .build(),
            NoticeCategoryCatalog.builder()
                    .categoryKey("TOURNAMENT")
                    .displayName("Tournament")
                    .iconName("emoji_events")
                    .accentTone("amber")
                    .active(true)
                    .sortOrder(20)
                    .build(),
            NoticeCategoryCatalog.builder()
                    .categoryKey("MATCH")
                    .displayName("Match")
                    .iconName("sports_tennis")
                    .accentTone("blue")
                    .active(true)
                    .sortOrder(30)
                    .build(),
            NoticeCategoryCatalog.builder()
                    .categoryKey("SOCIAL")
                    .displayName("Social")
                    .iconName("celebration")
                    .accentTone("purple")
                    .active(true)
                    .sortOrder(40)
                    .build(),
            NoticeCategoryCatalog.builder()
                    .categoryKey("GENERAL")
                    .displayName("General")
                    .iconName("description")
                    .accentTone("slate")
                    .active(true)
                    .sortOrder(50)
                    .build()
    );

    private final NoticeCategoryCatalogRepository noticeCategoryCatalogRepository;
    private final ClubNoticeCategorySettingRepository clubNoticeCategorySettingRepository;

    @Transactional(transactionManager = "pubTransactionManager")
    public void ensureDefaultCategories() {
        Set<String> existingKeys = noticeCategoryCatalogRepository.findAll().stream()
                .map(NoticeCategoryCatalog::getCategoryKey)
                .collect(Collectors.toSet());

        DEFAULT_CATEGORIES.stream()
                .filter(category -> !existingKeys.contains(category.getCategoryKey()))
                .forEach(noticeCategoryCatalogRepository::save);
    }

    public List<NoticeCategoryCatalog> getActiveCategories() {
        ensureDefaultCategories();
        return noticeCategoryCatalogRepository.findByActiveTrueOrderBySortOrderAscCategoryKeyAsc();
    }

    public Map<String, NoticeCategoryCatalog> getActiveCategoryMap() {
        return getActiveCategories().stream()
                .collect(Collectors.toMap(NoticeCategoryCatalog::getCategoryKey, Function.identity()));
    }

    public List<NoticeCategoryOptionResponse> getCategoryOptions() {
        return getActiveCategories().stream()
                .map(category -> new NoticeCategoryOptionResponse(
                        category.getCategoryKey(),
                        category.getDisplayName(),
                        category.getIconName(),
                        category.getAccentTone()
                ))
                .toList();
    }

    public String normalizeRequiredCategoryKey(String categoryKey) {
        String normalized = normalizeCategoryKey(categoryKey);
        if (!getActiveCategoryMap().containsKey(normalized)) {
            throw new SemoException.ValidationException("지원하지 않는 공지 카테고리입니다.");
        }
        return normalized;
    }

    public String normalizeOptionalCategoryKey(String categoryKey) {
        if (!StringUtils.hasText(categoryKey)) {
            return null;
        }
        return normalizeRequiredCategoryKey(categoryKey);
    }

    public List<NoticeCategoryCatalog> getTimelineVisibleCategories(Long clubId) {
        List<NoticeCategoryCatalog> activeCategories = getActiveCategories();
        Map<String, ClubNoticeCategorySetting> settingByKey = clubNoticeCategorySettingRepository.findByClubId(clubId).stream()
                .collect(Collectors.toMap(ClubNoticeCategorySetting::getCategoryKey, Function.identity()));

        return activeCategories.stream()
                .filter(category -> settingByKey.get(category.getCategoryKey()) == null
                        || settingByKey.get(category.getCategoryKey()).isVisibleInTimeline())
                .toList();
    }

    @Transactional(transactionManager = "pubTransactionManager")
    public List<NoticeCategorySettingResponse> updateTimelineVisibleCategories(
            Long clubId,
            Long clubProfileId,
            List<String> visibleCategoryKeys
    ) {
        List<NoticeCategoryCatalog> activeCategories = getActiveCategories();
        Set<String> allowedCategoryKeys = activeCategories.stream()
                .map(NoticeCategoryCatalog::getCategoryKey)
                .collect(Collectors.toSet());
        Set<String> normalizedVisibleKeys = (visibleCategoryKeys == null ? List.<String>of() : visibleCategoryKeys).stream()
                .map(this::normalizeCategoryKey)
                .collect(Collectors.toSet());

        if (!allowedCategoryKeys.containsAll(normalizedVisibleKeys)) {
            throw new SemoException.ValidationException("지원하지 않는 공지 카테고리가 포함되어 있습니다.");
        }

        Map<String, ClubNoticeCategorySetting> existingByKey = clubNoticeCategorySettingRepository.findByClubId(clubId).stream()
                .collect(Collectors.toMap(ClubNoticeCategorySetting::getCategoryKey, Function.identity()));

        for (NoticeCategoryCatalog category : activeCategories) {
            boolean visible = normalizedVisibleKeys.contains(category.getCategoryKey());
            ClubNoticeCategorySetting existing = existingByKey.get(category.getCategoryKey());
            clubNoticeCategorySettingRepository.save(ClubNoticeCategorySetting.builder()
                    .clubNoticeCategorySettingId(existing == null ? null : existing.getClubNoticeCategorySettingId())
                    .clubId(clubId)
                    .categoryKey(category.getCategoryKey())
                    .visibleInTimeline(visible)
                    .updatedByClubProfileId(clubProfileId)
                    .build());
        }

        return getTimelineSettingOptions(clubId);
    }

    public List<NoticeCategoryOptionResponse> getTimelineVisibleCategoryOptions(Long clubId) {
        return getTimelineVisibleCategories(clubId).stream()
                .map(category -> new NoticeCategoryOptionResponse(
                        category.getCategoryKey(),
                        category.getDisplayName(),
                        category.getIconName(),
                        category.getAccentTone()
                ))
                .toList();
    }

    public List<NoticeCategorySettingResponse> getTimelineSettingOptions(Long clubId) {
        List<NoticeCategoryCatalog> activeCategories = getActiveCategories();
        Set<String> visibleKeys = getTimelineVisibleCategories(clubId).stream()
                .map(NoticeCategoryCatalog::getCategoryKey)
                .collect(Collectors.toSet());

        return activeCategories.stream()
                .map(category -> new NoticeCategorySettingResponse(
                        category.getCategoryKey(),
                        category.getDisplayName(),
                        category.getIconName(),
                        category.getAccentTone(),
                        visibleKeys.contains(category.getCategoryKey())
                ))
                .toList();
    }

    private String normalizeCategoryKey(String categoryKey) {
        if (!StringUtils.hasText(categoryKey)) {
            return "ANNOUNCEMENT";
        }
        return categoryKey.trim().toUpperCase(Locale.ROOT);
    }
}
