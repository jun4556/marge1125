# 推奨コマンド集（Windows環境）

## ビルドコマンド

### drawer（メインアプリケーション）のビルド
```cmd
cd drawer
ant clean
ant build
```

### WARファイルの作成
```cmd
cd drawer
ant war
```
※ `KIfU4.war` ファイルが生成される

### Javaソースのコンパイルのみ
```cmd
cd drawer
ant javac
```

### GWTコンパイル（JavaScript生成）
```cmd
cd drawer
ant gwtc
```

## データベースセットアップ

### operation_logテーブルの作成
```cmd
setup_database.bat
```
または
```cmd
mysql -u root -p < api\operation_log.sql
```

### データベース接続（MySQL CLI）
```cmd
mysql -u root -p gwtumldrawer
```

## ライブラリ管理

### 必要なJARファイルのダウンロード
```cmd
download_libraries.bat
```

手動の場合:
- diff-match-patch-1.2.jar
- javax.websocket-api-1.1.jar
- gson-2.8.9.jar

を `drawer\war\WEB-INF\lib\` に配置

## デプロイ

### Tomcatへのデプロイ（WARファイル）
```cmd
copy drawer\KIfU4.war C:\path\to\tomcat\webapps\
```

### Tomcatの起動
```cmd
C:\path\to\tomcat\bin\startup.bat
```

### Tomcatの停止
```cmd
C:\path\to\tomcat\bin\shutdown.bat
```

## Git操作

### 変更の確認
```cmd
git status
```

### 変更のコミット
```cmd
git add .
git commit -m "コミットメッセージ"
```

### プッシュ
```cmd
git push origin main
```

### プル
```cmd
git pull origin main
```

## ファイル操作（Windows CMD）

### ディレクトリ一覧
```cmd
dir
dir /s      :: サブディレクトリも含む
```

### ファイル検索
```cmd
dir /s /b *.java    :: 全Javaファイルを検索
```

### ファイル内容表示
```cmd
type ファイル名
```

### テキスト検索（grep相当）
```cmd
findstr /s /i "検索文字列" *.java
```

### ディレクトリ移動
```cmd
cd drawer
cd ..
cd \
```

### ディレクトリ作成
```cmd
mkdir ディレクトリ名
```

### ファイル削除
```cmd
del ファイル名
rmdir /s /q ディレクトリ名    :: ディレクトリごと削除
```

## デバッグ・ログ確認

### Tomcatログの確認
```cmd
type C:\path\to\tomcat\logs\catalina.out
```

### MySQLログの確認（エラーログ）
```cmd
type "C:\ProgramData\MySQL\MySQL Server 8.0\Data\*.err"
```

## クリーンアップ

### ビルド成果物の削除
```cmd
cd drawer
ant clean
```

### 一時ファイルの削除
```cmd
del /s /q drawer\war\WEB-INF\classes\*
del /s /q drawer\war\umldrawer\*
```

## 開発ホストモード（GWT Development Mode）

### OOPHMモードで起動
```cmd
cd drawer
ant oophm
```

### 通常のホストモード
```cmd
cd drawer
ant hosted
```

## テスト実行

※ 現在テストフレームワークは未設定
（JUnitテスト追加時はbuild.xmlにtargetを追加）
