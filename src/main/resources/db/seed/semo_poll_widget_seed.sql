USE SEMO;

INSERT INTO dashboard_widget_catalog (
    widget_key,
    display_name,
    description,
    icon_name,
    required_feature_key,
    default_visibility_scope,
    default_column_span,
    default_row_span,
    default_sort_order,
    active,
    create_date,
    update_date
)
SELECT 'POLL_STATUS', 'Poll Status', 'Latest ongoing poll for your club.', 'poll', 'POLL', 'USER_HOME', 1, 1, 25, 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM dashboard_widget_catalog WHERE widget_key = 'POLL_STATUS');
