package com.objetdirect.gwt.umldrawer.server.collaboration;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.objetdirect.gwt.umldrawer.client.beans.EditOperation;

/**
 * サーバー側で編集操作を管理し、Operational Transformation (OT) による
 * 同時編集の競合解決を行うマネージャークラス。
 */
public class OperationManager {
    
    /** エクササイズごとの操作履歴を管理するマップ */
    private final Map<Integer, List<EditOperation>> operationHistory;
    
    /** エクササイズごとのグローバルシーケンス番号カウンター */
    private final Map<Integer, AtomicInteger> sequenceCounters;
    
    /** エクササイズごとの現在のテキスト状態を管理 (elementId:partId -> currentText) */
    private final Map<Integer, Map<String, String>> currentStates;
    
    /** シングルトンインスタンス */
    private static OperationManager instance;
    
    private OperationManager() {
        this.operationHistory = new ConcurrentHashMap<>();
        this.sequenceCounters = new ConcurrentHashMap<>();
        this.currentStates = new ConcurrentHashMap<>();
    }
    
    /**
     * シングルトンインスタンスを取得
     */
    public static synchronized OperationManager getInstance() {
        if (instance == null) {
            instance = new OperationManager();
        }
        return instance;
    }
    
    /**
     * 新しい編集操作を受信し、適切に処理する
     * 
     * @param operation クライアントから送信された編集操作
     * @return サーバーで処理された操作（他のクライアントに配信すべきもの）
     */
    public synchronized EditOperation processOperation(EditOperation operation) {
        int exerciseId = operation.getExerciseId();
        
        // 初期化処理
        initializeExercise(exerciseId);
        
        // グローバルシーケンス番号を割り当て
        int serverSeq = sequenceCounters.get(exerciseId).incrementAndGet();
        operation.setServerSequence(serverSeq);
        
        // 操作履歴から、この操作が基づいているシーケンス以降の操作を取得
        List<EditOperation> history = operationHistory.get(exerciseId);
        List<EditOperation> concurrentOps = getConcurrentOperations(
            history, 
            operation.getBasedOnServerSequence()
        );
        
        // 同時に発生した操作がある場合、トランスフォームを適用
        EditOperation transformedOp = operation;
        if (!concurrentOps.isEmpty()) {
            transformedOp = transformOperation(operation, concurrentOps);
        }
        
        // 現在の状態を更新
        String stateKey = createStateKey(operation.getElementId(), operation.getPartId());
        Map<String, String> states = currentStates.get(exerciseId);
        
        String currentText = states.getOrDefault(stateKey, "");
        String newText = applyPatch(currentText, transformedOp.getPatchText());
        states.put(stateKey, newText);
        
        // トランスフォーム後のテキストを設定
        transformedOp.setAfterText(newText);
        
        // 操作履歴に追加
        history.add(transformedOp);
        
        return transformedOp;
    }
    
    /**
     * エクササイズの初期化
     */
    private void initializeExercise(int exerciseId) {
        operationHistory.putIfAbsent(exerciseId, Collections.synchronizedList(new ArrayList<>()));
        sequenceCounters.putIfAbsent(exerciseId, new AtomicInteger(0));
        currentStates.putIfAbsent(exerciseId, new ConcurrentHashMap<>());
    }
    
    /**
     * 指定したシーケンス番号以降の操作を取得
     */
    private List<EditOperation> getConcurrentOperations(
            List<EditOperation> history, 
            int basedOnSequence) {
        List<EditOperation> concurrent = new ArrayList<>();
        for (EditOperation op : history) {
            if (op.getServerSequence() > basedOnSequence) {
                concurrent.add(op);
            }
        }
        return concurrent;
    }
    
    /**
     * 操作をトランスフォームする
     * 
     * @param newOp 新しく受信した操作
     * @param concurrentOps 同時に発生していた操作のリスト
     * @return トランスフォーム後の操作
     */
    private EditOperation transformOperation(
            EditOperation newOp, 
            List<EditOperation> concurrentOps) {
        
        EditOperation transformed = cloneOperation(newOp);
        
        // 同じ要素・同じ部分に対する操作のみをトランスフォーム対象とする
        for (EditOperation concurrentOp : concurrentOps) {
            if (isSameTarget(newOp, concurrentOp)) {
                // パッチをトランスフォーム
                // 注: 本格的なOTでは、操作の種類に応じて複雑なトランスフォームが必要
                // ここでは簡易版として、パッチを再計算する
                transformed = recomputePatch(transformed, concurrentOp);
            }
        }
        
        return transformed;
    }
    
    /**
     * 同じ編集対象かどうかをチェック
     */
    private boolean isSameTarget(EditOperation op1, EditOperation op2) {
        return op1.getElementId().equals(op2.getElementId()) &&
               op1.getPartId().equals(op2.getPartId());
    }
    
    /**
     * 操作をクローン
     */
    private EditOperation cloneOperation(EditOperation op) {
        EditOperation cloned = new EditOperation();
        cloned.setClientSequence(op.getClientSequence());
        cloned.setUserId(op.getUserId());
        cloned.setSessionId(op.getSessionId());
        cloned.setElementId(op.getElementId());
        cloned.setPartId(op.getPartId());
        cloned.setOperationType(op.getOperationType());
        cloned.setPatchText(op.getPatchText());
        cloned.setBeforeText(op.getBeforeText());
        cloned.setAfterText(op.getAfterText());
        cloned.setTimestamp(op.getTimestamp());
        cloned.setBasedOnServerSequence(op.getBasedOnServerSequence());
        cloned.setExerciseId(op.getExerciseId());
        return cloned;
    }
    
    /**
     * 先行する操作を考慮してパッチを再計算
     */
    private EditOperation recomputePatch(EditOperation newOp, EditOperation priorOp) {
        // 先行操作適用後の状態を基準に、新しいパッチを計算
        String baseText = priorOp.getAfterText();
        String targetText = newOp.getAfterText();
        
        // TODO: diff-match-patchライブラリを使用した実装
        // 実装例はOperationManager_patch_implementation.txtを参照
        
        EditOperation recomputed = cloneOperation(newOp);
        
        // 簡易実装: 先行操作の結果を基準テキストとして、
        // 新しいパッチを再生成する必要がある
        // 現状は元のパッチをそのまま使用（要改善）
        
        return recomputed;
    }
    
    /**
     * パッチを適用してテキストを更新
     * 注: この実装は簡易版。実際にはdiff-match-patchライブラリが必要
     */
    private String applyPatch(String currentText, String patchText) {
        // TODO: サーバー側でdiff-match-patchを使用してパッチを適用
        // 現状は簡易実装として、パッチテキストをそのまま返す
        return currentText;
    }
    
    /**
     * 状態管理用のキーを生成
     */
    private String createStateKey(String elementId, String partId) {
        return elementId + ":" + partId;
    }
    
    /**
     * 特定のエクササイズの操作履歴を取得
     */
    public List<EditOperation> getHistory(int exerciseId) {
        return new ArrayList<>(operationHistory.getOrDefault(
            exerciseId, 
            Collections.emptyList()
        ));
    }
    
    /**
     * 特定の要素の現在のテキストを取得
     */
    public String getCurrentText(int exerciseId, String elementId, String partId) {
        Map<String, String> states = currentStates.get(exerciseId);
        if (states == null) {
            return "";
        }
        String stateKey = createStateKey(elementId, partId);
        return states.getOrDefault(stateKey, "");
    }
    
    /**
     * エクササイズの履歴をクリア(テスト用)
     */
    public void clearHistory(int exerciseId) {
        operationHistory.remove(exerciseId);
        sequenceCounters.remove(exerciseId);
        currentStates.remove(exerciseId);
    }
    
    // ========== MOVE操作の処理 ==========
    
    /** エクササイズごとの要素位置を管理するマップ (elementId -> Position) */
    private final Map<Integer, Map<String, Position>> elementPositions = new ConcurrentHashMap<>();
    
    /** エクササイズごとの進行中のドラッグ操作を追跡 (elementId -> DragInfo) */
    private final Map<Integer, Map<String, DragInfo>> activeDrags = new ConcurrentHashMap<>();
    
    /**
     * 位置情報を保持する内部クラス
     */
    public static class Position {
        public int x;
        public int y;
        public long timestamp;
        public String userId;
        
        public Position(int x, int y, long timestamp, String userId) {
            this.x = x;
            this.y = y;
            this.timestamp = timestamp;
            this.userId = userId;
        }
    }
    
    /**
     * ドラッグ情報を保持する内部クラス
     */
    private static class DragInfo {
        public String userId;
        public long startTime;
        public Position startPosition;
        
        public DragInfo(String userId, long startTime, Position startPosition) {
            this.userId = userId;
            this.startTime = startTime;
            this.startPosition = startPosition;
        }
    }
    
    /**
     * ドラッグ開始を記録
     */
    public synchronized void recordDragStart(int exerciseId, String elementId, 
                                            String userId, int x, int y, long timestamp) {
        activeDrags.putIfAbsent(exerciseId, new ConcurrentHashMap<>());
        Map<String, DragInfo> drags = activeDrags.get(exerciseId);
        
        Position startPos = new Position(x, y, timestamp, userId);
        DragInfo dragInfo = new DragInfo(userId, timestamp, startPos);
        drags.put(elementId, dragInfo);
    }
    
    /**
     * MOVE操作を処理し、競合を検出・解決
     * 
     * @param exerciseId エクササイズID
     * @param elementId 要素ID
     * @param userId ユーザーID
     * @param x 新しいX座標
     * @param y 新しいY座標
     * @param timestamp 操作のタイムスタンプ
     * @return 確定した位置情報(競合解決済み)
     */
    public synchronized Position processMoveOperation(int exerciseId, String elementId, 
                                                      String userId, int x, int y, long timestamp) {
        elementPositions.putIfAbsent(exerciseId, new ConcurrentHashMap<>());
        Map<String, Position> positions = elementPositions.get(exerciseId);
        
        // 現在の位置を取得
        Position currentPos = positions.get(elementId);
        
        if (currentPos == null) {
            // 初めての移動: そのまま記録
            Position newPos = new Position(x, y, timestamp, userId);
            positions.put(elementId, newPos);
            clearDrag(exerciseId, elementId);
            return newPos;
        }
        
        // 競合を検出
        boolean isConcurrent = detectConcurrentMoves(currentPos, timestamp, userId);
        
        if (isConcurrent) {
            // 競合が発生: Last-Write-Winsで解決
            Position resolvedPos = handleConcurrentMoves(currentPos, 
                new Position(x, y, timestamp, userId));
            positions.put(elementId, resolvedPos);
            clearDrag(exerciseId, elementId);
            return resolvedPos;
        } else {
            // 競合なし: そのまま更新
            Position newPos = new Position(x, y, timestamp, userId);
            positions.put(elementId, newPos);
            clearDrag(exerciseId, elementId);
            return newPos;
        }
    }
    
    /**
     * 同時MOVE操作を検出
     * 
     * @param existingPos 既存の位置情報
     * @param newTimestamp 新しい操作のタイムスタンプ
     * @param newUserId 新しい操作のユーザーID
     * @return 同時操作が発生しているか
     */
    private boolean detectConcurrentMoves(Position existingPos, long newTimestamp, String newUserId) {
        // 同じユーザーの連続操作は競合とみなさない
        if (existingPos.userId.equals(newUserId)) {
            return false;
        }
        
        // タイムスタンプの差が一定時間以内(例: 1秒)なら同時操作とみなす
        long timeDiff = Math.abs(newTimestamp - existingPos.timestamp);
        return timeDiff < 1000; // 1秒以内
    }
    
    /**
     * 同時MOVE操作をLast-Write-Wins戦略で解決
     * 
     * @param pos1 既存の位置情報
     * @param pos2 新しい位置情報
     * @return 確定した位置情報
     */
    private Position handleConcurrentMoves(Position pos1, Position pos2) {
        // Last-Write-Wins: タイムスタンプが新しい方を採用
        if (pos2.timestamp > pos1.timestamp) {
            return pos2;
        } else if (pos2.timestamp < pos1.timestamp) {
            return pos1;
        } else {
            // タイムスタンプが同じ場合、ユーザーID辞書順で決定
            return pos1.userId.compareTo(pos2.userId) < 0 ? pos1 : pos2;
        }
    }
    
    /**
     * ドラッグ情報をクリア
     */
    private void clearDrag(int exerciseId, String elementId) {
        Map<String, DragInfo> drags = activeDrags.get(exerciseId);
        if (drags != null) {
            drags.remove(elementId);
        }
    }
    
    /**
     * 特定要素の現在位置を取得
     */
    public Position getCurrentPosition(int exerciseId, String elementId) {
        Map<String, Position> positions = elementPositions.get(exerciseId);
        if (positions == null) {
            return null;
        }
        return positions.get(elementId);
    }
    
    /**
     * 特定要素がドラッグ中かどうかを確認
     */
    public boolean isDragging(int exerciseId, String elementId) {
        Map<String, DragInfo> drags = activeDrags.get(exerciseId);
        if (drags == null) {
            return false;
        }
        return drags.containsKey(elementId);
    }
}
