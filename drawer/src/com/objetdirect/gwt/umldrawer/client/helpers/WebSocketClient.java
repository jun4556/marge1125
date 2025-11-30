package com.objetdirect.gwt.umldrawer.client.helpers;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONValue;
import com.objetdirect.gwt.umlapi.client.engine.Point;
import com.objetdirect.gwt.umldrawer.client.DrawerPanel;


public class WebSocketClient {

    private JavaScriptObject ws;
    private DrawerPanel drawerPanel;
    private String userId;
    private int exerciseId;

    public WebSocketClient(DrawerPanel panel) {
        this.drawerPanel = panel;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public void setExerciseId(int exerciseId) {
        this.exerciseId = exerciseId;
    }
    
    public String getUserId() {
        return this.userId;
    }
    
    public int getExerciseId() {
        return this.exerciseId;
    }

    public native void connect(String url) /*-{
        var self = this;
        var ws = new WebSocket(url);

        ws.onopen = function() {
            console.log("WebSocket connection opened.");
        };

        ws.onmessage = function(event) {
            // Javaの onMessage メソッドを呼び出す
            self.@com.objetdirect.gwt.umldrawer.client.helpers.WebSocketClient::onMessage(Ljava/lang/String;)(event.data);
        };

        ws.onclose = function() {
            console.log("WebSocket connection closed.");
        };

        ws.onerror = function(error) {
            console.error("WebSocket Error: ", error);
        };

        this.@com.objetdirect.gwt.umldrawer.client.helpers.WebSocketClient::ws = ws;
    }-*/;

    public native void send(String message) /*-{
        if (this.@com.objetdirect.gwt.umldrawer.client.helpers.WebSocketClient::ws &&
            this.@com.objetdirect.gwt.umldrawer.client.helpers.WebSocketClient::ws.readyState === WebSocket.OPEN) {
            this.@com.objetdirect.gwt.umldrawer.client.helpers.WebSocketClient::ws.send(message);
        } else {
            console.error("WebSocket is not connected.");
        }
    }-*/;
    public native void disconnect() /*-{
        if (this.@com.objetdirect.gwt.umldrawer.client.helpers.WebSocketClient::ws) {
            this.@com.objetdirect.gwt.umldrawer.client.helpers.WebSocketClient::ws.close();
            this.@com.objetdirect.gwt.umldrawer.client.helpers.WebSocketClient::ws = null;
        }
    }-*/;
    
    public native boolean isOpen() /*-{
        return this.@com.objetdirect.gwt.umldrawer.client.helpers.WebSocketClient::ws &&
               this.@com.objetdirect.gwt.umldrawer.client.helpers.WebSocketClient::ws.readyState === WebSocket.OPEN;
    }-*/;
 // WebSocketClient.java の onMessage メソッドをこれに置き換える

    public void onMessage(String message) {
        try {
            JSONValue jsonValue = JSONParser.parseStrict(message);
            JSONObject jsonObject = jsonValue.isObject();

            if (jsonObject != null && jsonObject.containsKey("action")) {
                String action = jsonObject.get("action").isString().stringValue();

                if ("sync".equals(action)) {
                    String url = jsonObject.get("url").isString().stringValue();
                    // DrawerPanelからDrawerBaseへの参照を取得し、syncCanvasFromServerを呼び出す！
                    if (drawerPanel != null && drawerPanel.getDrawerBaseInstance()  != null) {
                         drawerPanel.getDrawerBaseInstance() .syncCanvasFromServer(url);
                    }
                }
                else if ("textUpdate".equals(action)) {
                    // "textUpdate"の荷物が届いたら、中身を取り出す
                    String elementId = jsonObject.get("elementId").isString().stringValue();
                    String partId = jsonObject.get("partId").isString().stringValue();
                    String newText = jsonObject.get("newText").isString().stringValue();

                    // DrawerPanelに、テキストを更新する新しい命令を出す！
                    if (drawerPanel != null) {
                        drawerPanel.updateArtifactText(elementId, partId, newText);
                    }
                }
                else if ("applyPatch".equals(action)) {
                    String elementId = jsonObject.get("elementId").isString().stringValue();
                    String partId = jsonObject.get("partId").isString().stringValue();
                    String patchText = jsonObject.get("patch").isString().stringValue();

                    if (drawerPanel != null) {
                        // DrawerPanelに、パッチを適用する新しい命令を出す！
                        drawerPanel.applyPatchToArtifactText(elementId, partId, patchText);
                    }
                }
                else if ("editOperationResponse".equals(action)) {
                    // OT方式の編集操作レスポンス
                    int serverSequence = (int) jsonObject.get("serverSequence").isNumber().doubleValue();
                    String elementId = jsonObject.get("elementId").isString().stringValue();
                    String partId = jsonObject.get("partId").isString().stringValue();
                    String afterText = jsonObject.get("afterText").isString().stringValue();
                    String userId = jsonObject.get("userId").isString().stringValue();
                    
                    // 自分の操作かどうかを判定
                    boolean isOwnOperation = jsonObject.containsKey("isOwnOperation") && 
                                            jsonObject.get("isOwnOperation").isBoolean().booleanValue();
                    
                    if (drawerPanel != null) {
                        // DrawerPanelのOTヘルパーを使用して操作を適用
                        drawerPanel.applyOTOperation(serverSequence, elementId, partId, afterText, userId, isOwnOperation);
                    }
                }
                else if ("move".equals(action)) {
                    // MOVE操作の受信
                    String elementId = jsonObject.get("elementId").isString().stringValue();
                    int x = (int) jsonObject.get("x").isNumber().doubleValue();
                    int y = (int) jsonObject.get("y").isNumber().doubleValue();
                    long timestamp = (long) jsonObject.get("timestamp").isNumber().doubleValue();
                    String userId = jsonObject.containsKey("userId") ? 
                                   jsonObject.get("userId").isString().stringValue() : "unknown";
                    
                    if (drawerPanel != null) {
                        com.objetdirect.gwt.umlapi.client.engine.Point position = 
                            new com.objetdirect.gwt.umlapi.client.engine.Point(x, y);
                        drawerPanel.applyRemoteOperation(userId, elementId, "MOVE", position, null, null, timestamp);
                    }
                }
                else if ("dragStart".equals(action)) {
                    // ドラッグ開始通知の受信(他のユーザーがドラッグを開始した)
                    String elementId = jsonObject.get("elementId").isString().stringValue();
                    int x = (int) jsonObject.get("x").isNumber().doubleValue();
                    int y = (int) jsonObject.get("y").isNumber().doubleValue();
                    String userId = jsonObject.containsKey("userId") ? 
                                   jsonObject.get("userId").isString().stringValue() : "unknown";
                    
                    // TODO: 他のユーザーのドラッグ開始を視覚的に表示
                    // 例: ゴーストアーティファクトを表示
                    System.out.println("User " + userId + " started dragging " + elementId);
                }
            }
        } catch (Exception e) {
            System.err.println("受信したメッセージの解析に失敗: " + message);
        }
    }
}