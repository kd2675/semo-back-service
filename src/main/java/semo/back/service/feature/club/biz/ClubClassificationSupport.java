package semo.back.service.feature.club.biz;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import semo.back.service.common.exception.SemoException;
import semo.back.service.feature.club.vo.ClubActivityCategory;
import semo.back.service.feature.club.vo.ClubActivityTagKey;
import semo.back.service.feature.club.vo.ClubAffiliationType;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class ClubClassificationSupport {
    private static final int MAX_TAG_COUNT = 5;
    private static final Set<String> LEGACY_CATEGORY_KEYS = Set.of(
            "TENNIS",
            "RUNNING",
            "HIKING",
            "CROSSFIT",
            "CYCLING",
            "OTHER"
    );

    public ResolvedClubClassification resolveForWrite(
            String activityCategoryValue,
            Collection<String> activityTagValues,
            String affiliationTypeValue,
            String legacyCategoryKeyValue
    ) {
        List<String> normalizedTags = normalizeTags(activityTagValues);
        ClubActivityCategory inferredLegacyCategory = null;
        List<String> inferredLegacyTags = List.of();

        String normalizedLegacyCategoryKey = normalizeLegacyCategoryKey(legacyCategoryKeyValue);
        if ((activityCategoryValue == null || activityCategoryValue.isBlank()) && normalizedTags.isEmpty()) {
            LegacyClassification legacyClassification = resolveLegacyClassification(normalizedLegacyCategoryKey);
            inferredLegacyCategory = legacyClassification.activityCategory();
            inferredLegacyTags = legacyClassification.activityTags();
        }

        ClubActivityCategory activityCategory = resolveActivityCategory(
                activityCategoryValue,
                normalizedTags.isEmpty() ? inferredLegacyTags : normalizedTags,
                inferredLegacyCategory
        );
        List<String> activityTags = normalizedTags.isEmpty() ? inferredLegacyTags : normalizedTags;

        if (!activityTags.isEmpty()) {
            validateTagsMatchCategory(activityCategory, activityTags);
        }

        ClubAffiliationType affiliationType = resolveAffiliationType(affiliationTypeValue);
        String legacyCategoryKey = resolveLegacyCategoryKey(activityTags, normalizedLegacyCategoryKey);

        return new ResolvedClubClassification(
                activityCategory.name(),
                activityTags,
                affiliationType.name(),
                legacyCategoryKey
        );
    }

    public ResolvedClubClassification resolveStored(
            String activityCategoryValue,
            Collection<String> activityTagValues,
            String affiliationTypeValue,
            String legacyCategoryKeyValue
    ) {
        return resolveForWrite(activityCategoryValue, activityTagValues, affiliationTypeValue, legacyCategoryKeyValue);
    }

    private ClubActivityCategory resolveActivityCategory(
            String activityCategoryValue,
            Collection<String> activityTags,
            ClubActivityCategory fallback
    ) {
        if (StringUtils.hasText(activityCategoryValue)) {
            try {
                return ClubActivityCategory.valueOf(activityCategoryValue.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                throw new SemoException.ValidationException("지원하지 않는 활동 카테고리입니다.");
            }
        }
        if (!activityTags.isEmpty()) {
            ClubActivityCategory category = null;
            for (String activityTag : activityTags) {
                ClubActivityCategory tagCategory = ClubActivityTagKey.valueOf(activityTag).category();
                if (category == null) {
                    category = tagCategory;
                    continue;
                }
                if (category != tagCategory) {
                    throw new SemoException.ValidationException("서로 다른 활동 카테고리의 태그를 함께 저장할 수 없습니다.");
                }
            }
            return category;
        }
        return fallback == null ? ClubActivityCategory.OTHER : fallback;
    }

    private ClubAffiliationType resolveAffiliationType(String affiliationTypeValue) {
        if (!StringUtils.hasText(affiliationTypeValue)) {
            return ClubAffiliationType.INDEPENDENT;
        }
        try {
            return ClubAffiliationType.valueOf(affiliationTypeValue.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new SemoException.ValidationException("지원하지 않는 소속 유형입니다.");
        }
    }

    private List<String> normalizeTags(Collection<String> activityTagValues) {
        if (activityTagValues == null || activityTagValues.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> normalizedTags = new LinkedHashSet<>();
        for (String activityTagValue : activityTagValues) {
            if (!StringUtils.hasText(activityTagValue)) {
                continue;
            }
            String normalized = activityTagValue.trim().toUpperCase(Locale.ROOT);
            try {
                ClubActivityTagKey.valueOf(normalized);
            } catch (IllegalArgumentException exception) {
                throw new SemoException.ValidationException("지원하지 않는 활동 태그입니다.");
            }
            normalizedTags.add(normalized);
        }
        if (normalizedTags.size() > MAX_TAG_COUNT) {
            throw new SemoException.ValidationException("활동 태그는 최대 5개까지 선택할 수 있습니다.");
        }
        return List.copyOf(normalizedTags);
    }

    private void validateTagsMatchCategory(ClubActivityCategory activityCategory, Collection<String> activityTags) {
        for (String activityTag : activityTags) {
            ClubActivityTagKey activityTagKey = ClubActivityTagKey.valueOf(activityTag);
            if (activityTagKey.category() != activityCategory) {
                throw new SemoException.ValidationException("활동 카테고리와 맞지 않는 태그가 포함되어 있습니다.");
            }
        }
    }

    private String normalizeLegacyCategoryKey(String legacyCategoryKeyValue) {
        if (!StringUtils.hasText(legacyCategoryKeyValue)) {
            return "OTHER";
        }
        String normalized = legacyCategoryKeyValue.trim().toUpperCase(Locale.ROOT);
        if (!LEGACY_CATEGORY_KEYS.contains(normalized)) {
            throw new SemoException.ValidationException("지원하지 않는 클럽 카테고리입니다.");
        }
        return normalized;
    }

    private LegacyClassification resolveLegacyClassification(String legacyCategoryKey) {
        return switch (legacyCategoryKey) {
            case "TENNIS" -> new LegacyClassification(ClubActivityCategory.SPORTS, List.of("TENNIS"));
            case "RUNNING" -> new LegacyClassification(ClubActivityCategory.SPORTS, List.of("RUNNING"));
            case "HIKING" -> new LegacyClassification(ClubActivityCategory.SPORTS, List.of("HIKING"));
            case "CROSSFIT" -> new LegacyClassification(ClubActivityCategory.SPORTS, List.of("CROSSFIT"));
            case "CYCLING" -> new LegacyClassification(ClubActivityCategory.SPORTS, List.of("CYCLING"));
            default -> new LegacyClassification(ClubActivityCategory.OTHER, List.of());
        };
    }

    private String resolveLegacyCategoryKey(List<String> activityTags, String fallbackLegacyCategoryKey) {
        if (!activityTags.isEmpty()) {
            String primaryTag = activityTags.getFirst();
            return LEGACY_CATEGORY_KEYS.contains(primaryTag) ? primaryTag : "OTHER";
        }
        return fallbackLegacyCategoryKey;
    }

    private record LegacyClassification(
            ClubActivityCategory activityCategory,
            List<String> activityTags
    ) {
    }

    public record ResolvedClubClassification(
            String activityCategory,
            List<String> activityTags,
            String affiliationType,
            String legacyCategoryKey
    ) {
    }
}
