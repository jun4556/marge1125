# KIfU_marge ドラッグ競合解決実装ガイド

## 概要
このドキュメントは、KIfU_marge UMLエディターにOperational Transformation (OT)方式のドラッグ競合解決機能を実装した内容をまとめたものです。

## 実装完了日
2025年1月

## 解決した問題
### 1. Last-Write-Wins問題
- **問題**: 2人のユーザーが同時にクラスを編集すると、最初の変更のみがDB保存され、2番目の変更は破棄される
- **解決**: ドラッグ操作中はリモート更新をバッファリングし、ドラッグ完了後にタイムスタンプベースのLast-Write-Wins戦略で競合を解決

### 2. ドラッグ中のUI破壊
- **問題**: ドラッグ中にリモート更新を受信すると、UIが強制的に更新されドラッグ状態が破壊される
- **解決**: ドラッグ中の要素を追跡し、その要素へのリモート更新は即座に適用せず、バッファに保存

## 実装したファイル一覧

### クライアント側 (drawer/)
1. **DrawerPanel.java** (`drawer/src/com/objetdirect/gwt/umldrawer/client/DrawerPanel.java`)
   - DragState内部クラス: ドラッグ状態管理
   - RemoteOperation内部クラス: リモート操作バッファ
   - draggingElementsマップ: 現在ドラッグ中の要素追跡
   - pendingRemoteOpsマップ: バッファされたリモート操作
   - notifyDragStart/Move/End(): ドラッグイベント通知
   - applyRemoteOperation(): リモート操作処理
   - onDragCompleted(): 競合検出と解決
   - sendDragStart/MoveOperation(): サーバーへの通知

2. **WebSocketClient.java** (`drawer/src/com/objetdirect/gwt/umldrawer/client/helpers/WebSocketClient.java`)
   - userIdフィールド追加
   - exerciseIdフィールド追加
   - "move"アクション処理
   - "dragStart"アクション処理
   - Point型インポート

3. **DrawerBase.java** (`drawer/src/com/objetdirect/gwt/umldrawer/client/DrawerBase.java`)
   - WebSocket接続時にuserIdとexerciseIdを設定

### サーバー側 (drawer/src/com/objetdirect/gwt/umldrawer/server/)
4. **OperationManager.java** (`drawer/src/com/objetdirect/gwt/umldrawer/server/collaboration/OperationManager.java`)
   - Position内部クラス: 位置情報管理
   - DragInfo内部クラス: ドラッグ情報管理
   - elementPositionsマップ: 要素位置追跡
   - activeDragsマップ: アクティブなドラッグ追跡
   - recordDragStart(): ドラッグ開始記録
   - processMoveOperation(): MOVE操作処理と競合解決
   - detectConcurrentMoves(): 同時MOVE操作検出
   - handleConcurrentMoves(): Last-Write-Wins戦略実装

5. **CollaborationWebSocket.java** (`drawer/src/com/objetdirect/gwt/umldrawer/server/collaboration/CollaborationWebSocket.java`)
   - handleMoveOperation(): MOVE操作処理
   - handleDragStart(): ドラッグ開始処理
   - broadcastMoveOperation(): MOVE操作配信
   - broadcastDragStart(): ドラッグ開始配信

## データフロー

### ドラッグ開始 (T0)
```
クライアントA: マウスダウン
  → DrawerPanel.notifyDragStart()
  → sendDragStart() → WebSocket送信
  → サーバー: CollaborationWebSocket.handleDragStart()
  → OperationManager.recordDragStart()
  → broadcastDragStart() → 他のクライアントに通知
```

### ドラッグ中にリモート更新受信 (T3)
```
クライアントB: MOVE操作送信
  → サーバー: OperationManager.processMoveOperation()
  → 全クライアントに配信
クライアントA: WebSocketClient.onMessage("move")
  → DrawerPanel.applyRemoteOperation()
  → draggingElements.containsKey(elementId) == true
  → pendingRemoteOps.add() (バッファに保存)
  → showGhostArtifact() (半透明プレビュー表示)
```

### ドラッグ完了と競合解決 (T6)
```
クライアントA: マウスアップ
  → DrawerPanel.notifyDragEnd()
  → onDragCompleted()
    → pendingRemoteOpsから同じelementIdの操作を検索
    → タイムスタンプ比較
    → 新しい方の位置を採用 (Last-Write-Wins)
    → sendMoveOperation() または applyRemoteMoveOperation()
  → draggingElements.remove()
  → pendingRemoteOps.remove()
```

## WebSocketメッセージフォーマット

### ドラッグ開始
```json
{
  "action": "dragStart",
  "elementId": "element-123",
  "userId": "user1",
  "exerciseId": 1,
  "x": 100,
  "y": 200
}
```

### MOVE操作
```json
{
  "action": "move",
  "elementId": "element-123",
  "userId": "user1",
  "exerciseId": 1,
  "x": 150,
  "y": 250,
  "timestamp": 1704067200000
}
```

## 競合解決戦略: Last-Write-Wins

1. **タイムスタンプ比較**
   - 各MOVE操作にクライアント側のタイムスタンプを付与
   - サーバー側でタイムスタンプを比較
   - より新しいタイムスタンプの操作を採用

2. **同一タイムスタンプの場合**
   - ユーザーIDの辞書順で決定
   - 決定的な結果を保証

3. **バッファリング戦略**
   - ドラッグ中: リモート更新をバッファに保存
   - ドラッグ完了後: バッファされた操作を処理
   - 視覚的フィードバック: ゴーストアーティファクト表示

## Eclipse環境でのビルド手順

1. **プロジェクトのインポート**
   ```
   File → Import → Existing Projects into Workspace
   → Browse → kifu_1124/drawer を選択
   ```

2. **必要なJARライブラリの確認**
   - drawer/war/WEB-INF/lib/に以下が存在することを確認:
     - diff-match-patch-1.2.jar
     - javax.websocket-api-1.1.jar
     - gson-2.8.9.jar

3. **ビルド設定**
   - Project → Properties → Java Build Path
   - Libraries タブで上記JARが追加されていることを確認

4. **GWTコンパイル**
   - Eclipseメニュー: Google → GWT Compile
   - Project: KIfU4
   - Entry point module: GWTUMLDrawer
   - コンパイル開始

5. **WARファイル生成**
   - Antビュー (Window → Show View → Ant) でbuild.xmlを開く
   - "war"タ� ゲットを実行
   - drawer/war/KIfU4.war が生成される

## デプロイ手順

1. **Tomcat 8+の準備**
   - WebSocket (JSR 356) サポートのためTomcat 8以上が必要

2. **データベースセットアップ**
   ```cmd
   mysql -u root -p < api\operation_log.sql
   ```

3. **WARファイルデプロイ**
   ```
   KIfU4.warをTomcatのwebappsディレクトリにコピー
   Tomcatを再起動
   ```

4. **接続URLの確認**
   - DrawerBase.java 915行目のWebSocket URLを環境に合わせて変更:
   ```java
   String webSocketURL = "ws://192.168.18.123:8080/KIfU4/diagram/" + exerciseId;
   ```

## テスト手順

### 2ブラウザでの同時編集テスト

1. **環境準備**
   - ブラウザA (Chrome): http://localhost:8080/KIfU4/
   - ブラウザB (Firefox): http://localhost:8080/KIfU4/
   - 両方で同じエクササイズIDを開く

2. **シナリオ1: 異なるクラスの同時移動**
   - ブラウザA: Class1をドラッグ開始 (T0)
   - ブラウザB: Class2を移動完了 (T3)
   - ブラウザA: Class1をドロップ (T6)
   - **期待結果**: 両方の移動が独立して成功

3. **シナリオ2: 同じクラスの同時移動 (競合)**
   - ブラウザA: Class1をドラッグ開始 (T0)
   - ブラウザB: Class1を位置P2に移動 (T3)
   - ブラウザA: 半透明のゴーストアーティファクトが表示される
   - ブラウザA: Class1を位置P1にドロップ (T6)
   - **期待結果**: タイムスタンプが新しい方の位置が採用される

4. **シナリオ3: ドラッグ中のテキスト編集**
   - ブラウザA: Class1をドラッグ開始 (T0)
   - ブラウザB: Class1の名前を編集 (T3)
   - ブラウザA: Class1をドロップ (T6)
   - **期待結果**: 移動とテキスト編集の両方が反映される

### デバッグログ確認

1. **クライアント側ログ (ブラウザコンソール)**
   ```
   F12 → Console
   "WebSocket connection opened."
   "ドラッグ開始を送信: elementId=..."
   "ゴーストアーティファクト表示: elementId=..."
   ```

2. **サーバー側ログ (Tomcat catalina.out)**
   ```
   INFO: WebSocket接続が確立されました
   INFO: ドラッグ開始を処理しました. element: element-123, user: user1
   INFO: MOVE操作を処理しました. element: element-123, pos: (150,250)
   ```

## 既知の制限事項と今後の改善点

### 制限事項
1. **ゴーストアーティファクト未実装**
   - showGhostArtifact()メソッドは現在ログ出力のみ
   - GWT UIでの半透明図形描画が必要

2. **UMLCanvasとの連携未実装**
   - notifyDragStart/Move/Endの呼び出しは手動で追加が必要
   - UMLCanvasまたはUMLArtifactのマウスイベントハンドラーに統合が必要

3. **テキスト編集のOT処理簡易版**
   - diff-match-patchライブラリの完全統合が未完了
   - 現在は基本的なパッチ適用のみ

### 改善点
1. **ドラッグイベントの自動フック**
   - UMLCanvas.javaのマウスイベントハンドラーを修正
   - ドラッグ検出時に自動的にDrawerPanel.notifyDrag*()を呼び出す

2. **ゴーストアーティファクトの実装**
   - SimplePanel + CSS opacity/transformで半透明プレビュー
   - ユーザーIDラベル表示
   - 数秒後の自動削除

3. **サーバー側diff-match-patch統合**
   - OperationManager.applyPatch()の完全実装
   - パッチの妥当性検証

4. **エラーハンドリング強化**
   - WebSocket切断時の再接続ロジック
   - 操作失敗時のリトライ機構

## トラブルシューティング

### 問題: WebSocket接続エラー
**原因**: Tomcatバージョンが古い、または設定不足
**解決**: Tomcat 8以上を使用し、WebSocket対応を確認

### 問題: ドラッグ操作が送信されない
**原因**: UMLCanvasのドラッグイベントとの連携未実装
**解決**: UMLCanvas.javaのマウスイベントハンドラーにnotifyDrag*()呼び出しを追加

### 問題: 競合解決が動作しない
**原因**: userIdまたはexerciseIdが未設定
**解決**: DrawerBase.javaでWebSocket接続時に確実に設定されているか確認

## 関連ドキュメント

- `OT_Implementation_Summary.md` - OT実装の詳細技術仕様
- `OT_Integration_Checklist.md` - 統合チェックリスト
- `docs/OT_Implementation_Guide.md` - 実装ガイド
- `docs/WebSocket_Deployment.md` - WebSocketデプロイメントガイド
- `docs/DiffMatchPatch_Installation.md` - diff-match-patchインストールガイド

## 連絡先
実装に関する質問や問題報告は、プロジェクトリポジトリのIssueトラッカーまで。
