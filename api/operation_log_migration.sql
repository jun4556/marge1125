-- ================================================================
-- Operation Log Schema Extension for Move Operations
-- ================================================================
-- This migration adds support for storing move operation details
-- including old position, new position, and delta values.
-- 
-- Author: GitHub Copilot
-- Date: 2025-01-XX
-- Purpose: Enable complete move operation tracking for collaborative editing
-- ================================================================

-- 移動操作用フィールドを追加
-- Add columns for move operation tracking
ALTER TABLE operation_log 
ADD COLUMN old_x INT DEFAULT NULL COMMENT '移動前のX座標 (Move operation: previous X coordinate)',
ADD COLUMN old_y INT DEFAULT NULL COMMENT '移動前のY座標 (Move operation: previous Y coordinate)',
ADD COLUMN new_x INT DEFAULT NULL COMMENT '移動後のX座標 (Move operation: new X coordinate)',
ADD COLUMN new_y INT DEFAULT NULL COMMENT '移動後のY座標 (Move operation: new Y coordinate)',
ADD COLUMN delta_x INT DEFAULT NULL COMMENT 'X方向の移動距離 (Move operation: X delta)',
ADD COLUMN delta_y INT DEFAULT NULL COMMENT 'Y方向の移動距離 (Move operation: Y delta)';

-- インデックス追加 (移動操作検索の高速化)
-- Add index for efficient move operation queries
CREATE INDEX idx_move_ops ON operation_log(operation_type, element_id, timestamp);

-- インデックス追加 (演習ID + 要素ID + タイムスタンプでの検索を高速化)
-- Add composite index for exercise-specific element queries
CREATE INDEX idx_exercise_element ON operation_log(exercise_id, element_id, timestamp);

-- 確認用クエリ (マイグレーション後に実行)
-- Verification query (run after migration)
-- SELECT 
--     COLUMN_NAME, 
--     DATA_TYPE, 
--     IS_NULLABLE, 
--     COLUMN_DEFAULT,
--     COLUMN_COMMENT
-- FROM INFORMATION_SCHEMA.COLUMNS 
-- WHERE TABLE_NAME = 'operation_log' 
-- AND COLUMN_NAME IN ('old_x', 'old_y', 'new_x', 'new_y', 'delta_x', 'delta_y')
-- ORDER BY ORDINAL_POSITION;

-- インデックス確認用クエリ
-- Index verification query
-- SHOW INDEX FROM operation_log WHERE Key_name IN ('idx_move_ops', 'idx_exercise_element');

-- ================================================================
-- Migration Complete
-- ================================================================
-- Next steps:
-- 1. Verify columns added: SELECT * FROM operation_log LIMIT 1;
-- 2. Update CollaborationWebSocket.java to save move operations
-- 3. Test with concurrent move operations
-- ================================================================
