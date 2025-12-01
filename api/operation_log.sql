-- Operational Transformation用の操作ログテーブル
-- 全ての編集操作を記録し、リプレイや監査に使用

CREATE TABLE IF NOT EXISTS operation_log (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    exercise_id INT NOT NULL,
    element_id VARCHAR(255),
    part_id VARCHAR(255),
    operation_type VARCHAR(50) NOT NULL,
    patch_text TEXT,
    before_text TEXT,
    after_text TEXT,
    old_x INT DEFAULT NULL COMMENT '移動前のX座標 (Move operation)',
    old_y INT DEFAULT NULL COMMENT '移動前のY座標 (Move operation)',
    new_x INT DEFAULT NULL COMMENT '移動後のX座標 (Move operation)',
    new_y INT DEFAULT NULL COMMENT '移動後のY座標 (Move operation)',
    delta_x INT DEFAULT NULL COMMENT 'X方向の移動距離 (Move operation)',
    delta_y INT DEFAULT NULL COMMENT 'Y方向の移動距離 (Move operation)',
    client_sequence INT,
    server_sequence INT NOT NULL,
    based_on_sequence INT,
    timestamp BIGINT,
    date DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_exercise_seq (exercise_id, server_sequence),
    INDEX idx_user_exercise (user_id, exercise_id),
    INDEX idx_timestamp (timestamp),
    INDEX idx_move_ops (operation_type, element_id, timestamp),
    INDEX idx_exercise_element (exercise_id, element_id, timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- exerciseテーブルへの外部キー制約（既存のexerciseテーブルがある場合）
-- ALTER TABLE operation_log ADD CONSTRAINT fk_exercise FOREIGN KEY (exercise_id) REFERENCES exercise(id) ON DELETE CASCADE;
