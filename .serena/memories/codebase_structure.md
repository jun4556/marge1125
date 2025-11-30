# コードベース構造

## ディレクトリ構造概要

```
kifu_1124/
├── .serena/                 # Serenaメモリファイル
├── .vscode/                 # VSCode設定
├── api/                     # バックエンドAPI
├── drawer/                  # メインアプリケーション（GWT UML Drawer）
├── download_libraries.bat   # ライブラリダウンロードスクリプト
├── setup_database.bat       # DBセットアップスクリプト
├── OT_Implementation_Summary.md      # OT実装完了報告
└── OT_Integration_Checklist.md       # OT統合チェックリスト
```

## drawer/ ディレクトリ詳細

### 主要ディレクトリ
```
drawer/
├── src/                     # Javaソースコード
│   └── com/objetdirect/gwt/umldrawer/
│       ├── client/          # クライアント側コード（GWT）
│       │   ├── beans/       # データモデル（EditOperation等）
│       │   ├── helpers/     # ヘルパークラス（OT関連含む）
│       │   └── collaboration/  # コラボレーション機能
│       ├── server/          # サーバー側コード
│       │   └── collaboration/  # WebSocketエンドポイント
│       └── public/          # 静的リソース
│           ├── dojo/        # Dojo Toolkitライブラリ
│           ├── dijit/       # Dojo UIコンポーネント
│           └── dojox/       # Dojo拡張機能
├── war/                     # Webアプリケーションルート
│   ├── WEB-INF/
│   │   ├── classes/         # コンパイル済みJavaクラス
│   │   ├── lib/             # JARライブラリ
│   │   └── web.xml          # Web設定（WebSocket等）
│   └── umldrawer/           # GWTコンパイル出力（JavaScript）
├── docs/                    # ドキュメント
│   ├── OT_Implementation_Guide.md
│   ├── DiffMatchPatch_Installation.md
│   └── WebSocket_Deployment.md
├── build.xml                # Antビルドスクリプト
├── GWTUMLDrawer.gwt.xml     # GWT設定ファイル
└── KIfU4.war                # デプロイ用WARファイル
```

### 重要なソースファイル（OT関連）

#### サーバー側
- `server/collaboration/CollaborationWebSocket.java`: WebSocketエンドポイント（@ServerEndpoint）
- `server/collaboration/OperationManager.java`: 操作のトランスフォーム・キュー管理

#### クライアント側
- `client/beans/EditOperation.java`: 操作データ構造
- `client/helpers/OperationTransformHelper.java`: OT操作送受信ヘルパー
- `client/helpers/DiffMatchPatchGwtExtended.java`: diff-match-patchラッパー
- `client/helpers/WebSocketClient.java`: WebSocket通信（OT対応済み）
- `client/collaboration/WebSocketMessageHandler.java`: メッセージハンドラー

## api/ ディレクトリ詳細

```
api/
├── src/                     # APIのJavaソースコード
│   └── com/
│       ├── google/          # Googleライブラリ関連
│       └── objetdirect/     # プロジェクト固有コード
├── war/                     # Webアプリケーションルート
│   └── WEB-INF/
├── doc/                     # Javadoc
├── build/                   # ビルド出力
├── build.xml                # Antビルドスクリプト
├── operation_log.sql        # operation_logテーブル定義
├── kifu2.sql, kifu3.sql     # その他のDB定義
└── *.sql                    # 各種SQLファイル
```

## 主要設定ファイル

### build.xml（drawer/）
- Antビルド設定
- GWT SDK: `C:\gwt-2.8.2-custom`
- Java 8（source="8", target="8"）
- クラスパスに以下を含む:
  - diff-match-patch*.jar
  - javax.websocket-api*.jar
  - gson*.jar

### GWTUMLDrawer.gwt.xml
- GWTモジュール設定
- エントリーポイント、継承モジュール、リソースパス等を定義

### web.xml（war/WEB-INF/）
- サーブレット設定
- WebSocketエンドポイント設定（Tomcat 8+）

## データベーススキーマファイル

### operation_log.sql
```sql
CREATE TABLE operation_log (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    exercise_id INT NOT NULL,
    element_id VARCHAR(255),
    part_id VARCHAR(255),
    operation_type VARCHAR(50) NOT NULL,
    patch_text TEXT,
    before_text TEXT,
    after_text TEXT,
    client_sequence INT,
    server_sequence INT NOT NULL,
    based_on_sequence INT,
    timestamp BIGINT,
    date DATETIME DEFAULT CURRENT_TIMESTAMP,
    -- インデックス
    INDEX idx_exercise_seq (exercise_id, server_sequence),
    INDEX idx_user_exercise (user_id, exercise_id),
    INDEX idx_timestamp (timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### その他のテーブル（Exercise.txt参照）
- `exercise`: 課題情報
- `answer`: 学生の解答
- `comment`: コメント

## 静的リソース

### drawer/src/com/.../public/
- **dojo/**: Dojo Toolkitコアライブラリ
- **dijit/**: Dojo UIウィジェット
- **dojox/**: Dojo拡張機能（charting, gfx等）
- **CSS**: スタイルシート
- **画像**: アイコン、UI素材

## ビルド成果物

### drawer/war/WEB-INF/classes/
- コンパイル済みJavaクラスファイル（.class）
- パッケージ構造を保持

### drawer/war/umldrawer/
- GWTコンパイル出力（JavaScript）
- クライアント側コードのトランスパイル結果

### KIfU4.war
- デプロイ用WARファイル
- war/ディレクトリをZIP圧縮したもの

## 外部依存ライブラリ（drawer/war/WEB-INF/lib/）

### OT実装用
- diff-match-patch-1.2.jar
- javax.websocket-api-1.1.jar
- gson-2.8.9.jar

### GWT関連
- gwt-servlet.jar（${gwt.sdk}からコピー）

## ドキュメント

### drawer/docs/
- **OT_Implementation_Guide.md**: OT実装の総合ガイド
- **DiffMatchPatch_Installation.md**: diff-match-patchライブラリ導入手順
- **WebSocket_Deployment.md**: WebSocket設定・デプロイ手順

### ルートディレクトリ
- **OT_Implementation_Summary.md**: OT実装完了報告
- **OT_Integration_Checklist.md**: 統合チェックリスト

## その他のファイル

### drawer/
- **NextAction.txt**: 次のアクション項目
- **Exercise.txt**: DB演習問題スキーマ
- **DBMemo.txt**: DB関連メモ
- **reflection.txt**: リフレクション関連メモ
- **compile**: コンパイルスクリプト
- **fix**: 修正スクリプト
- **export.bat**: エクスポートスクリプト
- **KIfUマニュアル.docx**: マニュアル
- **MITライセンス.txt**: ライセンス情報

## モジュール依存関係

```
GWTUMLDrawer (drawer)
    ↓ depends on
GWTUMLAPI (api)
    ↓ uses
Tatami (GWT + Dojo統合)
    ↓ uses
Dojo Toolkit (JavaScript UI)
```

## コードベースのポイント

1. **クライアント・サーバー分離**: client/とserver/で明確に分離
2. **GWT制約**: client/配下はGWT互換のJavaのみ
3. **WebSocket対応**: server/collaboration/で実装
4. **OT実装**: 複数ファイルに分散（beans, helpers, collaboration）
5. **静的リソース**: public/配下に配置（Dojo等）
6. **ビルド自動化**: Ant（build.xml）で一元管理
