package de.iconten.headless;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.List;

import com.sun.glass.ui.Screen;
import com.sun.glass.ui.monocle.NativePlatform;
import com.sun.glass.ui.monocle.NativePlatformFactory;
import com.sun.javafx.webkit.Accessor;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * Internal use only.
 * 
 */
public class FxApplication extends Application {
	private static FxApplication fxApplication;
	private static final int HISTORY_SIZE = 8;
	private static final Object lock = new Object();
	private static Stage myStage;
	private static WebView myView;
	private int width;
	private int height;
	private boolean headless;
	private static boolean initDone = false;

	public static void setInitDone() {
		initDone = true;
	}

	public static boolean isInitDone() {
		return initDone;
	}

	public static FxApplication getFxApplication() {
		while (fxApplication == null) {
		}
		return fxApplication;
	}

	/**
	 * Internal use only.
	 */
	public FxApplication() {

	}

	public static Stage getStage() {
		synchronized (lock) {
			while (myStage == null) {
				try {
					lock.wait();
				} catch (final InterruptedException e) {
				}
			}
			return myStage;
		}
	}

	public static WebView getView() {
		synchronized (lock) {
			while (myView == null) {
				try {
					lock.wait();
				} catch (final InterruptedException e) {
				}
			}
			return myView;
		}
	}

	public void openPage(String url) {
		getView().getEngine().load(url);
	}

	void init(int width, int height, boolean headless) {
		this.width = width;
		this.height = height;
		this.headless = headless;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void init() throws Exception {
		final List<String> params = getParameters().getRaw();
		width = Integer.parseInt(params.get(0));
		height = Integer.parseInt(params.get(1));
		headless = Boolean.parseBoolean(params.get(2));
	}

	void start() throws Exception {
		start(null);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void start(Stage stage) throws Exception {
		if (headless) {
			System.setProperty("headless.geometry", width + "x" + height);
			final NativePlatform platform = NativePlatformFactory.getNativePlatform();
			final Field field = NativePlatform.class.getDeclaredField("screen");
			field.setAccessible(true);
			field.set(platform, null);
			final Method method = Screen.class.getDeclaredMethod("notifySettingsChanged");
			method.setAccessible(true); //before Java 8u20 this method was private
			method.invoke(null);
		}
		if (stage == null) {
			stage = new Stage();
		}
		Platform.setImplicitExit(false);
		final WebView view = new WebView();
		view.setCache(false);
		final StackPane root = new StackPane();
		root.setCache(false);
		if (headless) {
			stage.initStyle(StageStyle.UNDECORATED);
		}
		final WebEngine engine = view.getEngine();
		final File style = File.createTempFile("jbd_style_", ".css");
		style.deleteOnExit();
		Files.write(style.toPath(), "body::-webkit-scrollbar {width: 0px !important;height:0px !important;}".getBytes("utf-8"));
		engine.setUserStyleSheetLocation(style.toPath().toUri().toURL().toExternalForm());
		engine.getHistory().setMaxSize(HISTORY_SIZE);
		Accessor.getPageFor(engine).setDeveloperExtrasEnabled(false);
		Accessor.getPageFor(engine).setUsePageCache(false);
		root.getChildren().add(view);
		stage.setScene(new Scene(root, width, height));
		stage.sizeToScene();
		stage.show();
		synchronized (lock) {
			myStage = stage;
			myView = view;
			lock.notifyAll();
		}
		myStage.setOnCloseRequest(e -> Platform.exit());
		fxApplication = this;
		setInitDone();
	}
}
