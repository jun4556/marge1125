package com.objetdirect.gwt.umldrawer.server.collaboration;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.objetdirect.gwt.umldrawer.client.beans.EditOperation;
import com.objetdirect.gwt.umldrawer.server.dao.Dao;

/**
 * WebSocketエンドポイント
 * クライアントからの編集操作を受信し、OT方式で処理して他のクライアントに配信
 */
@ServerEndpoint("/collaboration")
public class CollaborationWebSocket {
    
    private static final Logger logger = Logger.getLogger(CollaborationWebSocket.class.getName());
    private static final Set<Session> sessions = Collections.synchronizedSet(new HashSet<>());
    private static final Gson gson = new Gson();
    private static final OperationManager operationManager = OperationManager.getInstance();
    
    @OnOpen
    public void onOpen(Session session) {
        sessions.add(session);
        logger.info("WebSocket接続が確立されました。セッションID: " + session.getId());
    }
    
    @OnClose
    public void onClose(Session session) {
        sessions.remove(session);
        logger.info("WebSocket接続が切断されました。セッションID: " + session.getId());
    }
    
    @OnError
    public void onError(Session session, Throwable throwable) {
        logger.severe("WebSocketエラー: " + throwable.getMessage());
        throwable.printStackTrace();
    }
    
    @OnMessage
    public void onMessage(String message, Session session) {
        try {
            JsonParser parser = new JsonParser();
            JsonObject json = parser.parse(message).getAsJsonObject();
            
            String action = json.get("action").getAsString();
            
            if ("editOperation".equals(action)) {
                handleEditOperation(json, session);
            }
            // 他のアクションもここで処理可能
            
        } catch (Exception e) {
            logger.severe("メッセージ処理エラー: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 編集操作を処理
     */
    private void handleEditOperation(JsonObject json, Session senderSession) {
        try {
            // JSONからEditOperationを構築
            EditOperation operation = parseEditOperation(json);
            
            // OperationManagerで処理（トランスフォーム含む）
            EditOperation processedOp = operationManager.processOperation(operation);
            
            // DBに保存
            saveOperationToDatabase(processedOp);
            
            // 全クライアントに配信
            broadcastOperation(processedOp, senderSession);
            
        } catch (Exception e) {
            logger.severe("編集操作の処理エラー: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * JSONからEditOperationを構築
     */
    private EditOperation parseEditOperation(JsonObject json) {
        EditOperation op = new EditOperation();
        
        if (json.has("clientSequence")) {
            op.setClientSequence(json.get("clientSequence").getAsInt());
        }
        if (json.has("basedOnServerSequence")) {
            op.setBasedOnServerSequence(json.get("basedOnServerSequence").getAsInt());
        }
        if (json.has("userId")) {
            op.setUserId(json.get("userId").getAsString());
        }
        if (json.has("sessionId")) {
            op.setSessionId(json.get("sessionId").getAsString());
        }
        if (json.has("elementId")) {
            op.setElementId(json.get("elementId").getAsString());
        }
        if (json.has("partId")) {
            op.setPartId(json.get("partId").getAsString());
        }
        if (json.has("operationType")) {
            op.setOperationType(json.get("operationType").getAsString());
        }
        if (json.has("patchText")) {
            op.setPatchText(json.get("patchText").getAsString());
        }
        if (json.has("beforeText")) {
            op.setBeforeText(json.get("beforeText").getAsString());
        }
        if (json.has("afterText")) {
            op.setAfterText(json.get("afterText").getAsString());
        }
        if (json.has("exerciseId")) {
            op.setExerciseId(json.get("exerciseId").getAsInt());
        }
        if (json.has("timestamp")) {
            op.setTimestamp(json.get("timestamp").getAsLong());
        }
        
        return op;
    }
    
    /**
     * 処理された操作をDBに保存
     */
    private void saveOperationToDatabase(EditOperation operation) {
        try {
            Dao dao = new Dao();
            
            // operation_logテーブルに保存
            String sql = "INSERT INTO operation_log " +
                        "(user_id, exercise_id, element_id, part_id, " +
                        "operation_type, patch_text, before_text, after_text, " +
                        "client_sequence, server_sequence, based_on_sequence, " +
                        "timestamp, date) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())";
            
            dao.execUpdate(sql, 
                operation.getUserId(),
                operation.getExerciseId(),
                operation.getElementId(),
                operation.getPartId(),
                operation.getOperationType(),
                operation.getPatchText(),
                operation.getBeforeText(),
                operation.getAfterText(),
                operation.getClientSequence(),
                operation.getServerSequence(),
                operation.getBasedOnServerSequence(),
                operation.getTimestamp()
            );
            
            logger.info("操作をDBに保存しました。ServerSeq: " + operation.getServerSequence());
            
        } catch (Exception e) {
            logger.severe("DB保存エラー: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 全クライアントに操作を配信
     */
    private void broadcastOperation(EditOperation operation, Session senderSession) {
        JsonObject response = new JsonObject();
        response.addProperty("action", "editOperationResponse");
        response.addProperty("serverSequence", operation.getServerSequence());
        response.addProperty("elementId", operation.getElementId());
        response.addProperty("partId", operation.getPartId());
        response.addProperty("afterText", operation.getAfterText());
        response.addProperty("userId", operation.getUserId());
        response.addProperty("patchText", operation.getPatchText());
        
        String jsonString = gson.toJson(response);
        
        synchronized (sessions) {
            for (Session session : sessions) {
                try {
                    // 送信者には「自分の操作」フラグを付与
                    if (session.equals(senderSession)) {
                        JsonObject selfResponse = response.deepCopy();
                        selfResponse.addProperty("isOwnOperation", true);
                        session.getBasicRemote().sendText(gson.toJson(selfResponse));
                    } else {
                        session.getBasicRemote().sendText(jsonString);
                    }
                } catch (IOException e) {
                    logger.warning("メッセージ送信失敗: " + e.getMessage());
                }
            }
        }
        
        logger.info("操作を全クライアントに配信しました。ServerSeq: " + operation.getServerSequence());
    }
}
