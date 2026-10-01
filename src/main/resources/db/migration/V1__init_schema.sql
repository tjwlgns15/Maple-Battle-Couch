-- 초기 스키마. 2026-10-01 까지 Hibernate ddl-auto: update 로 만들어진 스키마를 그대로 옮겼다.
-- 기존 DB 는 spring.flyway.baseline-version=1 로 이 파일을 실행하지 않고 적용된 것으로 표시한다.
-- 외래 키 이름(FK...)은 Hibernate 가 만든 이름을 그대로 둬서 새 DB 와 기존 DB 가 같은 이름을 갖게 했다.

CREATE TABLE `ranker_probe` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `character_name` varchar(30) NOT NULL,
  `job_class` varchar(50) NOT NULL,
  `period_no` int NOT NULL,
  `probed_at` datetime(6) NOT NULL,
  `result` enum('NOT_FOUND','NO_RECORD','OTHER_PERIOD_ONLY','SAMPLED') NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ranker_probe` (`job_class`,`character_name`,`period_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `ranker_sample` (
  `replay_id` varchar(64) NOT NULL,
  `character_class` varchar(30) NOT NULL,
  `character_name` varchar(30) NOT NULL,
  `collected_at` datetime(6) NOT NULL,
  `period_no` int NOT NULL,
  PRIMARY KEY (`replay_id`),
  KEY `idx_ranker_sample_class_period` (`character_class`,`period_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `replay` (
  `replay_id` varchar(64) NOT NULL,
  `register_date` date NOT NULL,
  `total_play_time_ms` bigint NOT NULL,
  `total_damage` bigint NOT NULL,
  `total_dps` bigint NOT NULL,
  `end_type` varchar(5) NOT NULL,
  `like_count` int NOT NULL,
  `character_name` varchar(30) NOT NULL,
  `character_class` varchar(30) NOT NULL,
  `character_level` int NOT NULL,
  `fetched_at` datetime(6) NOT NULL,
  `refreshed_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`replay_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `replay_cast` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `base_name` varchar(100) NOT NULL,
  `elapse_ms` bigint NOT NULL,
  `hexa_type` enum('ASCENT','NORMAL','ORIGIN','UNKNOWN') NOT NULL,
  `recorded_order` int NOT NULL,
  `sequence_key` varchar(10) DEFAULT NULL,
  `sequence_name` varchar(50) DEFAULT NULL,
  `skill_name` varchar(100) NOT NULL,
  `replay_id` varchar(64) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_cast_replay_order` (`replay_id`,`recorded_order`),
  CONSTRAINT `FKkef1nr8svv2t66ec0lp7m22gu` FOREIGN KEY (`replay_id`) REFERENCES `replay` (`replay_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `replay_period` (
  `replay_id` varchar(64) NOT NULL,
  `period_no` int NOT NULL,
  PRIMARY KEY (`replay_id`),
  KEY `idx_replay_period_no` (`period_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `replay_raw_data` (
  `replay_id` varchar(64) NOT NULL,
  `character_info_json` json NOT NULL,
  `result_json` json NOT NULL,
  `timeline_json` json NOT NULL,
  PRIMARY KEY (`replay_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `replay_skill_stat` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `base_name` varchar(100) NOT NULL,
  `damage` bigint NOT NULL,
  `damage_percent` decimal(6,2) NOT NULL,
  `dps` bigint NOT NULL,
  `use_count` int NOT NULL,
  `damage_per_use` bigint NOT NULL,
  `attack_count` int NOT NULL,
  `max_damage` bigint NOT NULL,
  `min_damage` bigint NOT NULL,
  `skill_name` varchar(100) NOT NULL,
  `replay_id` varchar(64) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_skill_stat_base_name` (`base_name`),
  KEY `FKbe2m3r78k0poi7u8rmobsoy5j` (`replay_id`),
  CONSTRAINT `FKbe2m3r78k0poi7u8rmobsoy5j` FOREIGN KEY (`replay_id`) REFERENCES `replay` (`replay_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

