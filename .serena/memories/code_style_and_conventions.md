# コード規約とスタイルガイド

## 一般規約

### ファイルエンコーディング
- **UTF-8**: すべてのソースファイル
- build.xmlに `encoding="utf-8"` が指定されている

### Java バージョン
- **Java 8** (source="8", target="8")
- Java 8の機能（Lambda、Stream等）が使用可能

## 命名規則

### パッケージ構造
```
com.objetdirect.gwt.umldrawer
├── client/          # クライアント側コード（GWT）
│   ├── beans/       # データモデル
│   ├── helpers/     # ヘルパークラス
│   └── collaboration/  # コラボレーション機能
└── server/          # サーバー側コード
    └── collaboration/  # WebSocketエンドポイント等
```

### クラス名
- **PascalCase**: `EditOperation`, `OperationManager`, `WebSocketClient`
- 機能を明確に表す名前を使用

### メソッド名
- **camelCase**: `processOperation()`, `sendTextChangeWithOT()`, `applyOTOperation()`
- 動詞で始める（get, set, process, send, apply等）

### 変数名
- **camelCase**: `userId`, `exerciseId`, `serverSequence`
- 意味が明確な名前を使用

### 定数
- **UPPER_SNAKE_CASE**: 定数の場合（例: `MAX_RETRY_COUNT`）

## コーディングスタイル

### インデント
- **タブまたは4スペース**: プロジェクト全体で統一
- ネストレベルに応じて適切にインデント

### 括弧
- Javaの標準的なスタイル（K&R style）
```java
public void method() {
    if (condition) {
        // code
    } else {
        // code
    }
}
```

### import文
- 使用するクラスを明示的にimport
- ワイルドカードimport（`.*`）は避ける（例外あり）
- 標準ライブラリ→サードパーティ→プロジェクト内の順

## ドキュメンテーション

### クラスコメント
- 各クラスの目的を明確に記述
```java
/**
 * WebSocketエンドポイントクラス
 * クライアントからの編集操作を受信し、OT処理後に全クライアントへ配信
 */
public class CollaborationWebSocket {
    // ...
}
```

### メソッドコメント
- 複雑なロジックには説明を追加
- パラメータと戻り値を記述（Javadoc形式）
```java
/**
 * 操作をトランスフォームしてサーバーシーケンスを割り当てる
 * @param operation クライアントから受信した操作
 * @return トランスフォーム後の操作
 */
public EditOperation processOperation(EditOperation operation) {
    // ...
}
```

### TODOコメント
- 未実装や改善が必要な箇所に記述
```java
// TODO: diff-match-patchを使用した完全なパッチトランスフォーム実装
```

## GWT固有の規約

### クライアント側コード
- `client/` パッケージ配下
- GWT互換のJavaのみ使用（JREエミュレーションクラス）
- Serializable を実装（RPC用のBeans）

### サーバー側コード
- `server/` パッケージ配下
- 標準Java API使用可能
- WebSocketエンドポイントには `@ServerEndpoint` アノテーション

### 共有クラス（Beans）
- クライアントとサーバーで共有するデータモデル
- `implements Serializable` 必須
- デフォルトコンストラクタ必須
```java
public class EditOperation implements Serializable {
    private String userId;
    private int exerciseId;
    
    public EditOperation() {} // 必須
    
    // getters/setters
}
```

## データベース関連

### SQL命名
- テーブル名: **snake_case** (`operation_log`, `edit_event`)
- カラム名: **snake_case** (`user_id`, `server_sequence`)
- インデックス: `idx_` プレフィックス (`idx_exercise_seq`)

### 文字コード
- **utf8mb4**: テーブル文字セット
- 絵文字対応も可能

## エラーハンドリング

### 例外処理
- 適切なtry-catchブロック
- エラーメッセージはログに記録
```java
try {
    // 処理
} catch (Exception e) {
    logger.error("操作の処理中にエラーが発生", e);
    // エラーハンドリング
}
```

### ログ出力
- `System.out.println()` よりロギングフレームワーク推奨
- エラー、警告、情報、デバッグのレベルを使い分け

## 設計パターン

### 使用されているパターン
- **Singleton**: OperationManagerなど
- **Helper/Utility**: OperationTransformHelper, DiffMatchPatchGwtExtended
- **Beans/POJO**: EditOperation
- **Observer/Callback**: WebSocketMessageHandler

## バージョン管理

### コミットメッセージ
- 変更内容を明確に記述
- 日本語または英語（プロジェクトに統一）
- 例: 「OT方式のOperationManagerクラスを追加」

### ブランチ戦略
- `main`: 安定版
- 機能追加は適宜ブランチを作成

## 注意事項
- **GWTの制限**: クライアント側では標準Java APIの一部が使用不可
- **WebSocket**: Tomcat 8以降が必須
- **データベース**: MySQLまたはMariaDBを使用
