# OT（Operational Transformation）実装について

## OT実装の背景

### 解決した問題
**従来の課題**: 2人が同時にクラス図を編集すると、最初に変更を送信した方のみがDBに記録され、2番目のユーザーの変更は破棄される（last-write-wins問題）

**OT方式による解決**: 各操作にシーケンス番号を付与し、サーバー側で操作をトランスフォーム（変換）することで、全ユーザーの変更を保持し矛盾なく統合

## アーキテクチャ

### データフロー
```
[クライアント1] --editOperation--> [CollaborationWebSocket]
                                           |
                                           v
                                    [OperationManager]
                                    - processOperation()
                                    - transformOperation()
                                    - serverSequence割り当て
                                           |
                                           v
                                    [operation_log] (DB)
                                           |
                                           v
[クライアント1] <--editOperationResponse-- [broadcast]
[クライアント2] <--editOperationResponse--
```

### 処理フロー
1. クライアントがテキストを変更
2. `OperationTransformHelper.sendTextChangeWithOT()` でサーバーに送信
3. `CollaborationWebSocket.onMessage()` で受信
4. `OperationManager.processOperation()` でトランスフォーム・serverSequence付与
5. `operation_log` テーブルに保存
6. 全クライアントに `editOperationResponse` を配信
7. クライアント側で `DrawerPanel.applyOTOperation()` で適用

## 主要コンポーネント

### サーバー側

#### CollaborationWebSocket.java
- **役割**: WebSocketエンドポイント
- **場所**: `drawer/src/com/objetdirect/gwt/umldrawer/server/collaboration/`
- **アノテーション**: `@ServerEndpoint("/collaboration")`
- **主要メソッド**:
  - `onMessage()`: クライアントからの操作を受信
  - `broadcastOperation()`: 全クライアントに操作を配信

#### OperationManager.java
- **役割**: 操作のキュー管理・トランスフォーム処理
- **場所**: `drawer/src/com/objetdirect/gwt/umldrawer/server/collaboration/`
- **主要メソッド**:
  - `processOperation()`: 操作を処理してserverSequenceを割り当て
  - `transformOperation()`: 操作をトランスフォーム
  - `recomputePatch()`: パッチを再計算（TODO: diff-match-patch完全実装）

### クライアント側

#### EditOperation.java
- **役割**: 操作データ構造（Bean）
- **場所**: `drawer/src/com/objetdirect/gwt/umldrawer/client/beans/`
- **実装**: `implements Serializable`
- **フィールド**:
  - `userId`: ユーザーID
  - `exerciseId`: 課題ID
  - `elementId`: 要素ID
  - `partId`: パーツID
  - `operationType`: 操作タイプ
  - `patchText`: パッチテキスト
  - `beforeText`, `afterText`: 変更前後のテキスト
  - `clientSequence`: クライアント側シーケンス番号
  - `serverSequence`: サーバー側シーケンス番号
  - `basedOnSequence`: 基準となるシーケンス番号
  - `timestamp`: タイムスタンプ

#### OperationTransformHelper.java
- **役割**: OT操作送受信ヘルパー
- **場所**: `drawer/src/com/objetdirect/gwt/umldrawer/client/helpers/`
- **主要メソッド**:
  - `sendTextChangeWithOT()`: テキスト変更をOT方式で送信
  - `handleEditOperationResponse()`: サーバーからの応答を処理

#### DiffMatchPatchGwtExtended.java
- **役割**: diff-match-patchライブラリのGWTラッパー
- **場所**: `drawer/src/com/objetdirect/gwt/umldrawer/client/helpers/`
- **機能**: パッチの生成と適用

#### WebSocketMessageHandler.java
- **役割**: WebSocketメッセージハンドラー
- **場所**: `drawer/src/com/objetdirect/gwt/umldrawer/client/collaboration/`
- **機能**: editOperationResponseメッセージの処理

### 既存ファイルへの追加

#### WebSocketClient.java（修正済み）
- **追加内容**:
  - `editOperationResponse` ハンドラー追加
  - `isOpen()` メソッド追加

#### DrawerPanel.java（修正済み）
- **追加内容**:
  - `otHelper` フィールド追加
  - import文追加
  - `applyOTOperation()` メソッド追加（予定）
  - `initializeOTHelper()` メソッド追加（予定）

## データベース

### operation_log テーブル
- **定義**: `api/operation_log.sql`
- **役割**: 全ての編集操作を記録（リプレイ・監査用）
- **主要カラム**:
  - `id`: 主キー
  - `user_id`, `exercise_id`: ユーザー・課題識別
  - `operation_type`: 操作タイプ
  - `patch_text`: パッチテキスト
  - `client_sequence`, `server_sequence`: シーケンス番号
  - `timestamp`: タイムスタンプ
- **インデックス**:
  - `idx_exercise_seq`: (exercise_id, server_sequence)
  - `idx_user_exercise`: (user_id, exercise_id)
  - `idx_timestamp`: (timestamp)

## 必要なライブラリ

### diff-match-patch-1.2.jar
- **提供**: Google
- **用途**: テキスト差分計算とパッチ生成
- **配置**: `drawer/war/WEB-INF/lib/`

### javax.websocket-api-1.1.jar
- **提供**: Oracle (JSR 356)
- **用途**: WebSocket API
- **配置**: `drawer/war/WEB-INF/lib/`

### gson-2.8.9.jar
- **提供**: Google
- **用途**: JSONシリアライゼーション
- **配置**: `drawer/war/WEB-INF/lib/`

## 現在の実装状態

### 完了（✅）
- EditOperation データ構造作成
- OperationManager サーバー側クラス作成
- CollaborationWebSocket エンドポイント作成
- DiffMatchPatchGwtExtended ヘルパークラス作成
- OperationTransformHelper クライアント側ヘルパー作成
- WebSocketMessageHandler 作成
- operation_log テーブルSQL作成
- build.xmlにライブラリパス追加
- WebSocketClient.javaにeditOperationResponseハンドラー追加
- DrawerPanel.javaにotHelperフィールド追加

### 手動統合が必要（⏳）
- 必要なJARファイルのダウンロード・配置
- operation_logテーブルの作成
- DrawerPanel.javaへの統合メソッド追加
- ビルド・デプロイ・テスト

### 今後の改善点（TODO）
- `OperationManager.recomputePatch()` の完全実装（diff-match-patch使用）
- `DrawerBase.getWebSocketClient()` メソッドの実装
- 初期化処理の統合（`initializeOTHelper(userId, exerciseId)` 呼び出し）
- エラーハンドリング強化（WebSocket切断時の再接続等）
- テストケース作成（JUnit）

## 参考ドキュメント

### プロジェクト内
- `OT_Implementation_Summary.md`: 実装完了報告
- `OT_Integration_Checklist.md`: 統合チェックリスト
- `drawer/docs/OT_Implementation_Guide.md`: 総合実装ガイド
- `drawer/docs/DiffMatchPatch_Installation.md`: ライブラリ導入手順
- `drawer/docs/WebSocket_Deployment.md`: WebSocketデプロイ手順

### 外部資料
- [Google diff-match-patch](https://github.com/google/diff-match-patch)
- [Operational Transformation (Wikipedia)](https://en.wikipedia.org/wiki/Operational_transformation)
- [Java WebSocket API (JSR 356)](https://www.oracle.com/technical-resources/articles/java/jsr356.html)
- [Apache Tomcat WebSocket](https://tomcat.apache.org/tomcat-8.5-doc/web-socket-howto.html)

## 重要な注意点

1. **Tomcat 8+必須**: WebSocket対応のため
2. **データベース**: operation_logテーブル作成が必須
3. **ライブラリ配置**: 3つのJARファイルを正しく配置
4. **GWT制約**: client/配下では標準Java APIの一部が使用不可
5. **シーケンス番号**: サーバー側で一元管理（競合を防ぐため）
