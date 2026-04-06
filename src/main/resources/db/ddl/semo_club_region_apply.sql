use SEMO;

SET @ddl = IF(
        EXISTS(
                SELECT 1
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = 'club'
                  AND column_name = 'region_scope'
        ),
        'SELECT 1',
        'ALTER TABLE club ADD COLUMN region_scope VARCHAR(20) NOT NULL DEFAULT ''NATIONWIDE'' AFTER membership_policy'
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
                  AND column_name = 'region_depth1_code'
        ),
        'SELECT 1',
        'ALTER TABLE club ADD COLUMN region_depth1_code VARCHAR(10) NULL AFTER region_scope'
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
                  AND column_name = 'region_depth2_code'
        ),
        'SELECT 1',
        'ALTER TABLE club ADD COLUMN region_depth2_code VARCHAR(10) NULL AFTER region_depth1_code'
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
                  AND column_name = 'region_depth1_name'
        ),
        'SELECT 1',
        'ALTER TABLE club ADD COLUMN region_depth1_name VARCHAR(60) NULL AFTER region_depth2_code'
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
                  AND column_name = 'region_depth2_name'
        ),
        'SELECT 1',
        'ALTER TABLE club ADD COLUMN region_depth2_name VARCHAR(60) NULL AFTER region_depth1_name'
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
                  AND column_name = 'region_label'
        ),
        'SELECT 1',
        'ALTER TABLE club ADD COLUMN region_label VARCHAR(140) NOT NULL DEFAULT ''전국'' AFTER region_depth2_name'
           );
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE club
SET region_scope = 'NATIONWIDE',
    region_label = '전국'
WHERE region_scope IS NULL
   OR region_label IS NULL
   OR region_label = '';

UPDATE club
SET region_depth1_code = NULL,
    region_depth2_code = NULL
WHERE region_scope IN ('ONLINE', 'NATIONWIDE');

UPDATE club SET region_depth1_code = '51' WHERE region_depth1_name = '강원특별자치도';
UPDATE club SET region_depth1_code = '41' WHERE region_depth1_name = '경기도';
UPDATE club SET region_depth1_code = '48' WHERE region_depth1_name = '경상남도';
UPDATE club SET region_depth1_code = '47' WHERE region_depth1_name = '경상북도';
UPDATE club SET region_depth1_code = '29' WHERE region_depth1_name = '광주광역시';
UPDATE club SET region_depth1_code = '27' WHERE region_depth1_name = '대구광역시';
UPDATE club SET region_depth1_code = '30' WHERE region_depth1_name = '대전광역시';
UPDATE club SET region_depth1_code = '26' WHERE region_depth1_name = '부산광역시';
UPDATE club SET region_depth1_code = '11' WHERE region_depth1_name = '서울특별시';
UPDATE club SET region_depth1_code = '36' WHERE region_depth1_name = '세종특별자치시';
UPDATE club SET region_depth1_code = '31' WHERE region_depth1_name = '울산광역시';
UPDATE club SET region_depth1_code = '28' WHERE region_depth1_name = '인천광역시';
UPDATE club SET region_depth1_code = '46' WHERE region_depth1_name = '전라남도';
UPDATE club SET region_depth1_code = '52' WHERE region_depth1_name = '전북특별자치도';
UPDATE club SET region_depth1_code = '50' WHERE region_depth1_name = '제주특별자치도';
UPDATE club SET region_depth1_code = '44' WHERE region_depth1_name = '충청남도';
UPDATE club SET region_depth1_code = '43' WHERE region_depth1_name = '충청북도';

UPDATE club SET region_depth2_code = '51150' WHERE region_depth1_name = '강원특별자치도' AND region_depth2_name = '강릉시';
UPDATE club SET region_depth2_code = '51820' WHERE region_depth1_name = '강원특별자치도' AND region_depth2_name = '고성군';
UPDATE club SET region_depth2_code = '51170' WHERE region_depth1_name = '강원특별자치도' AND region_depth2_name = '동해시';
UPDATE club SET region_depth2_code = '51230' WHERE region_depth1_name = '강원특별자치도' AND region_depth2_name = '삼척시';
UPDATE club SET region_depth2_code = '51210' WHERE region_depth1_name = '강원특별자치도' AND region_depth2_name = '속초시';
UPDATE club SET region_depth2_code = '51800' WHERE region_depth1_name = '강원특별자치도' AND region_depth2_name = '양구군';
UPDATE club SET region_depth2_code = '51830' WHERE region_depth1_name = '강원특별자치도' AND region_depth2_name = '양양군';
UPDATE club SET region_depth2_code = '51750' WHERE region_depth1_name = '강원특별자치도' AND region_depth2_name = '영월군';
UPDATE club SET region_depth2_code = '51130' WHERE region_depth1_name = '강원특별자치도' AND region_depth2_name = '원주시';
UPDATE club SET region_depth2_code = '51810' WHERE region_depth1_name = '강원특별자치도' AND region_depth2_name = '인제군';
UPDATE club SET region_depth2_code = '51770' WHERE region_depth1_name = '강원특별자치도' AND region_depth2_name = '정선군';
UPDATE club SET region_depth2_code = '51780' WHERE region_depth1_name = '강원특별자치도' AND region_depth2_name = '철원군';
UPDATE club SET region_depth2_code = '51110' WHERE region_depth1_name = '강원특별자치도' AND region_depth2_name = '춘천시';
UPDATE club SET region_depth2_code = '51190' WHERE region_depth1_name = '강원특별자치도' AND region_depth2_name = '태백시';
UPDATE club SET region_depth2_code = '51760' WHERE region_depth1_name = '강원특별자치도' AND region_depth2_name = '평창군';
UPDATE club SET region_depth2_code = '51720' WHERE region_depth1_name = '강원특별자치도' AND region_depth2_name = '홍천군';
UPDATE club SET region_depth2_code = '51790' WHERE region_depth1_name = '강원특별자치도' AND region_depth2_name = '화천군';
UPDATE club SET region_depth2_code = '51730' WHERE region_depth1_name = '강원특별자치도' AND region_depth2_name = '횡성군';
UPDATE club SET region_depth2_code = '41820' WHERE region_depth1_name = '경기도' AND region_depth2_name = '가평군';
UPDATE club SET region_depth2_code = '41280' WHERE region_depth1_name = '경기도' AND region_depth2_name = '고양시';
UPDATE club SET region_depth2_code = '41290' WHERE region_depth1_name = '경기도' AND region_depth2_name = '과천시';
UPDATE club SET region_depth2_code = '41210' WHERE region_depth1_name = '경기도' AND region_depth2_name = '광명시';
UPDATE club SET region_depth2_code = '41610' WHERE region_depth1_name = '경기도' AND region_depth2_name = '광주시';
UPDATE club SET region_depth2_code = '41310' WHERE region_depth1_name = '경기도' AND region_depth2_name = '구리시';
UPDATE club SET region_depth2_code = '41410' WHERE region_depth1_name = '경기도' AND region_depth2_name = '군포시';
UPDATE club SET region_depth2_code = '41570' WHERE region_depth1_name = '경기도' AND region_depth2_name = '김포시';
UPDATE club SET region_depth2_code = '41360' WHERE region_depth1_name = '경기도' AND region_depth2_name = '남양주시';
UPDATE club SET region_depth2_code = '41250' WHERE region_depth1_name = '경기도' AND region_depth2_name = '동두천시';
UPDATE club SET region_depth2_code = '41190' WHERE region_depth1_name = '경기도' AND region_depth2_name = '부천시';
UPDATE club SET region_depth2_code = '41130' WHERE region_depth1_name = '경기도' AND region_depth2_name = '성남시';
UPDATE club SET region_depth2_code = '41110' WHERE region_depth1_name = '경기도' AND region_depth2_name = '수원시';
UPDATE club SET region_depth2_code = '41390' WHERE region_depth1_name = '경기도' AND region_depth2_name = '시흥시';
UPDATE club SET region_depth2_code = '41270' WHERE region_depth1_name = '경기도' AND region_depth2_name = '안산시';
UPDATE club SET region_depth2_code = '41550' WHERE region_depth1_name = '경기도' AND region_depth2_name = '안성시';
UPDATE club SET region_depth2_code = '41170' WHERE region_depth1_name = '경기도' AND region_depth2_name = '안양시';
UPDATE club SET region_depth2_code = '41630' WHERE region_depth1_name = '경기도' AND region_depth2_name = '양주시';
UPDATE club SET region_depth2_code = '41830' WHERE region_depth1_name = '경기도' AND region_depth2_name = '양평군';
UPDATE club SET region_depth2_code = '41670' WHERE region_depth1_name = '경기도' AND region_depth2_name = '여주시';
UPDATE club SET region_depth2_code = '41800' WHERE region_depth1_name = '경기도' AND region_depth2_name = '연천군';
UPDATE club SET region_depth2_code = '41370' WHERE region_depth1_name = '경기도' AND region_depth2_name = '오산시';
UPDATE club SET region_depth2_code = '41460' WHERE region_depth1_name = '경기도' AND region_depth2_name = '용인시';
UPDATE club SET region_depth2_code = '41430' WHERE region_depth1_name = '경기도' AND region_depth2_name = '의왕시';
UPDATE club SET region_depth2_code = '41150' WHERE region_depth1_name = '경기도' AND region_depth2_name = '의정부시';
UPDATE club SET region_depth2_code = '41500' WHERE region_depth1_name = '경기도' AND region_depth2_name = '이천시';
UPDATE club SET region_depth2_code = '41480' WHERE region_depth1_name = '경기도' AND region_depth2_name = '파주시';
UPDATE club SET region_depth2_code = '41220' WHERE region_depth1_name = '경기도' AND region_depth2_name = '평택시';
UPDATE club SET region_depth2_code = '41650' WHERE region_depth1_name = '경기도' AND region_depth2_name = '포천시';
UPDATE club SET region_depth2_code = '41450' WHERE region_depth1_name = '경기도' AND region_depth2_name = '하남시';
UPDATE club SET region_depth2_code = '41590' WHERE region_depth1_name = '경기도' AND region_depth2_name = '화성시';
UPDATE club SET region_depth2_code = '48310' WHERE region_depth1_name = '경상남도' AND region_depth2_name = '거제시';
UPDATE club SET region_depth2_code = '48880' WHERE region_depth1_name = '경상남도' AND region_depth2_name = '거창군';
UPDATE club SET region_depth2_code = '48820' WHERE region_depth1_name = '경상남도' AND region_depth2_name = '고성군';
UPDATE club SET region_depth2_code = '48250' WHERE region_depth1_name = '경상남도' AND region_depth2_name = '김해시';
UPDATE club SET region_depth2_code = '48840' WHERE region_depth1_name = '경상남도' AND region_depth2_name = '남해군';
UPDATE club SET region_depth2_code = '48270' WHERE region_depth1_name = '경상남도' AND region_depth2_name = '밀양시';
UPDATE club SET region_depth2_code = '48240' WHERE region_depth1_name = '경상남도' AND region_depth2_name = '사천시';
UPDATE club SET region_depth2_code = '48860' WHERE region_depth1_name = '경상남도' AND region_depth2_name = '산청군';
UPDATE club SET region_depth2_code = '48330' WHERE region_depth1_name = '경상남도' AND region_depth2_name = '양산시';
UPDATE club SET region_depth2_code = '48720' WHERE region_depth1_name = '경상남도' AND region_depth2_name = '의령군';
UPDATE club SET region_depth2_code = '48170' WHERE region_depth1_name = '경상남도' AND region_depth2_name = '진주시';
UPDATE club SET region_depth2_code = '48740' WHERE region_depth1_name = '경상남도' AND region_depth2_name = '창녕군';
UPDATE club SET region_depth2_code = '48120' WHERE region_depth1_name = '경상남도' AND region_depth2_name = '창원시';
UPDATE club SET region_depth2_code = '48220' WHERE region_depth1_name = '경상남도' AND region_depth2_name = '통영시';
UPDATE club SET region_depth2_code = '48850' WHERE region_depth1_name = '경상남도' AND region_depth2_name = '하동군';
UPDATE club SET region_depth2_code = '48730' WHERE region_depth1_name = '경상남도' AND region_depth2_name = '함안군';
UPDATE club SET region_depth2_code = '48870' WHERE region_depth1_name = '경상남도' AND region_depth2_name = '함양군';
UPDATE club SET region_depth2_code = '48890' WHERE region_depth1_name = '경상남도' AND region_depth2_name = '합천군';
UPDATE club SET region_depth2_code = '47290' WHERE region_depth1_name = '경상북도' AND region_depth2_name = '경산시';
UPDATE club SET region_depth2_code = '47130' WHERE region_depth1_name = '경상북도' AND region_depth2_name = '경주시';
UPDATE club SET region_depth2_code = '47830' WHERE region_depth1_name = '경상북도' AND region_depth2_name = '고령군';
UPDATE club SET region_depth2_code = '47190' WHERE region_depth1_name = '경상북도' AND region_depth2_name = '구미시';
UPDATE club SET region_depth2_code = '47150' WHERE region_depth1_name = '경상북도' AND region_depth2_name = '김천시';
UPDATE club SET region_depth2_code = '47280' WHERE region_depth1_name = '경상북도' AND region_depth2_name = '문경시';
UPDATE club SET region_depth2_code = '47920' WHERE region_depth1_name = '경상북도' AND region_depth2_name = '봉화군';
UPDATE club SET region_depth2_code = '47250' WHERE region_depth1_name = '경상북도' AND region_depth2_name = '상주시';
UPDATE club SET region_depth2_code = '47840' WHERE region_depth1_name = '경상북도' AND region_depth2_name = '성주군';
UPDATE club SET region_depth2_code = '47170' WHERE region_depth1_name = '경상북도' AND region_depth2_name = '안동시';
UPDATE club SET region_depth2_code = '47770' WHERE region_depth1_name = '경상북도' AND region_depth2_name = '영덕군';
UPDATE club SET region_depth2_code = '47760' WHERE region_depth1_name = '경상북도' AND region_depth2_name = '영양군';
UPDATE club SET region_depth2_code = '47210' WHERE region_depth1_name = '경상북도' AND region_depth2_name = '영주시';
UPDATE club SET region_depth2_code = '47230' WHERE region_depth1_name = '경상북도' AND region_depth2_name = '영천시';
UPDATE club SET region_depth2_code = '47900' WHERE region_depth1_name = '경상북도' AND region_depth2_name = '예천군';
UPDATE club SET region_depth2_code = '47940' WHERE region_depth1_name = '경상북도' AND region_depth2_name = '울릉군';
UPDATE club SET region_depth2_code = '47930' WHERE region_depth1_name = '경상북도' AND region_depth2_name = '울진군';
UPDATE club SET region_depth2_code = '47730' WHERE region_depth1_name = '경상북도' AND region_depth2_name = '의성군';
UPDATE club SET region_depth2_code = '47820' WHERE region_depth1_name = '경상북도' AND region_depth2_name = '청도군';
UPDATE club SET region_depth2_code = '47750' WHERE region_depth1_name = '경상북도' AND region_depth2_name = '청송군';
UPDATE club SET region_depth2_code = '47850' WHERE region_depth1_name = '경상북도' AND region_depth2_name = '칠곡군';
UPDATE club SET region_depth2_code = '47110' WHERE region_depth1_name = '경상북도' AND region_depth2_name = '포항시';
UPDATE club SET region_depth2_code = '29200' WHERE region_depth1_name = '광주광역시' AND region_depth2_name = '광산구';
UPDATE club SET region_depth2_code = '29155' WHERE region_depth1_name = '광주광역시' AND region_depth2_name = '남구';
UPDATE club SET region_depth2_code = '29110' WHERE region_depth1_name = '광주광역시' AND region_depth2_name = '동구';
UPDATE club SET region_depth2_code = '29170' WHERE region_depth1_name = '광주광역시' AND region_depth2_name = '북구';
UPDATE club SET region_depth2_code = '29140' WHERE region_depth1_name = '광주광역시' AND region_depth2_name = '서구';
UPDATE club SET region_depth2_code = '27720' WHERE region_depth1_name = '대구광역시' AND region_depth2_name = '군위군';
UPDATE club SET region_depth2_code = '27200' WHERE region_depth1_name = '대구광역시' AND region_depth2_name = '남구';
UPDATE club SET region_depth2_code = '27290' WHERE region_depth1_name = '대구광역시' AND region_depth2_name = '달서구';
UPDATE club SET region_depth2_code = '27710' WHERE region_depth1_name = '대구광역시' AND region_depth2_name = '달성군';
UPDATE club SET region_depth2_code = '27140' WHERE region_depth1_name = '대구광역시' AND region_depth2_name = '동구';
UPDATE club SET region_depth2_code = '27230' WHERE region_depth1_name = '대구광역시' AND region_depth2_name = '북구';
UPDATE club SET region_depth2_code = '27170' WHERE region_depth1_name = '대구광역시' AND region_depth2_name = '서구';
UPDATE club SET region_depth2_code = '27260' WHERE region_depth1_name = '대구광역시' AND region_depth2_name = '수성구';
UPDATE club SET region_depth2_code = '27110' WHERE region_depth1_name = '대구광역시' AND region_depth2_name = '중구';
UPDATE club SET region_depth2_code = '30230' WHERE region_depth1_name = '대전광역시' AND region_depth2_name = '대덕구';
UPDATE club SET region_depth2_code = '30110' WHERE region_depth1_name = '대전광역시' AND region_depth2_name = '동구';
UPDATE club SET region_depth2_code = '30170' WHERE region_depth1_name = '대전광역시' AND region_depth2_name = '서구';
UPDATE club SET region_depth2_code = '30200' WHERE region_depth1_name = '대전광역시' AND region_depth2_name = '유성구';
UPDATE club SET region_depth2_code = '30140' WHERE region_depth1_name = '대전광역시' AND region_depth2_name = '중구';
UPDATE club SET region_depth2_code = '26440' WHERE region_depth1_name = '부산광역시' AND region_depth2_name = '강서구';
UPDATE club SET region_depth2_code = '26410' WHERE region_depth1_name = '부산광역시' AND region_depth2_name = '금정구';
UPDATE club SET region_depth2_code = '26710' WHERE region_depth1_name = '부산광역시' AND region_depth2_name = '기장군';
UPDATE club SET region_depth2_code = '26290' WHERE region_depth1_name = '부산광역시' AND region_depth2_name = '남구';
UPDATE club SET region_depth2_code = '26170' WHERE region_depth1_name = '부산광역시' AND region_depth2_name = '동구';
UPDATE club SET region_depth2_code = '26260' WHERE region_depth1_name = '부산광역시' AND region_depth2_name = '동래구';
UPDATE club SET region_depth2_code = '26230' WHERE region_depth1_name = '부산광역시' AND region_depth2_name = '부산진구';
UPDATE club SET region_depth2_code = '26320' WHERE region_depth1_name = '부산광역시' AND region_depth2_name = '북구';
UPDATE club SET region_depth2_code = '26530' WHERE region_depth1_name = '부산광역시' AND region_depth2_name = '사상구';
UPDATE club SET region_depth2_code = '26380' WHERE region_depth1_name = '부산광역시' AND region_depth2_name = '사하구';
UPDATE club SET region_depth2_code = '26140' WHERE region_depth1_name = '부산광역시' AND region_depth2_name = '서구';
UPDATE club SET region_depth2_code = '26500' WHERE region_depth1_name = '부산광역시' AND region_depth2_name = '수영구';
UPDATE club SET region_depth2_code = '26470' WHERE region_depth1_name = '부산광역시' AND region_depth2_name = '연제구';
UPDATE club SET region_depth2_code = '26200' WHERE region_depth1_name = '부산광역시' AND region_depth2_name = '영도구';
UPDATE club SET region_depth2_code = '26110' WHERE region_depth1_name = '부산광역시' AND region_depth2_name = '중구';
UPDATE club SET region_depth2_code = '26350' WHERE region_depth1_name = '부산광역시' AND region_depth2_name = '해운대구';
UPDATE club SET region_depth2_code = '11680' WHERE region_depth1_name = '서울특별시' AND region_depth2_name = '강남구';
UPDATE club SET region_depth2_code = '11740' WHERE region_depth1_name = '서울특별시' AND region_depth2_name = '강동구';
UPDATE club SET region_depth2_code = '11305' WHERE region_depth1_name = '서울특별시' AND region_depth2_name = '강북구';
UPDATE club SET region_depth2_code = '11500' WHERE region_depth1_name = '서울특별시' AND region_depth2_name = '강서구';
UPDATE club SET region_depth2_code = '11620' WHERE region_depth1_name = '서울특별시' AND region_depth2_name = '관악구';
UPDATE club SET region_depth2_code = '11215' WHERE region_depth1_name = '서울특별시' AND region_depth2_name = '광진구';
UPDATE club SET region_depth2_code = '11530' WHERE region_depth1_name = '서울특별시' AND region_depth2_name = '구로구';
UPDATE club SET region_depth2_code = '11545' WHERE region_depth1_name = '서울특별시' AND region_depth2_name = '금천구';
UPDATE club SET region_depth2_code = '11350' WHERE region_depth1_name = '서울특별시' AND region_depth2_name = '노원구';
UPDATE club SET region_depth2_code = '11320' WHERE region_depth1_name = '서울특별시' AND region_depth2_name = '도봉구';
UPDATE club SET region_depth2_code = '11230' WHERE region_depth1_name = '서울특별시' AND region_depth2_name = '동대문구';
UPDATE club SET region_depth2_code = '11590' WHERE region_depth1_name = '서울특별시' AND region_depth2_name = '동작구';
UPDATE club SET region_depth2_code = '11440' WHERE region_depth1_name = '서울특별시' AND region_depth2_name = '마포구';
UPDATE club SET region_depth2_code = '11410' WHERE region_depth1_name = '서울특별시' AND region_depth2_name = '서대문구';
UPDATE club SET region_depth2_code = '11650' WHERE region_depth1_name = '서울특별시' AND region_depth2_name = '서초구';
UPDATE club SET region_depth2_code = '11200' WHERE region_depth1_name = '서울특별시' AND region_depth2_name = '성동구';
UPDATE club SET region_depth2_code = '11290' WHERE region_depth1_name = '서울특별시' AND region_depth2_name = '성북구';
UPDATE club SET region_depth2_code = '11710' WHERE region_depth1_name = '서울특별시' AND region_depth2_name = '송파구';
UPDATE club SET region_depth2_code = '11470' WHERE region_depth1_name = '서울특별시' AND region_depth2_name = '양천구';
UPDATE club SET region_depth2_code = '11560' WHERE region_depth1_name = '서울특별시' AND region_depth2_name = '영등포구';
UPDATE club SET region_depth2_code = '11170' WHERE region_depth1_name = '서울특별시' AND region_depth2_name = '용산구';
UPDATE club SET region_depth2_code = '11380' WHERE region_depth1_name = '서울특별시' AND region_depth2_name = '은평구';
UPDATE club SET region_depth2_code = '11110' WHERE region_depth1_name = '서울특별시' AND region_depth2_name = '종로구';
UPDATE club SET region_depth2_code = '11140' WHERE region_depth1_name = '서울특별시' AND region_depth2_name = '중구';
UPDATE club SET region_depth2_code = '11260' WHERE region_depth1_name = '서울특별시' AND region_depth2_name = '중랑구';
UPDATE club SET region_depth2_code = '36110' WHERE region_depth1_name = '세종특별자치시' AND region_depth2_name = '세종특별자치시';
UPDATE club SET region_depth2_code = '31140' WHERE region_depth1_name = '울산광역시' AND region_depth2_name = '남구';
UPDATE club SET region_depth2_code = '31170' WHERE region_depth1_name = '울산광역시' AND region_depth2_name = '동구';
UPDATE club SET region_depth2_code = '31200' WHERE region_depth1_name = '울산광역시' AND region_depth2_name = '북구';
UPDATE club SET region_depth2_code = '31710' WHERE region_depth1_name = '울산광역시' AND region_depth2_name = '울주군';
UPDATE club SET region_depth2_code = '31110' WHERE region_depth1_name = '울산광역시' AND region_depth2_name = '중구';
UPDATE club SET region_depth2_code = '28710' WHERE region_depth1_name = '인천광역시' AND region_depth2_name = '강화군';
UPDATE club SET region_depth2_code = '28245' WHERE region_depth1_name = '인천광역시' AND region_depth2_name = '계양구';
UPDATE club SET region_depth2_code = '28200' WHERE region_depth1_name = '인천광역시' AND region_depth2_name = '남동구';
UPDATE club SET region_depth2_code = '28140' WHERE region_depth1_name = '인천광역시' AND region_depth2_name = '동구';
UPDATE club SET region_depth2_code = '28177' WHERE region_depth1_name = '인천광역시' AND region_depth2_name = '미추홀구';
UPDATE club SET region_depth2_code = '28237' WHERE region_depth1_name = '인천광역시' AND region_depth2_name = '부평구';
UPDATE club SET region_depth2_code = '28260' WHERE region_depth1_name = '인천광역시' AND region_depth2_name = '서구';
UPDATE club SET region_depth2_code = '28185' WHERE region_depth1_name = '인천광역시' AND region_depth2_name = '연수구';
UPDATE club SET region_depth2_code = '28720' WHERE region_depth1_name = '인천광역시' AND region_depth2_name = '옹진군';
UPDATE club SET region_depth2_code = '28110' WHERE region_depth1_name = '인천광역시' AND region_depth2_name = '중구';
UPDATE club SET region_depth2_code = '46810' WHERE region_depth1_name = '전라남도' AND region_depth2_name = '강진군';
UPDATE club SET region_depth2_code = '46770' WHERE region_depth1_name = '전라남도' AND region_depth2_name = '고흥군';
UPDATE club SET region_depth2_code = '46720' WHERE region_depth1_name = '전라남도' AND region_depth2_name = '곡성군';
UPDATE club SET region_depth2_code = '46230' WHERE region_depth1_name = '전라남도' AND region_depth2_name = '광양시';
UPDATE club SET region_depth2_code = '46730' WHERE region_depth1_name = '전라남도' AND region_depth2_name = '구례군';
UPDATE club SET region_depth2_code = '46170' WHERE region_depth1_name = '전라남도' AND region_depth2_name = '나주시';
UPDATE club SET region_depth2_code = '46710' WHERE region_depth1_name = '전라남도' AND region_depth2_name = '담양군';
UPDATE club SET region_depth2_code = '46110' WHERE region_depth1_name = '전라남도' AND region_depth2_name = '목포시';
UPDATE club SET region_depth2_code = '46840' WHERE region_depth1_name = '전라남도' AND region_depth2_name = '무안군';
UPDATE club SET region_depth2_code = '46780' WHERE region_depth1_name = '전라남도' AND region_depth2_name = '보성군';
UPDATE club SET region_depth2_code = '46150' WHERE region_depth1_name = '전라남도' AND region_depth2_name = '순천시';
UPDATE club SET region_depth2_code = '46910' WHERE region_depth1_name = '전라남도' AND region_depth2_name = '신안군';
UPDATE club SET region_depth2_code = '46130' WHERE region_depth1_name = '전라남도' AND region_depth2_name = '여수시';
UPDATE club SET region_depth2_code = '46870' WHERE region_depth1_name = '전라남도' AND region_depth2_name = '영광군';
UPDATE club SET region_depth2_code = '46830' WHERE region_depth1_name = '전라남도' AND region_depth2_name = '영암군';
UPDATE club SET region_depth2_code = '46890' WHERE region_depth1_name = '전라남도' AND region_depth2_name = '완도군';
UPDATE club SET region_depth2_code = '46880' WHERE region_depth1_name = '전라남도' AND region_depth2_name = '장성군';
UPDATE club SET region_depth2_code = '46800' WHERE region_depth1_name = '전라남도' AND region_depth2_name = '장흥군';
UPDATE club SET region_depth2_code = '46900' WHERE region_depth1_name = '전라남도' AND region_depth2_name = '진도군';
UPDATE club SET region_depth2_code = '46860' WHERE region_depth1_name = '전라남도' AND region_depth2_name = '함평군';
UPDATE club SET region_depth2_code = '46820' WHERE region_depth1_name = '전라남도' AND region_depth2_name = '해남군';
UPDATE club SET region_depth2_code = '46790' WHERE region_depth1_name = '전라남도' AND region_depth2_name = '화순군';
UPDATE club SET region_depth2_code = '52790' WHERE region_depth1_name = '전북특별자치도' AND region_depth2_name = '고창군';
UPDATE club SET region_depth2_code = '52130' WHERE region_depth1_name = '전북특별자치도' AND region_depth2_name = '군산시';
UPDATE club SET region_depth2_code = '52210' WHERE region_depth1_name = '전북특별자치도' AND region_depth2_name = '김제시';
UPDATE club SET region_depth2_code = '52190' WHERE region_depth1_name = '전북특별자치도' AND region_depth2_name = '남원시';
UPDATE club SET region_depth2_code = '52730' WHERE region_depth1_name = '전북특별자치도' AND region_depth2_name = '무주군';
UPDATE club SET region_depth2_code = '52800' WHERE region_depth1_name = '전북특별자치도' AND region_depth2_name = '부안군';
UPDATE club SET region_depth2_code = '52770' WHERE region_depth1_name = '전북특별자치도' AND region_depth2_name = '순창군';
UPDATE club SET region_depth2_code = '52710' WHERE region_depth1_name = '전북특별자치도' AND region_depth2_name = '완주군';
UPDATE club SET region_depth2_code = '52140' WHERE region_depth1_name = '전북특별자치도' AND region_depth2_name = '익산시';
UPDATE club SET region_depth2_code = '52750' WHERE region_depth1_name = '전북특별자치도' AND region_depth2_name = '임실군';
UPDATE club SET region_depth2_code = '52740' WHERE region_depth1_name = '전북특별자치도' AND region_depth2_name = '장수군';
UPDATE club SET region_depth2_code = '52110' WHERE region_depth1_name = '전북특별자치도' AND region_depth2_name = '전주시';
UPDATE club SET region_depth2_code = '52180' WHERE region_depth1_name = '전북특별자치도' AND region_depth2_name = '정읍시';
UPDATE club SET region_depth2_code = '52720' WHERE region_depth1_name = '전북특별자치도' AND region_depth2_name = '진안군';
UPDATE club SET region_depth2_code = '50130' WHERE region_depth1_name = '제주특별자치도' AND region_depth2_name = '서귀포시';
UPDATE club SET region_depth2_code = '50110' WHERE region_depth1_name = '제주특별자치도' AND region_depth2_name = '제주시';
UPDATE club SET region_depth2_code = '44250' WHERE region_depth1_name = '충청남도' AND region_depth2_name = '계룡시';
UPDATE club SET region_depth2_code = '44150' WHERE region_depth1_name = '충청남도' AND region_depth2_name = '공주시';
UPDATE club SET region_depth2_code = '44710' WHERE region_depth1_name = '충청남도' AND region_depth2_name = '금산군';
UPDATE club SET region_depth2_code = '44230' WHERE region_depth1_name = '충청남도' AND region_depth2_name = '논산시';
UPDATE club SET region_depth2_code = '44270' WHERE region_depth1_name = '충청남도' AND region_depth2_name = '당진시';
UPDATE club SET region_depth2_code = '44180' WHERE region_depth1_name = '충청남도' AND region_depth2_name = '보령시';
UPDATE club SET region_depth2_code = '44760' WHERE region_depth1_name = '충청남도' AND region_depth2_name = '부여군';
UPDATE club SET region_depth2_code = '44210' WHERE region_depth1_name = '충청남도' AND region_depth2_name = '서산시';
UPDATE club SET region_depth2_code = '44770' WHERE region_depth1_name = '충청남도' AND region_depth2_name = '서천군';
UPDATE club SET region_depth2_code = '44200' WHERE region_depth1_name = '충청남도' AND region_depth2_name = '아산시';
UPDATE club SET region_depth2_code = '44810' WHERE region_depth1_name = '충청남도' AND region_depth2_name = '예산군';
UPDATE club SET region_depth2_code = '44130' WHERE region_depth1_name = '충청남도' AND region_depth2_name = '천안시';
UPDATE club SET region_depth2_code = '44790' WHERE region_depth1_name = '충청남도' AND region_depth2_name = '청양군';
UPDATE club SET region_depth2_code = '44825' WHERE region_depth1_name = '충청남도' AND region_depth2_name = '태안군';
UPDATE club SET region_depth2_code = '44800' WHERE region_depth1_name = '충청남도' AND region_depth2_name = '홍성군';
UPDATE club SET region_depth2_code = '43760' WHERE region_depth1_name = '충청북도' AND region_depth2_name = '괴산군';
UPDATE club SET region_depth2_code = '43800' WHERE region_depth1_name = '충청북도' AND region_depth2_name = '단양군';
UPDATE club SET region_depth2_code = '43720' WHERE region_depth1_name = '충청북도' AND region_depth2_name = '보은군';
UPDATE club SET region_depth2_code = '43740' WHERE region_depth1_name = '충청북도' AND region_depth2_name = '영동군';
UPDATE club SET region_depth2_code = '43730' WHERE region_depth1_name = '충청북도' AND region_depth2_name = '옥천군';
UPDATE club SET region_depth2_code = '43770' WHERE region_depth1_name = '충청북도' AND region_depth2_name = '음성군';
UPDATE club SET region_depth2_code = '43150' WHERE region_depth1_name = '충청북도' AND region_depth2_name = '제천시';
UPDATE club SET region_depth2_code = '43745' WHERE region_depth1_name = '충청북도' AND region_depth2_name = '증평군';
UPDATE club SET region_depth2_code = '43750' WHERE region_depth1_name = '충청북도' AND region_depth2_name = '진천군';
UPDATE club SET region_depth2_code = '43110' WHERE region_depth1_name = '충청북도' AND region_depth2_name = '청주시';
UPDATE club SET region_depth2_code = '43130' WHERE region_depth1_name = '충청북도' AND region_depth2_name = '충주시';

UPDATE club
SET region_label = CASE
        WHEN region_scope = 'ONLINE' THEN '온라인'
        WHEN region_scope = 'NATIONWIDE' THEN '전국'
        WHEN region_depth1_name IS NOT NULL AND region_depth2_name IS NOT NULL THEN CONCAT(region_depth1_name, ' ', region_depth2_name)
        WHEN region_depth1_name IS NOT NULL THEN region_depth1_name
        ELSE region_label
    END;

SELECT club_id, name, region_scope, region_depth1_code, region_depth2_code, region_depth1_name, region_depth2_name
FROM club
WHERE region_scope = 'OFFLINE'
  AND region_depth1_code IS NULL;
