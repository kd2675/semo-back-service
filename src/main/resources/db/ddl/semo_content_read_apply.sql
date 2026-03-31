-- semo content read apply
-- 운영 중인 DB에 게시판/캘린더 읽음 추적 테이블을 추가하기 위한 적용 스크립트입니다.

USE SEMO;

CREATE TABLE IF NOT EXISTS club_board_item_read (
    club_board_item_read_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    board_item_id BIGINT NOT NULL,
    club_profile_id BIGINT NOT NULL,
    first_read_at DATETIME NOT NULL,
    last_read_at DATETIME NOT NULL,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    CONSTRAINT uk_club_board_item_read UNIQUE (board_item_id, club_profile_id),
    CONSTRAINT fk_club_board_item_read_item FOREIGN KEY (board_item_id) REFERENCES club_board_item(board_item_id),
    CONSTRAINT fk_club_board_item_read_profile FOREIGN KEY (club_profile_id) REFERENCES club_profile(club_profile_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_club_board_item_read_item
    ON club_board_item_read (board_item_id, club_profile_id);

CREATE TABLE IF NOT EXISTS club_calendar_item_read (
    club_calendar_item_read_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    calendar_item_id BIGINT NOT NULL,
    club_profile_id BIGINT NOT NULL,
    first_read_at DATETIME NOT NULL,
    last_read_at DATETIME NOT NULL,
    create_date DATETIME NOT NULL,
    update_date DATETIME NOT NULL,
    CONSTRAINT uk_club_calendar_item_read UNIQUE (calendar_item_id, club_profile_id),
    CONSTRAINT fk_club_calendar_item_read_item FOREIGN KEY (calendar_item_id) REFERENCES club_calendar_item(calendar_item_id),
    CONSTRAINT fk_club_calendar_item_read_profile FOREIGN KEY (club_profile_id) REFERENCES club_profile(club_profile_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_club_calendar_item_read_item
    ON club_calendar_item_read (calendar_item_id, club_profile_id);
