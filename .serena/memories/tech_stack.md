# 技術スタック

## フロントエンド
- **GWT (Google Web Toolkit) 2.8.2**: JavaからJavaScriptへのトランスパイラ
- **Dojo Toolkit**: JavaScript UIフレームワーク（drawer/src/com/.../public/dojo*, dijit, dojox）
- **Tatami**: GWTとDojoの統合ライブラリ

## バックエンド
- **Java 8**: プログラミング言語（source="8" target="8"）
- **WebSocket (JSR 356)**: リアルタイム通信
  - javax.websocket-api-1.1.jar
- **Apache Ant**: ビルドツール

## ライブラリ
### OT実装用ライブラリ
- **diff-match-patch-1.2.jar**: Googleのテキスト差分・パッチライブラリ
- **javax.websocket-api-1.1.jar**: Java WebSocket API
- **gson-2.8.9.jar**: JSONシリアライゼーション

### その他
- **HikariCP**: データベースコネクションプール（GWTUMLDrawer.gwt.xmlにinherits設定）
- **gwt-servlet.jar**: GWTサーブレット実行環境

## データベース
- **MySQL / MariaDB**: リレーショナルデータベース
  - `operation_log` テーブル: OT操作ログ
  - `exercise` テーブル: 課題管理
  - `answer` テーブル: 学生の解答
  - `comment` テーブル: コメント

## サーバー環境
- **Apache Tomcat 8+**: Webコンテナ（WebSocket対応のため8以降必須）
- **JDK 8**: Java Development Kit

## 開発ツール
- **Apache Ant**: ビルド自動化
- **Git**: バージョン管理
- **Eclipse / IntelliJ IDEA**: 統合開発環境（想定）

## ファイルエンコーディング
- **UTF-8**: プロジェクト全体
