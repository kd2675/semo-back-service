package semo.back.service.feature.club.biz.catalog;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import semo.back.service.common.exception.SemoException;
import semo.back.service.feature.club.vo.ClubRegionScope;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ClubRegionCatalog {
    private final ObjectMapper objectMapper;

    private Map<String, RegionEntry> regionByDepth1Code;
    private Map<String, RegionEntry> regionByDepth1Name;

    public ResolvedClubRegion resolve(
            String regionScopeValue,
            String regionDepth1Code,
            String regionDepth2Code,
            String regionDepth1Name,
            String regionDepth2Name
    ) {
        ensureCatalogLoaded();
        ClubRegionScope regionScope = resolveScope(regionScopeValue);
        if (regionScope == ClubRegionScope.NATIONWIDE) {
            return new ResolvedClubRegion(regionScope.name(), null, null, null, null, "전국");
        }
        if (regionScope == ClubRegionScope.ONLINE) {
            return new ResolvedClubRegion(regionScope.name(), null, null, null, null, "온라인");
        }

        RegionEntry depth1 = resolveDepth1(regionDepth1Code, regionDepth1Name);
        Depth2Entry depth2 = resolveDepth2(depth1, regionDepth2Code, regionDepth2Name);

        return new ResolvedClubRegion(
                regionScope.name(),
                depth1.depth1Code(),
                depth2 == null ? null : depth2.depth2Code(),
                depth1.depth1Name(),
                depth2 == null ? null : depth2.depth2Name(),
                depth2 == null ? depth1.depth1Name() : depth1.depth1Name() + " " + depth2.depth2Name()
        );
    }

    private RegionEntry resolveDepth1(String regionDepth1Code, String regionDepth1Name) {
        String normalizedDepth1Code = trimToNull(regionDepth1Code);
        if (normalizedDepth1Code != null) {
            RegionEntry byCode = regionByDepth1Code.get(normalizedDepth1Code);
            if (byCode == null) {
                throw new SemoException.ValidationException("지원하지 않는 시도 코드입니다.");
            }
            return byCode;
        }

        String normalizedDepth1Name = trimToNull(regionDepth1Name);
        if (normalizedDepth1Name == null) {
            throw new SemoException.ValidationException("오프라인 모임은 시도 코드를 선택해야 합니다.");
        }

        RegionEntry byName = regionByDepth1Name.get(normalizedDepth1Name);
        if (byName == null) {
            throw new SemoException.ValidationException("지원하지 않는 시도입니다.");
        }
        return byName;
    }

    private Depth2Entry resolveDepth2(RegionEntry depth1, String regionDepth2Code, String regionDepth2Name) {
        String normalizedDepth2Code = trimToNull(regionDepth2Code);
        if (normalizedDepth2Code != null) {
            Depth2Entry byCode = depth1.depth2ByCode().get(normalizedDepth2Code);
            if (byCode == null) {
                throw new SemoException.ValidationException("선택한 시도에 속하지 않는 시군구 코드입니다.");
            }
            return byCode;
        }

        String normalizedDepth2Name = trimToNull(regionDepth2Name);
        if (normalizedDepth2Name == null) {
            return null;
        }

        Depth2Entry byName = depth1.depth2ByName().get(normalizedDepth2Name);
        if (byName == null) {
            throw new SemoException.ValidationException("선택한 시도에 속하지 않는 시군구입니다.");
        }
        return byName;
    }

    private ClubRegionScope resolveScope(String regionScopeValue) {
        String normalized = trimToNull(regionScopeValue);
        if (normalized == null) {
            return ClubRegionScope.NATIONWIDE;
        }
        try {
            return ClubRegionScope.valueOf(normalized.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new SemoException.ValidationException("지원하지 않는 지역 범위입니다.");
        }
    }

    private void ensureCatalogLoaded() {
        if (regionByDepth1Code != null) {
            return;
        }
        try (InputStream inputStream = new ClassPathResource("club/korea-regions.json").getInputStream()) {
            List<RegionEntryJson> entries = objectMapper.readValue(inputStream, new TypeReference<>() {
            });
            List<RegionEntry> normalizedEntries = entries.stream()
                    .map(entry -> new RegionEntry(
                            entry.depth1Code(),
                            entry.depth1Name(),
                            entry.depth2List(),
                            entry.depth2List().stream().collect(Collectors.toUnmodifiableMap(Depth2Entry::depth2Code, Function.identity())),
                            entry.depth2List().stream().collect(Collectors.toUnmodifiableMap(Depth2Entry::depth2Name, Function.identity()))
                    ))
                    .toList();
            regionByDepth1Code = normalizedEntries.stream()
                    .collect(Collectors.toUnmodifiableMap(RegionEntry::depth1Code, Function.identity()));
            regionByDepth1Name = normalizedEntries.stream()
                    .collect(Collectors.toUnmodifiableMap(RegionEntry::depth1Name, Function.identity()));
        } catch (IOException exception) {
            throw new IllegalStateException("대한민국 지역 카탈로그를 불러오지 못했습니다.", exception);
        }
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private record RegionEntryJson(
            String depth1Code,
            String depth1Name,
            List<Depth2Entry> depth2List
    ) {
    }

    private record RegionEntry(
            String depth1Code,
            String depth1Name,
            List<Depth2Entry> depth2List,
            Map<String, Depth2Entry> depth2ByCode,
            Map<String, Depth2Entry> depth2ByName
    ) {
    }

    private record Depth2Entry(
            String depth2Code,
            String depth2Name
    ) {
    }

    public record ResolvedClubRegion(
            String regionScope,
            String regionDepth1Code,
            String regionDepth2Code,
            String regionDepth1Name,
            String regionDepth2Name,
            String regionLabel
    ) {
    }
}
