package com.objetdirect.gwt.umldrawer.client;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.logging.Logger;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.dom.client.MouseDownEvent;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.dom.client.MouseMoveEvent;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseUpEvent;
import com.google.gwt.event.dom.client.MouseUpHandler;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.objetdirect.gwt.umlapi.client.artifacts.ClassArtifact;
import com.objetdirect.gwt.umlapi.client.artifacts.LifeLineArtifact;
import com.objetdirect.gwt.umlapi.client.artifacts.ObjectArtifact;
import com.objetdirect.gwt.umlapi.client.artifacts.UMLArtifact;
import com.objetdirect.gwt.umlapi.client.engine.Direction;
import com.objetdirect.gwt.umlapi.client.engine.GeometryManager;
import com.objetdirect.gwt.umlapi.client.engine.Point;
import com.objetdirect.gwt.umlapi.client.engine.Scheduler;
import com.objetdirect.gwt.umlapi.client.gfx.GfxManager;
import com.objetdirect.gwt.umlapi.client.helpers.GWTUMLDrawerHelper;
import com.objetdirect.gwt.umlapi.client.helpers.Mouse;
import com.objetdirect.gwt.umlapi.client.helpers.OptionsManager;
import com.objetdirect.gwt.umlapi.client.helpers.Session;
import com.objetdirect.gwt.umlapi.client.helpers.ThemeManager;
import com.objetdirect.gwt.umlapi.client.helpers.ThemeManager.Theme;
import com.objetdirect.gwt.umlapi.client.helpers.UMLCanvas;
import com.objetdirect.gwt.umlapi.client.umlcomponents.UMLDiagram;
import com.objetdirect.gwt.umlapi.client.umlcomponents.UMLDiagram.Type;
import com.objetdirect.gwt.umldrawer.client.helpers.DiffMatchPatchGwt;
import com.objetdirect.gwt.umldrawer.client.helpers.OperationTransformHelper;
import com.objetdirect.gwt.umldrawer.client.helpers.WebSocketClient;


public class DrawerPanel extends AbsolutePanel {

	private SimplePanel bottomLeftCornerShadow;
	private SimplePanel bottomRightCornerShadow;
	private SimplePanel bottomShadow;
	private UMLCanvas uMLCanvas; // finalを外す！
	private int height;
	private SimplePanel rightShadow;
	private SimplePanel topRightCornerShadow;
	private int width;
	
	// --- ここからが改造箇所だ！ ---
	private DrawerBase drawerBase; // drawerBaseをしまっておく宝箱
	// --- 改造箇所ここまで ---
	
	// OT方式のための追加フィールド
	private int clientSequence = 0; // クライアント側のシーケンス番号
	private int lastServerSequence = 0; // 最後に受信したサーバーシーケンス番号
	private OperationTransformHelper otHelper; // OT操作送受信ヘルパー
	
	// ドラッグ状態管理のための追加フィールド
	private HashMap<String, DragState> draggingElements = new HashMap<String, DragState>(); // 現在ドラッグ中の要素
	private HashMap<String, List<RemoteOperation>> pendingRemoteOps = new HashMap<String, List<RemoteOperation>>(); // ドラッグ中にバッファされるリモート操作

	FocusPanel topLeft = new FocusPanel();
	FocusPanel top = new FocusPanel();
	FocusPanel topRight = new FocusPanel();
	FocusPanel right = new FocusPanel();
	FocusPanel bottomRight = new FocusPanel();
	FocusPanel bottom = new FocusPanel();
	FocusPanel bottomLeft = new FocusPanel();
	FocusPanel left = new FocusPanel();

	private final HashMap<FocusPanel, Direction> directionPanels = new HashMap<FocusPanel, Direction>() {
		{
			this.put(DrawerPanel.this.topLeft, Direction.UP_LEFT);
			this.put(DrawerPanel.this.top, Direction.UP);
			this.put(DrawerPanel.this.topRight, Direction.UP_RIGHT);
			this.put(DrawerPanel.this.right, Direction.RIGHT);
			this.put(DrawerPanel.this.bottomRight, Direction.DOWN_RIGHT);
			this.put(DrawerPanel.this.bottom, Direction.DOWN);
			this.put(DrawerPanel.this.bottomLeft, Direction.DOWN_LEFT);
			this.put(DrawerPanel.this.left, Direction.LEFT);
		}
	};

	private ResizeHandler resizeHandler; // finalを外す！

	// --- ここからが改造箇所だ！ ---

	/**
	 * Default constructor of a DrawerPanel
	 * NewViewer.java が必要とする、引数なしの設計図だ！
	 */
	public DrawerPanel() {
		super();
		init(350, 10, null);
	}

	/**
	 * DrawerBase.java が必要とする、DrawerBaseを受け取る設計図だ！
	 */
	public DrawerPanel(DrawerBase drawerBase) {
		super();
		init(350, 10, drawerBase);
	}

	/**
	 * 古いコードが使っていた、数字だけを受け取る設計図だ！
	 */
	public DrawerPanel(final int widthDiffBetweenWindow, final int heightDiffBetweenWindow) {
		super();
		init(widthDiffBetweenWindow, heightDiffBetweenWindow, null);
	}

	/**
	 * 全ての部品を組み立てる、メインの初期化メソッドだ！
	 */
	private void init(final int widthDiffBetweenWindow, final int heightDiffBetweenWindow, final DrawerBase drawerBase) {
	// --- 改造箇所ここまで ---
		this.drawerBase = drawerBase;

		ThemeManager.setCurrentTheme((Theme.getThemeFromIndex(OptionsManager.get("Theme"))));
		GfxManager.setPlatform(OptionsManager.get("GraphicEngine"));
		GeometryManager.setPlatform(OptionsManager.get("GeometryStyle"));
		if (OptionsManager.get("AutoResolution") == 0) {
			this.width = OptionsManager.get("Width");
			this.height = OptionsManager.get("Height");
		} else {
			this.width = (int) (Window.getClientWidth()) - widthDiffBetweenWindow;
			this.height = (int) (Window.getClientHeight()) - heightDiffBetweenWindow;
		}

		final boolean isShadowed = OptionsManager.get("Shadowed") == 1;
		Logger.getGlobal().info("Creating drawer");

		this.uMLCanvas = new UMLCanvas(new UMLDiagram(UMLDiagram.Type.getUMLDiagramFromIndex(OptionsManager.get("DiagramType"))), this.width,
				this.height);

		this.add(this.uMLCanvas);

		final int directionPanelSizes = OptionsManager.get("DirectionPanelSizes");

		final HashMap<FocusPanel, Point> panelsSizes = this.makeDirectionPanelsSizes(directionPanelSizes);
		final HashMap<FocusPanel, Point> panelsPositions = this.makeDirectionPanelsPositions(directionPanelSizes);

		for (final Entry<FocusPanel, Direction> panelEntry : this.directionPanels.entrySet()) {
			final FocusPanel panel = panelEntry.getKey();
			final Direction direction = panelEntry.getValue();
			DOM.setStyleAttribute(panel.getElement(), "backgroundColor", ThemeManager.getTheme().getDirectionPanelColor().toString());
			DOM.setStyleAttribute(panel.getElement(), "opacity", Double.toString(((double) OptionsManager.get("DirectionPanelOpacity")) / 100));
			panel.addMouseDownHandler(new MouseDownHandler() {
				@Override
				public void onMouseDown(final MouseDownEvent event) {

					for (double d = ((double) OptionsManager.get("DirectionPanelOpacity")) / 100; d <= ((double) OptionsManager
							.get("DirectionPanelMaxOpacity")) / 100; d += 0.05) {
						final double opacity = Math.ceil(d * 100) / 100;

						new Scheduler.Task("Opacifying") {
							@Override
							public void process() {
								DOM.setStyleAttribute(panel.getElement(), "opacity", Double.toString(opacity));
							}
						};
					}
					new Scheduler.Task("MovingAllArtifacts") {
						@Override
						public void process() {
							Scheduler.cancel("MovingAllArtifactsRecursive");
							DrawerPanel.this.uMLCanvas.moveAll(direction.withSpeed(Direction.getDependingOnQualityLevelSpeed()), true);
						}
					};
				}
			});
			panel.addMouseOutHandler(new MouseOutHandler() {
				@Override
				public void onMouseOut(final MouseOutEvent event) {
					Scheduler.cancel("Opacifying");
					Scheduler.cancel("MovingAllArtifacts");
					Scheduler.cancel("MovingAllArtifactsRecursive");
					for (final FocusPanel onePanel : DrawerPanel.this.directionPanels.keySet()) {
						double currentOpacity = 0;
						try {
							currentOpacity = Math.ceil(Double.parseDouble(DOM.getStyleAttribute(onePanel.getElement(), "opacity")) * 100) / 100;
						} catch (final Exception ex) {
							Logger.getGlobal().severe("Unable to parse element opacity : " + ex);
						}
						for (double d = currentOpacity; d >= ((double) OptionsManager.get("DirectionPanelOpacity")) / 100; d -= 0.05) {
							final double opacity = Math.ceil(d * 100) / 100;

							new Scheduler.Task("Desopacifying") {
								@Override
								public void process() {
									DOM.setStyleAttribute(onePanel.getElement(), "opacity", Double.toString(opacity));
								}
							};
						}
					}
					DOM.setStyleAttribute(panel.getElement(), "backgroundColor", ThemeManager.getTheme().getDirectionPanelColor().toString());
				}
			});
			panel.addMouseDownHandler(new MouseDownHandler() {
				@Override
				public void onMouseDown(final MouseDownEvent event) {
					DOM.setStyleAttribute(panel.getElement(), "backgroundColor", ThemeManager.getTheme().getDirectionPanelPressedColor().toString());
					Scheduler.cancel("MovingAllArtifactsRecursive");
				}
			});
			panel.addMouseUpHandler(new MouseUpHandler() {
				@Override
				public void onMouseUp(final MouseUpEvent event) {
					Scheduler.cancel("Opacifying");
					Scheduler.cancel("MovingAllArtifacts");
					Scheduler.cancel("MovingAllArtifactsRecursive");
					for (final FocusPanel onePanel : DrawerPanel.this.directionPanels.keySet()) {
						double currentOpacity = 0;
						try {
							currentOpacity = Math.ceil(Double.parseDouble(DOM.getStyleAttribute(onePanel.getElement(), "opacity")) * 100) / 100;
						} catch (final Exception ex) {
							Logger.getGlobal().severe("Unable to parse element opacity : " + ex);
						}
						for (double d = currentOpacity; d >= ((double) OptionsManager.get("DirectionPanelOpacity")) / 100; d -= 0.05) {
							final double opacity = Math.ceil(d * 100) / 100;

							new Scheduler.Task("Desopacifying") {
								@Override
								public void process() {
									DOM.setStyleAttribute(onePanel.getElement(), "opacity", Double.toString(opacity));
								}
							};
						}
					}
					DOM.setStyleAttribute(panel.getElement(), "backgroundColor", ThemeManager.getTheme().getDirectionPanelColor().toString());
				}
			});

			panel.addMouseMoveHandler(new MouseMoveHandler() {
				@Override
				public void onMouseMove(final MouseMoveEvent event) {
					Mouse.move(new Point(event.getClientX(), event.getClientY()), event.getNativeButton(), event.isControlKeyDown(), event
							.isAltKeyDown(), event.isShiftKeyDown(), event.isMetaKeyDown());
				}
			});
			final Point panelPosition = panelsPositions.get(panel);
			final Point panelSize = panelsSizes.get(panel);
			panel.setPixelSize(panelSize.getX(), panelSize.getY());
			this.add(panel, panelPosition.getX(), panelPosition.getY());

		}

		Logger.getGlobal().info("Canvas added");
		if (isShadowed) {
			Logger.getGlobal().info("Making shadow");
			this.makeShadow();
		} else {
			this.uMLCanvas.setStylePrimaryName("canvas");
		}

		this.resizeHandler = new ResizeHandler() {
			public void onResize(final ResizeEvent resizeEvent) {
				if (OptionsManager.get("AutoResolution") == 1) {
					DrawerPanel.this.width = (int) (resizeEvent.getWidth() - widthDiffBetweenWindow);
					DrawerPanel.this.height = (int) (resizeEvent.getHeight() - heightDiffBetweenWindow);
					DrawerPanel.this.setPixelSize(DrawerPanel.this.width, DrawerPanel.this.height);
					DrawerPanel.this.uMLCanvas.setPixelSize(DrawerPanel.this.width, DrawerPanel.this.height);
					DrawerPanel.this.uMLCanvas.setSize(DrawerPanel.this.width, DrawerPanel.this.height);
					GfxManager.getPlatform().setSize(Session.getActiveCanvas().getDrawingCanvas(), DrawerPanel.this.width, DrawerPanel.this.height);
					DrawerPanel.this.clearShadow();
					DrawerPanel.this.makeShadow();
					final HashMap<FocusPanel, Point> panelsNewSizes = DrawerPanel.this.makeDirectionPanelsSizes(directionPanelSizes);
					final HashMap<FocusPanel, Point> panelsNewPositions = DrawerPanel.this.makeDirectionPanelsPositions(directionPanelSizes);
					for (final FocusPanel panel : DrawerPanel.this.directionPanels.keySet()) {
						final Point panelPosition = panelsNewPositions.get(panel);
						final Point panelSize = panelsNewSizes.get(panel);
						panel.setPixelSize(panelSize.getX(), panelSize.getY());
						DrawerPanel.this.setWidgetPosition(panel, panelPosition.getX(), panelPosition.getY());
					}
					DrawerPanel.this.uMLCanvas.clearArrows();
					DrawerPanel.this.uMLCanvas.makeArrows();
				}
			}

		};
		Window.addResizeHandler(this.resizeHandler);
		Logger.getGlobal().info("Setting active canvas");
		Session.setActiveCanvas(this.uMLCanvas);
		Logger.getGlobal().info("Disabling browser events");
		GWTUMLDrawerHelper.disableBrowserEvents();
		Logger.getGlobal().info("Init end");
	}

	// --- ここからが改造箇所だ！ ---
	/**
	 * WebSocketClientがDrawerBaseにたどり着くための"橋"だ！
	 */
	public DrawerBase getDrawerBaseInstance() {
		return this.drawerBase;
	}
	
	/**
	 * 指定されたIDの図形（アーティファクト）の、指定された部分のテキストを更新するメソッドだ。
	 * @param elementId 更新対象の図形のID
	 * @param partId 更新対象の部分を識別するID
	 * @param newText 新しいテキスト
	 */
	public void updateArtifactText(String elementId, String partId, String newText) {
	    try {
	        int id = Integer.parseInt(elementId.substring("element-".length()));
	        UMLArtifact artifact = UMLArtifact.getArtifactById(id);

	        if (artifact != null) {
	            // ClassArtifactのクラス名が変更された場合
	            if (artifact instanceof ClassArtifact && partId.contains("ClassPartNameArtifact")) {
	                ((ClassArtifact) artifact).getUMLClass().setName(newText);
	            }
	            // 他にも属性や操作名など、様々な部分の更新処理をここに追加していくことになる

	            // 変更を画面に反映させるために、図形を再描画する
	            artifact.rebuildGfxObject();
	        }
	    } catch (Exception e) {
	        System.err.println("テキストの更新に失敗: " + e.getMessage());
	    }
	}
	// --- 改造箇所ここまで ---

	/**
	 * Getter for the height
	 * @return the height
	 */
	public final int getHeight() {
		return this.height;
	}

	/**
	 * Getter for the uMLCanvas
	 * @return the uMLCanvas
	 */
	public final UMLCanvas getUMLCanvas() {
		return this.uMLCanvas;
	}

	/**
	 * Getter for the width
	 * @return the width
	 */
	public final int getWidth() {
		return this.width;
	}

	/**
	 * Setter for the height
	 * @param height
	 */
	public final void setHeight(final int height) {
		this.height = height;
	}

	/**
	 * Setter for the width
	 * @param width
	 */
	public final void setWidth(final int width) {
		this.width = width;
	}

	void addDefaultNode() {
		final Type type = UMLDiagram.Type.getUMLDiagramFromIndex(OptionsManager.get("DiagramType"));
		if (type.isClassType()) {
			final ClassArtifact defaultclass = new ClassArtifact("Class1");
			defaultclass.setLocation(new Point(this.width / 2, this.height / 2));
			this.uMLCanvas.add(defaultclass);
		}
		if (type.isObjectType()) {
			final ObjectArtifact defaultobject = new ObjectArtifact("obj1", "Object1");
			defaultobject.setLocation(new Point(this.width / 3, this.height / 3));
			this.uMLCanvas.add(defaultobject);
		}
		if (type == Type.SEQUENCE) {
			final LifeLineArtifact defaultLifeLineArtifact = new LifeLineArtifact("LifeLine1", "ll1");
			defaultLifeLineArtifact.setLocation(new Point(this.width / 2, this.height / 2));
			this.uMLCanvas.add(defaultLifeLineArtifact);
		}
	}

	void clearShadow() {
		this.remove(this.bottomShadow);
		this.remove(this.rightShadow);
		this.remove(this.bottomRightCornerShadow);
		this.remove(this.topRightCornerShadow);
		this.remove(this.bottomLeftCornerShadow);
	}

	void makeShadow() {
		final int shadowSize = 8;
		this.setWidth(this.width + shadowSize + this.getAbsoluteLeft() + "px");
		this.setHeight(this.height + shadowSize + this.getAbsoluteTop() + "px");

		this.bottomShadow = new SimplePanel();
		this.bottomShadow.setPixelSize(this.width - shadowSize, shadowSize);
		this.bottomShadow.setStylePrimaryName("bottomShadow");
		this.add(this.bottomShadow, shadowSize, this.height);

		this.rightShadow = new SimplePanel();
		this.rightShadow.setPixelSize(shadowSize, this.height - shadowSize);
		this.rightShadow.setStylePrimaryName("rightShadow");
		this.add(this.rightShadow, this.width, shadowSize);

		this.bottomRightCornerShadow = new SimplePanel();
		this.bottomRightCornerShadow.setPixelSize(shadowSize, shadowSize);
		this.bottomRightCornerShadow.setStylePrimaryName("bottomRightCornerShadow");
		this.add(this.bottomRightCornerShadow, this.width, this.height);

		this.topRightCornerShadow = new SimplePanel();
		this.topRightCornerShadow.setPixelSize(shadowSize, shadowSize);
		this.topRightCornerShadow.setStylePrimaryName("topRightCornerShadow");
		this.add(this.topRightCornerShadow, this.width, 0);

		this.bottomLeftCornerShadow = new SimplePanel();
		this.bottomLeftCornerShadow.setPixelSize(shadowSize, shadowSize);
		this.bottomLeftCornerShadow.setStylePrimaryName("bottomLeftCornerShadow");
		this.add(this.bottomLeftCornerShadow, 0, this.height);
	}

	@Override
	protected void onAttach() {
		super.onAttach();
	}

	private HashMap<FocusPanel, Point> makeDirectionPanelsPositions(final int directionPanelSizes) {
		return new HashMap<FocusPanel, Point>() {
			{
				this.put(DrawerPanel.this.topLeft, Point.getOrigin());
				this.put(DrawerPanel.this.top, new Point(directionPanelSizes, 0));
				this.put(DrawerPanel.this.topRight, new Point(DrawerPanel.this.getWidth() - directionPanelSizes, 0));
				this.put(DrawerPanel.this.right, new Point(DrawerPanel.this.getWidth() - directionPanelSizes, directionPanelSizes));
				this.put(DrawerPanel.this.bottomRight, new Point(DrawerPanel.this.getWidth() - directionPanelSizes, DrawerPanel.this.getHeight() - directionPanelSizes));
				this.put(DrawerPanel.this.bottom, new Point(directionPanelSizes, DrawerPanel.this.getHeight() - directionPanelSizes));
				this.put(DrawerPanel.this.bottomLeft, new Point(0, DrawerPanel.this.getHeight() - directionPanelSizes));
				this.put(DrawerPanel.this.left, new Point(0, directionPanelSizes));
			}
		};
	}

	private HashMap<FocusPanel, Point> makeDirectionPanelsSizes(final int directionPanelSizes) {
		return new HashMap<FocusPanel, Point>() {
			{
				this.put(DrawerPanel.this.topLeft, new Point(directionPanelSizes, directionPanelSizes));
				this.put(DrawerPanel.this.top, new Point(DrawerPanel.this.getWidth() - 2 * directionPanelSizes, directionPanelSizes));
				this.put(DrawerPanel.this.topRight, new Point(directionPanelSizes, directionPanelSizes));
				this.put(DrawerPanel.this.right, new Point(directionPanelSizes, DrawerPanel.this.getHeight() - 2 * directionPanelSizes));
				this.put(DrawerPanel.this.bottomRight, new Point(directionPanelSizes, directionPanelSizes));
				this.put(DrawerPanel.this.bottom, new Point(DrawerPanel.this.getWidth() - 2 * directionPanelSizes, directionPanelSizes));
				this.put(DrawerPanel.this.bottomLeft, new Point(directionPanelSizes, directionPanelSizes));
				this.put(DrawerPanel.this.left, new Point(directionPanelSizes, DrawerPanel.this.getHeight() - 2 * directionPanelSizes));
			}
		};
	}

	public void clearCanvas() {
		this.uMLCanvas.clearCanvas();
	}

	public void fromURL(String url, boolean isForPasting) {
		this.uMLCanvas.fromURL(url, isForPasting);
	}

	/**
	 * OperationTransformHelperを初期化
	 * WebSocket接続確立後に呼び出す
	 */
	public void initializeOTHelper(String userId, int exerciseId) {
	    WebSocketClient webSocketClient = getWebSocketClient();
	    if (webSocketClient != null) {
	        this.otHelper = new OperationTransformHelper(webSocketClient, userId, exerciseId);
	    }
	}
	
	/**
	 * テキスト変更をOT方式で送信
	 * 既存のsendTextUpdateメソッドの代わりに使用
	 */
	public void sendTextChangeWithOT(String elementId, String partId, String beforeText, String afterText) {
	    if (otHelper != null) {
	        otHelper.sendTextChangeWithOT(elementId, partId, beforeText, afterText);
	    }
	}
	
	/**
	 * サーバーからのOT操作を適用
	 * WebSocketClientから呼び出される
	 */
	public void applyOTOperation(int serverSequence, String elementId, String partId, 
	                             String afterText, String userId, boolean isOwnOperation) {
	    if (otHelper != null) {
	        // サーバーシーケンスを更新
	        otHelper.setLastServerSequence(serverSequence);
	    }
	    
	    // 自分の操作の場合は既に適用済みなので何もしない
	    if (isOwnOperation) {
	        return;
	    }
	    
	    // 他ユーザーの操作の場合は、テキストを更新
	    updateArtifactText(elementId, partId, afterText);
	}
	
	/**
	 * OTヘルパーを取得（テスト用）
	 */
	public OperationTransformHelper getOTHelper() {
	    return otHelper;
	}
	
	/**
	 * WebSocketClientを取得（プライベートメソッド）
	 * DrawerBaseから取得する実装を想定
	 */
	private WebSocketClient getWebSocketClient() {
	    if (drawerBase != null) {
	        return drawerBase.getWebSocketClient();
	    }
	    return null;
	}
	
	/**
	 * パッチを適用してテキストを更新する（WebSocketClientから呼ばれる）
	 */
	public void applyPatchToArtifactText(String elementId, String partId, String patchText) {
	    try {
	        int id = Integer.parseInt(elementId.substring("element-".length()));
	        UMLArtifact artifact = UMLArtifact.getArtifactById(id);

	        if (artifact != null) {
	            String currentText = "";
	            // クラス名の場合
	            if (artifact instanceof ClassArtifact && partId.contains("ClassPartNameArtifact")) {
	                currentText = ((ClassArtifact) artifact).getUMLClass().getName();
	            }
	            // ※他のアーティファクトタイプ（属性、操作など）への対応も必要ならここに追加

	            // パッチ適用 (DiffMatchPatchGwtを使用)
	            DiffMatchPatchGwt dmp = new DiffMatchPatchGwt();
	            JavaScriptObject patches = dmp.patchFromText(patchText);
	            
	            // パッチ適用結果を取得（配列の0番目がテキスト）
	            // 注意: 以下の行は dmp.patchApply の戻り値の型によって調整が必要な場合がありますが
	            // まずは標準的な実装として配置します
	            String newText = dmp.patchApply(patches, currentText);

	            // 反映
	            if (artifact instanceof ClassArtifact && partId.contains("ClassPartNameArtifact")) {
	                ((ClassArtifact) artifact).getUMLClass().setName(newText);
	            }
	            
	            artifact.rebuildGfxObject();
	        }
	    } catch (Exception e) {
	        System.err.println("パッチ適用エラー: " + e.getMessage());
	    }
	}
	
	// ========== ドラッグ状態管理のための内部クラスとメソッド ==========
	
	/**
	 * ドラッグ操作の状態を表す内部クラス
	 */
	private static class DragState {
		public String elementId;      // ドラッグ中の要素のID
		public Point startPosition;   // ドラッグ開始位置
		public Point currentPosition; // 現在のドラッグ位置
		public long startTime;        // ドラッグ開始時刻(ミリ秒)
		public boolean isActive;      // ドラッグが現在アクティブか
		
		public DragState(String elementId, Point startPosition) {
			this.elementId = elementId;
			this.startPosition = startPosition;
			this.currentPosition = startPosition;
			this.startTime = System.currentTimeMillis();
			this.isActive = true;
		}
	}
	
	/**
	 * リモート操作を一時保管するための内部クラス
	 */
	private static class RemoteOperation {
		public String userId;
		public String elementId;
		public String operationType;
		public Point newPosition;  // MOVE操作の場合
		public String partId;      // TEXT_EDIT操作の場合
		public String newText;     // TEXT_EDIT操作の場合
		public long timestamp;
		
		public RemoteOperation(String userId, String elementId, String operationType, long timestamp) {
			this.userId = userId;
			this.elementId = elementId;
			this.operationType = operationType;
			this.timestamp = timestamp;
		}
	}
	
	/**
	 * ドラッグ開始を通知
	 * UMLCanvasまたはアーティファクトのマウスダウンイベントから呼び出される
	 */
	public void notifyDragStart(String elementId, Point startPosition) {
		DragState dragState = new DragState(elementId, startPosition);
		draggingElements.put(elementId, dragState);
		
		// サーバーにドラッグ開始を通知(他のユーザーにゴーストアーティファクト表示の準備をさせる)
		sendDragStart(elementId, startPosition);
	}
	
	/**
	 * ドラッグ中の位置更新を通知
	 */
	public void notifyDragMove(String elementId, Point currentPosition) {
		DragState dragState = draggingElements.get(elementId);
		if (dragState != null && dragState.isActive) {
			dragState.currentPosition = currentPosition;
		}
	}
	
	/**
	 * ドラッグ完了を通知
	 * UMLCanvasまたはアーティファクトのマウスアップイベントから呼び出される
	 */
	public void notifyDragEnd(String elementId, Point finalPosition) {
		DragState dragState = draggingElements.get(elementId);
		if (dragState != null) {
			dragState.isActive = false;
			dragState.currentPosition = finalPosition;
			
			// バッファされていたリモート操作を処理
			onDragCompleted(elementId, finalPosition);
			
			// ドラッグ状態をクリア
			draggingElements.remove(elementId);
			pendingRemoteOps.remove(elementId);
		}
	}
	
	/**
	 * ドラッグ開始をサーバーに送信
	 */
	private void sendDragStart(String elementId, Point startPosition) {
		WebSocketClient client = getWebSocketClient();
		if (client != null && client.isOpen()) {
			// WebSocketでDRAG_START操作を送信
			// JSONフォーマット: {"action":"dragStart", "elementId":"...", "x":..., "y":...}
			String message = "{\"action\":\"dragStart\", \"elementId\":\"" + elementId + 
			                 "\", \"userId\":\"" + client.getUserId() + 
			                 "\", \"exerciseId\":" + client.getExerciseId() +
			                 ", \"x\":" + startPosition.getX() + ", \"y\":" + startPosition.getY() + "}";
			client.send(message);
		}
	}
	
	/**
	 * ドラッグ完了後にバッファされた操作を処理し、競合を検出・解決
	 */
	private void onDragCompleted(String elementId, Point finalPosition) {
		List<RemoteOperation> bufferedOps = pendingRemoteOps.get(elementId);
		if (bufferedOps == null || bufferedOps.isEmpty()) {
			// バッファされた操作がない場合、通常通り移動操作を送信
			sendMoveOperation(elementId, finalPosition);
			return;
		}
		
		// バッファされた操作の中に同じ要素への移動操作があるかチェック
		RemoteOperation concurrentMove = null;
		for (RemoteOperation op : bufferedOps) {
			if ("MOVE".equals(op.operationType) && elementId.equals(op.elementId)) {
				concurrentMove = op;
				break;
			}
		}
		
		if (concurrentMove != null) {
			// 競合が発生: Last-Write-Wins戦略を適用
			DragState dragState = new DragState(elementId, finalPosition);
			long localTimestamp = dragState.startTime;
			
			if (concurrentMove.timestamp < localTimestamp) {
				// リモート操作の方が古い → ローカル操作を優先
				sendMoveOperation(elementId, finalPosition);
				// ローカルUIは既に正しい位置にあるはず
			} else {
				// リモート操作の方が新しい → リモート操作を優先
				applyRemoteMoveOperation(elementId, concurrentMove.newPosition);
				// ローカルの移動は破棄される(サーバーに送信しない)
			}
		} else {
			// 競合なし: 通常通り送信
			sendMoveOperation(elementId, finalPosition);
		}
		
		// バッファされた他の操作(テキスト編集など)を適用
		for (RemoteOperation op : bufferedOps) {
			if (!"MOVE".equals(op.operationType)) {
				applyBufferedOperation(op);
			}
		}
	}
	
	/**
	 * 移動操作をサーバーに送信
	 */
	private void sendMoveOperation(String elementId, Point position) {
		WebSocketClient client = getWebSocketClient();
		if (client != null && client.isOpen()) {
			String message = "{\"action\":\"move\", \"elementId\":\"" + elementId + 
			                 "\", \"userId\":\"" + client.getUserId() + 
			                 "\", \"exerciseId\":" + client.getExerciseId() +
			                 ", \"x\":" + position.getX() + ", \"y\":" + position.getY() + 
			                 ", \"timestamp\":" + System.currentTimeMillis() + "}";
			client.send(message);
		}
	}
	
	/**
	 * リモートからの移動操作を適用(UIを更新)
	 */
	private void applyRemoteMoveOperation(String elementId, Point newPosition) {
		try {
			int id = Integer.parseInt(elementId.substring("element-".length()));
			UMLArtifact artifact = UMLArtifact.getArtifactById(id);
			if (artifact != null) {
				artifact.setLocation(newPosition);
				artifact.rebuildGfxObject();
			}
		} catch (Exception e) {
			System.err.println("リモート移動操作の適用エラー: " + e.getMessage());
		}
	}
	
	/**
	 * バッファされた操作を適用
	 */
	private void applyBufferedOperation(RemoteOperation op) {
		if ("TEXT_EDIT".equals(op.operationType)) {
			updateArtifactText(op.elementId, op.partId, op.newText);
		}
		// 他の操作タイプもここで処理
	}
	
	/**
	 * リモート操作を受信したときに呼び出される
	 * WebSocketClientから呼び出される
	 */
	public void applyRemoteOperation(String userId, String elementId, String operationType, 
	                                 Point newPosition, String partId, String newText, long timestamp) {
		// ドラッグ中の要素への操作かチェック
		if (draggingElements.containsKey(elementId)) {
			// ドラッグ中 → バッファに保存
			RemoteOperation remoteOp = new RemoteOperation(userId, elementId, operationType, timestamp);
			remoteOp.newPosition = newPosition;
			remoteOp.partId = partId;
			remoteOp.newText = newText;
			
			List<RemoteOperation> buffer = pendingRemoteOps.get(elementId);
			if (buffer == null) {
				buffer = new ArrayList<RemoteOperation>();
				pendingRemoteOps.put(elementId, buffer);
			}
			buffer.add(remoteOp);
			
			// ゴーストアーティファクトを表示(視覚的フィードバック)
			if ("MOVE".equals(operationType)) {
				showGhostArtifact(elementId, newPosition, userId);
			}
		} else {
			// ドラッグ中でない → 即座に適用
			if ("MOVE".equals(operationType)) {
				applyRemoteMoveOperation(elementId, newPosition);
			} else if ("TEXT_EDIT".equals(operationType)) {
				updateArtifactText(elementId, partId, newText);
			}
		}
	}
	
	/**
	 * ゴーストアーティファクト(半透明の位置プレビュー)を表示
	 */
	private void showGhostArtifact(String elementId, Point position, String userId) {
		// TODO: GWT UIでゴーストアーティファクトを表示する実装
		// 半透明の図形を指定位置に描画し、ユーザーIDラベルを付ける
		// 実装例:
		// - 一時的なSimplePanelを作成し、半透明スタイルを適用
		// - 指定位置に配置
		// - 数秒後に自動削除、またはドラッグ完了時に削除
		Logger.getGlobal().info("ゴーストアーティファクト表示: elementId=" + elementId + ", userId=" + userId);
	}
}