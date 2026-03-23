package semo.back.service.feature.dashboard.biz;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import semo.back.service.common.exception.SemoException;
import semo.back.service.database.pub.entity.ClubDashboardWidget;
import semo.back.service.database.pub.entity.ClubFeature;
import semo.back.service.database.pub.entity.DashboardWidgetCatalog;
import semo.back.service.database.pub.repository.ClubDashboardWidgetRepository;
import semo.back.service.database.pub.repository.ClubFeatureRepository;
import semo.back.service.database.pub.repository.DashboardWidgetCatalogRepository;
import semo.back.service.feature.club.biz.ClubAccessResolver;
import semo.back.service.feature.dashboard.vo.ClubDashboardEditorResponse;
import semo.back.service.feature.dashboard.vo.ClubDashboardWidgetResponse;
import semo.back.service.feature.dashboard.vo.UpdateClubDashboardLayoutRequest;
import semo.back.service.feature.dashboard.vo.UpdateClubDashboardWidgetItemRequest;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubDashboardService {
    public static final String SCOPE_USER_HOME = "USER_HOME";
    public static final String SCOPE_ADMIN_HOME = "ADMIN_HOME";
    private static final String FEATURE_ATTENDANCE = "ATTENDANCE";
    private static final int MIN_SPAN = 1;
    private static final int MAX_SPAN = 3;
    private static final int TITLE_MAX_LENGTH = 100;

    private final DashboardWidgetCatalogRepository dashboardWidgetCatalogRepository;
    private final ClubDashboardWidgetRepository clubDashboardWidgetRepository;
    private final ClubFeatureRepository clubFeatureRepository;
    private final ClubAccessResolver clubAccessResolver;

    @Transactional(transactionManager = "pubTransactionManager")
    public List<ClubDashboardWidgetResponse> getDashboardWidgets(Long clubId, String userKey, String scope) {
        String normalizedScope = normalizeScope(scope);
        clubAccessResolver.requireActiveMember(clubId, userKey);

        return getEditorWidgetsInternal(clubId, normalizedScope).stream()
                .filter(widget -> widget.enabled() && widget.available())
                .toList();
    }

    @Transactional(transactionManager = "pubTransactionManager")
    public ClubDashboardEditorResponse getDashboardWidgetEditor(Long clubId, String userKey, String scope) {
        String normalizedScope = normalizeScope(scope);
        clubAccessResolver.requireAdmin(clubId, userKey);
        return new ClubDashboardEditorResponse(
                normalizedScope,
                getEditorWidgetsInternal(clubId, normalizedScope)
        );
    }

    @Transactional(transactionManager = "pubTransactionManager", propagation = Propagation.REQUIRES_NEW)
    public ClubDashboardEditorResponse updateDashboardWidgetLayout(
            Long clubId,
            String userKey,
            UpdateClubDashboardLayoutRequest request
    ) {
        clubAccessResolver.requireAdmin(clubId, userKey);
        if (request == null || request.widgets() == null) {
            throw new SemoException.ValidationException("위젯 레이아웃 요청이 비어 있습니다.");
        }

        String normalizedScope = normalizeScope(request.scope());
        List<DashboardWidgetCatalog> catalogs = getCatalogsByScope(normalizedScope);
        Map<String, DashboardWidgetCatalog> catalogByKey = catalogs.stream()
                .collect(Collectors.toMap(
                        DashboardWidgetCatalog::getWidgetKey,
                        catalog -> catalog
                ));
        if (catalogByKey.isEmpty()) {
            throw new SemoException.ValidationException("해당 스코프에 등록된 위젯 카탈로그가 없습니다.");
        }

        ensureWidgetRows(clubId, normalizedScope, catalogs);
        Map<String, ClubDashboardWidget> existingByKey = getWidgetsByScope(clubId, normalizedScope).stream()
                .collect(Collectors.toMap(
                        ClubDashboardWidget::getWidgetKey,
                        widget -> widget
                ));
        Set<String> enabledFeatureKeys = resolveEnabledFeatureKeys(clubId, catalogs);

        List<ClubDashboardWidget> updatedWidgets = request.widgets().stream()
                .map(item -> toUpdatedWidget(clubId, item, existingByKey, catalogByKey, enabledFeatureKeys, normalizedScope))
                .toList();
        clubDashboardWidgetRepository.saveAll(updatedWidgets);

        return new ClubDashboardEditorResponse(
                normalizedScope,
                getEditorWidgetsInternal(clubId, normalizedScope)
        );
    }

    @Transactional(transactionManager = "pubTransactionManager")
    public void syncWidgetsForClub(Long clubId) {
        List<DashboardWidgetCatalog> userScopeCatalogs = getCatalogsByScope(SCOPE_USER_HOME);
        ensureWidgetRows(clubId, SCOPE_USER_HOME, userScopeCatalogs);
        disableUnavailableWidgets(clubId, SCOPE_USER_HOME, userScopeCatalogs);
    }

    private ClubDashboardWidget toUpdatedWidget(
            Long clubId,
            UpdateClubDashboardWidgetItemRequest item,
            Map<String, ClubDashboardWidget> existingByKey,
            Map<String, DashboardWidgetCatalog> catalogByKey,
            Set<String> enabledFeatureKeys,
            String scope
    ) {
        String widgetKey = normalizeWidgetKey(item.widgetKey());
        DashboardWidgetCatalog catalog = catalogByKey.get(widgetKey);
        if (catalog == null) {
            throw new SemoException.ValidationException("지원하지 않는 위젯 키가 포함되어 있습니다: " + widgetKey);
        }

        ClubDashboardWidget existing = existingByKey.get(widgetKey);
        if (existing == null) {
            existing = ClubDashboardWidget.builder()
                    .clubId(clubId)
                    .widgetKey(widgetKey)
                    .visibilityScope(scope)
                    .enabled(!StringUtils.hasText(catalog.getRequiredFeatureKey()))
                    .sortOrder(catalog.getDefaultSortOrder())
                    .columnSpan(catalog.getDefaultColumnSpan())
                    .rowSpan(catalog.getDefaultRowSpan())
                    .build();
        }

        boolean available = isWidgetAvailable(catalog, enabledFeatureKeys);
        boolean enabled = item.enabled() == null ? existing.isEnabled() : item.enabled();
        if (enabled && !available) {
            throw new SemoException.ValidationException(
                    "필수 기능이 활성화되지 않아 위젯을 켤 수 없습니다: " + widgetKey
            );
        }

        return ClubDashboardWidget.builder()
                .clubDashboardWidgetId(existing.getClubDashboardWidgetId())
                .clubId(existing.getClubId())
                .widgetKey(existing.getWidgetKey())
                .titleOverride(item.titleOverride() == null
                        ? existing.getTitleOverride()
                        : normalizeTitleOverride(item.titleOverride()))
                .columnSpan(normalizeSpan(item.columnSpan(), existing.getColumnSpan()))
                .rowSpan(normalizeSpan(item.rowSpan(), existing.getRowSpan()))
                .sortOrder(item.sortOrder() == null ? existing.getSortOrder() : item.sortOrder())
                .enabled(enabled)
                .visibilityScope(scope)
                .build();
    }

    private List<ClubDashboardWidgetResponse> getEditorWidgetsInternal(Long clubId, String scope) {
        List<DashboardWidgetCatalog> catalogs = getCatalogsByScope(scope);
        ensureWidgetRows(clubId, scope, catalogs);

        Map<String, DashboardWidgetCatalog> catalogByKey = catalogs.stream()
                .collect(Collectors.toMap(
                        DashboardWidgetCatalog::getWidgetKey,
                        catalog -> catalog
                ));
        Set<String> enabledFeatureKeys = resolveEnabledFeatureKeys(clubId, catalogs);

        return getWidgetsByScope(clubId, scope).stream()
                .map(widget -> {
                    DashboardWidgetCatalog catalog = catalogByKey.get(widget.getWidgetKey());
                    if (catalog == null) {
                        return null;
                    }
                    return toWidgetResponse(clubId, scope, widget, catalog, enabledFeatureKeys);
                })
                .filter(response -> response != null)
                .sorted(Comparator
                        .comparingInt(ClubDashboardWidgetResponse::sortOrder)
                        .thenComparing(ClubDashboardWidgetResponse::widgetKey))
                .toList();
    }

    private ClubDashboardWidgetResponse toWidgetResponse(
            Long clubId,
            String scope,
            ClubDashboardWidget widget,
            DashboardWidgetCatalog catalog,
            Set<String> enabledFeatureKeys
    ) {
        boolean available = isWidgetAvailable(catalog, enabledFeatureKeys);
        boolean enabled = widget.isEnabled() && available;
        return new ClubDashboardWidgetResponse(
                catalog.getWidgetKey(),
                catalog.getDisplayName(),
                catalog.getDescription(),
                catalog.getIconName(),
                normalizeFeatureKey(catalog.getRequiredFeatureKey()),
                scope,
                available,
                enabled,
                widget.getSortOrder(),
                widget.getColumnSpan(),
                widget.getRowSpan(),
                StringUtils.hasText(widget.getTitleOverride()) ? widget.getTitleOverride() : catalog.getDisplayName(),
                toUserPath(clubId, catalog.getWidgetKey()),
                toAdminPath(clubId, catalog.getWidgetKey())
        );
    }

    private void disableUnavailableWidgets(Long clubId, String scope, List<DashboardWidgetCatalog> catalogs) {
        if (catalogs.isEmpty()) {
            return;
        }

        Map<String, DashboardWidgetCatalog> catalogByKey = catalogs.stream()
                .collect(Collectors.toMap(
                        DashboardWidgetCatalog::getWidgetKey,
                        catalog -> catalog
                ));
        Set<String> enabledFeatureKeys = resolveEnabledFeatureKeys(clubId, catalogs);

        List<ClubDashboardWidget> changed = getWidgetsByScope(clubId, scope).stream()
                .filter(ClubDashboardWidget::isEnabled)
                .map(widget -> {
                    DashboardWidgetCatalog catalog = catalogByKey.get(widget.getWidgetKey());
                    if (catalog == null || isWidgetAvailable(catalog, enabledFeatureKeys)) {
                        return null;
                    }
                    return ClubDashboardWidget.builder()
                            .clubDashboardWidgetId(widget.getClubDashboardWidgetId())
                            .clubId(widget.getClubId())
                            .widgetKey(widget.getWidgetKey())
                            .titleOverride(widget.getTitleOverride())
                            .columnSpan(widget.getColumnSpan())
                            .rowSpan(widget.getRowSpan())
                            .sortOrder(widget.getSortOrder())
                            .enabled(false)
                            .visibilityScope(widget.getVisibilityScope())
                            .build();
                })
                .filter(widget -> widget != null)
                .toList();

        if (!changed.isEmpty()) {
            clubDashboardWidgetRepository.saveAll(changed);
        }
    }

    private void ensureWidgetRows(Long clubId, String scope, List<DashboardWidgetCatalog> catalogs) {
        if (catalogs.isEmpty()) {
            return;
        }

        Map<String, ClubDashboardWidget> existingByKey = getWidgetsByScope(clubId, scope).stream()
                .collect(Collectors.toMap(
                        ClubDashboardWidget::getWidgetKey,
                        widget -> widget
                ));

        List<ClubDashboardWidget> missingWidgets = catalogs.stream()
                .filter(catalog -> !existingByKey.containsKey(catalog.getWidgetKey()))
                .map(catalog -> ClubDashboardWidget.builder()
                        .clubId(clubId)
                        .widgetKey(catalog.getWidgetKey())
                        .titleOverride(null)
                        .columnSpan(catalog.getDefaultColumnSpan())
                        .rowSpan(catalog.getDefaultRowSpan())
                        .sortOrder(catalog.getDefaultSortOrder())
                        .enabled(!StringUtils.hasText(catalog.getRequiredFeatureKey()))
                        .visibilityScope(scope)
                        .build())
                .toList();
        if (!missingWidgets.isEmpty()) {
            clubDashboardWidgetRepository.saveAll(missingWidgets);
        }
    }

    private List<ClubDashboardWidget> getWidgetsByScope(Long clubId, String scope) {
        return clubDashboardWidgetRepository.findByClubIdOrderBySortOrderAscWidgetKeyAsc(clubId).stream()
                .filter(widget -> isSameScope(widget.getVisibilityScope(), scope))
                .toList();
    }

    private List<DashboardWidgetCatalog> getCatalogsByScope(String scope) {
        return dashboardWidgetCatalogRepository.findByActiveTrueOrderByDefaultSortOrderAscWidgetKeyAsc().stream()
                .filter(catalog -> normalizeScope(catalog.getDefaultVisibilityScope()).equals(scope))
                .toList();
    }

    private Set<String> resolveEnabledFeatureKeys(Long clubId, List<DashboardWidgetCatalog> catalogs) {
        Set<String> requiredFeatureKeys = catalogs.stream()
                .map(DashboardWidgetCatalog::getRequiredFeatureKey)
                .filter(StringUtils::hasText)
                .map(this::normalizeFeatureKey)
                .collect(Collectors.toSet());
        if (requiredFeatureKeys.isEmpty()) {
            return Set.of();
        }

        return clubFeatureRepository.findByClubIdAndFeatureKeyIn(clubId, requiredFeatureKeys).stream()
                .filter(ClubFeature::isEnabled)
                .map(ClubFeature::getFeatureKey)
                .map(this::normalizeFeatureKey)
                .collect(Collectors.toSet());
    }

    private boolean isWidgetAvailable(DashboardWidgetCatalog catalog, Set<String> enabledFeatureKeys) {
        String requiredFeatureKey = normalizeFeatureKey(catalog.getRequiredFeatureKey());
        if (!StringUtils.hasText(requiredFeatureKey)) {
            return true;
        }
        return enabledFeatureKeys.contains(requiredFeatureKey);
    }

    private int normalizeSpan(Integer value, int fallback) {
        if (value == null) {
            return fallback;
        }
        if (value < MIN_SPAN || value > MAX_SPAN) {
            throw new SemoException.ValidationException("위젯 span은 1~3 범위여야 합니다.");
        }
        return value;
    }

    private String normalizeTitleOverride(String titleOverride) {
        if (!StringUtils.hasText(titleOverride)) {
            return null;
        }
        String normalized = titleOverride.trim();
        if (normalized.length() > TITLE_MAX_LENGTH) {
            throw new SemoException.ValidationException("위젯 제목은 100자 이하여야 합니다.");
        }
        return normalized;
    }

    private String toUserPath(Long clubId, String widgetKey) {
        return switch (normalizeWidgetKey(widgetKey)) {
            case "BOARD_NOTICE" -> "/clubs/%d/board".formatted(clubId);
            case "SCHEDULE_OVERVIEW" -> "/clubs/%d/schedule".formatted(clubId);
            case "POLL_STATUS" -> "/clubs/%d/more/polls".formatted(clubId);
            case "PROFILE_SUMMARY" -> "/clubs/%d/profile".formatted(clubId);
            case "ATTENDANCE_STATUS" -> "/clubs/%d/more/attendance".formatted(clubId);
            default -> "/clubs/%d".formatted(clubId);
        };
    }

    private String toAdminPath(Long clubId, String widgetKey) {
        return switch (normalizeWidgetKey(widgetKey)) {
            case "BOARD_NOTICE" -> "/clubs/%d/board".formatted(clubId);
            case "SCHEDULE_OVERVIEW" -> "/clubs/%d/schedule".formatted(clubId);
            case "POLL_STATUS" -> "/clubs/%d/admin/more/polls".formatted(clubId);
            case "PROFILE_SUMMARY" -> "/clubs/%d/profile".formatted(clubId);
            case "ATTENDANCE_STATUS" -> "/clubs/%d/admin/more/attendance".formatted(clubId);
            default -> "/clubs/%d/admin".formatted(clubId);
        };
    }

    private String normalizeWidgetKey(String widgetKey) {
        if (!StringUtils.hasText(widgetKey)) {
            return "";
        }
        return widgetKey.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeFeatureKey(String featureKey) {
        if (!StringUtils.hasText(featureKey)) {
            return "";
        }
        return featureKey.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeScope(String scope) {
        if (!StringUtils.hasText(scope)) {
            return SCOPE_USER_HOME;
        }

        String normalized = scope.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case SCOPE_USER_HOME, "USER" -> SCOPE_USER_HOME;
            case SCOPE_ADMIN_HOME, "ADMIN" -> SCOPE_ADMIN_HOME;
            default -> throw new SemoException.ValidationException("지원하지 않는 위젯 스코프입니다.");
        };
    }

    private boolean isSameScope(String currentScope, String targetScope) {
        try {
            return normalizeScope(currentScope).equals(targetScope);
        } catch (SemoException.ValidationException ignored) {
            return false;
        }
    }
}
