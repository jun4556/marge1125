# プロジェクト概要

## プロジェクト名
KIfU_marge（GWT UML Drawer）

## 目的
リアルタイム共同編集機能を備えたUML図エディタWebアプリケーション。複数ユーザーが同時にクラス図などのUML図を編集できるシステム。

## 主要な特徴
- **リアルタイム共同編集**: Operational Transformation (OT)方式により、複数ユーザーが同時編集しても競合を解決
- **WebSocketベース**: クライアント-サーバー間でWebSocketを使用したリアルタイム通信
- **UML図作成**: クラス図、シーケンス図などのUML図を作成・編集
- **データベース永続化**: MySQL/MariaDBに編集操作とデータを保存

## 解決した課題
従来の「last-write-wins」方式では、2人が同時に編集すると後から送信した変更が破棄される問題があった。OT方式により、全ユーザーの変更を保持し矛盾なく統合できるようになった。

## プロジェクト構造
```
kifu_1124/
├── api/           # バックエンドAPI（GWTサーバーサイド）
├── drawer/        # フロントエンド＋サーバー（GWT UMLDrawer本体）
├── download_libraries.bat    # 依存ライブラリダウンロードスクリプト
├── setup_database.bat        # DBセットアップスクリプト
├── OT_Implementation_Summary.md     # OT実装完了報告
└── OT_Integration_Checklist.md      # 統合チェックリスト
```

### drawer/ ディレクトリ
- `src/com/objetdirect/gwt/umldrawer/` - Javaソースコード
  - `client/` - クライアント側コード（GWT）
  - `server/` - サーバー側コード（WebSocket等）
  - `public/` - 静的リソース（Dojo等）
- `war/` - Webアプリケーションルート（デプロイ用）
- `build.xml` - Antビルドスクリプト
- `GWTUMLDrawer.gwt.xml` - GWT設定ファイル
- `docs/` - ドキュメント

### api/ ディレクトリ
- `src/com/` - APIのJavaソースコード
- `*.sql` - データベーススキーマファイル
- `build.xml` - Antビルドスクリプト
