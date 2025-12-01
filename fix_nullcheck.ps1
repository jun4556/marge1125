$file = 'c:\橋浦研究室\kifu_1124\api\src\com\objetdirect\gwt\umlapi\client\helpers\UMLCanvas.java'
$content = Get-Content $file -Encoding UTF8

# Line 2004 (index 2003) を修正: getLocation()のnullチェック追加
$content[2003] = "`t`t`t`t// OT実装: ドラッグ完了をリスナーに通知"
$content[2004] = "`t`t`t`tif (this.dragEventListener != null && selectedArtifact.getLocation() != null) {"

Set-Content $file $content -Encoding UTF8
Write-Host "修正完了!"
