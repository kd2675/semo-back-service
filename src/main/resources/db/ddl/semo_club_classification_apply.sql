use SEMO;

SET @ddl = IF(
        EXISTS(
                SELECT 1
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = 'club'
                  AND column_name = 'activity_category'
        ),
        'SELECT 1',
        'ALTER TABLE club ADD COLUMN activity_category VARCHAR(30) NULL AFTER category_key'
           );
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF(
        EXISTS(
                SELECT 1
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = 'club'
                  AND column_name = 'affiliation_type'
        ),
        'SELECT 1',
        'ALTER TABLE club ADD COLUMN affiliation_type VARCHAR(30) NULL AFTER activity_category'
           );
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS club_activity_tag (
    club_activity_tag_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    club_id BIGINT NOT NULL,
    tag_key VARCHAR(30) NOT NULL,
    create_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_club_activity_tag UNIQUE (club_id, tag_key),
    CONSTRAINT fk_club_activity_tag_club FOREIGN KEY (club_id) REFERENCES club(club_id)
);

SET @ddl = IF(
        EXISTS(
                SELECT 1
                FROM information_schema.statistics
                WHERE table_schema = DATABASE()
                  AND table_name = 'club_activity_tag'
                  AND index_name = 'idx_club_activity_tag_club'
        ),
        'SELECT 1',
        'CREATE INDEX idx_club_activity_tag_club ON club_activity_tag (club_id, tag_key)'
           );
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE club
SET activity_category = CASE category_key
        WHEN 'TENNIS' THEN 'SPORTS'
        WHEN 'RUNNING' THEN 'SPORTS'
        WHEN 'HIKING' THEN 'SPORTS'
        WHEN 'CROSSFIT' THEN 'SPORTS'
        WHEN 'CYCLING' THEN 'SPORTS'
        ELSE 'OTHER'
    END
WHERE activity_category IS NULL
   OR activity_category = '';

UPDATE club
SET affiliation_type = 'INDEPENDENT'
WHERE affiliation_type IS NULL
   OR affiliation_type = '';

INSERT INTO club_activity_tag (club_id, tag_key, create_date, update_date)
SELECT club_id, category_key, NOW(), NOW()
FROM club
WHERE category_key IN ('TENNIS', 'RUNNING', 'HIKING', 'CROSSFIT', 'CYCLING')
  AND NOT EXISTS(
        SELECT 1
        FROM club_activity_tag existing
        WHERE existing.club_id = club.club_id
          AND existing.tag_key = club.category_key
    );

SELECT club_id, name, category_key, activity_category, affiliation_type
FROM club
WHERE activity_category IS NULL
   OR affiliation_type IS NULL;
