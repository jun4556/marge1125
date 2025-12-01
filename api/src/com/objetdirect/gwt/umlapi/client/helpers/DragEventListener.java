package com.objetdirect.gwt.umlapi.client.helpers;

import com.objetdirect.gwt.umlapi.client.engine.Point;

/**
 * OT実装: ドラッグ&ドロップイベント通知用のリスナーインターフェース
 * 
 * DrawerPanelなどの上位コンポーネントがこのインターフェースを実装し、
 * UMLCanvasのドラッグイベントを受け取ることで、Operational Transformation (OT)
 * による競合解決を実現します。
 * 
 * @author KIfU_marge
 * @version 1.0
 * @since 2025-12-01
 */
public interface DragEventListener {
	
	/**
	 * ドラッグ開始イベント通知
	 * マウスボタンが押下され、ドラッグ可能な要素が選択された際に呼ばれます。
	 * 
	 * @param elementId ドラッグされている要素のID (例: "artifact-123")
	 * @param startPosition ドラッグ開始位置
	 */
	void onDragStart(String elementId, Point startPosition);
	
	/**
	 * ドラッグ中の移動イベント通知
	 * マウスが移動し、ドラッグ中の要素位置が変化した際に呼ばれます。
	 * 
	 * @param elementId ドラッグされている要素のID
	 * @param currentPosition 現在のドラッグ位置
	 */
	void onDragMove(String elementId, Point currentPosition);
	
	/**
	 * ドラッグ完了イベント通知
	 * マウスボタンがリリースされ、ドラッグ操作が完了した際に呼ばれます。
	 * この時点で、他のユーザーの同時ドラッグ操作との競合解決が行われます。
	 * 
	 * @param elementId ドラッグされた要素のID
	 * @param finalPosition 最終的なドロップ位置
	 */
	void onDragEnd(String elementId, Point finalPosition);
}
