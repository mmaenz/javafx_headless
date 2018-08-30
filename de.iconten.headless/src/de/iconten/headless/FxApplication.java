package de.iconten.headless;

/* 
 * jBrowserDriver (TM)
 * Copyright (C) 2014-2016 jBrowserDriver committers
 * https://github.com/MachinePublishers/jBrowserDriver
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

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
public class FxApplication extends Application implements Observer {
	private static DataFXObject fxInstance;
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

	public static DataFXObject getDataFXObject() {
		if (fxInstance == null) {
			fxInstance = new DataFXObject();
			setInitDone();
			return fxInstance;
		} else {
			return fxInstance;
		}
	}

	public static class DataFXObject extends Observable {
		private volatile int wert = 0;
		private volatile String url = "";

		public DataFXObject() {
		}

		public int getWert() {
			return wert;
		}

		public void incToFx() {
			wert++;
			setChanged();
			notifyObservers(new String("fx"));
		}

		public void incToMain() {
			wert++;
			setChanged();
			notifyObservers(new String("main"));
		}

		public void loadPage(String url) {
			this.url = url;
			setChanged();
			notifyObservers(new String("fx"));
		}

		public String loadPage() {
			return url;
		}
	}

	/**
	 * Internal use only.
	 */
	public FxApplication() {

	}

	static Stage getStage() {
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

	static WebView getView() {
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

	static void openPage(String url) {
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
		getDataFXObject();
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
		engine.titleProperty().addListener(new TitleListener(stage));
		stage.show();
		synchronized (lock) {
			myStage = stage;
			myView = view;
			lock.notifyAll();
			getDataFXObject().addObserver(this);
		}
		myStage.setOnCloseRequest(e -> Platform.exit());
	}

	@Override
	public void update(Observable o, Object arg) {
		if (((String) arg).equalsIgnoreCase("fx")) {
			System.out.println("FxApplication called: " + Integer.toString(((DataFXObject) o).getWert()));
			try {
				myView.getEngine().load(((DataFXObject) o).loadPage());
			} catch (final Exception ex) {
				System.out.println(ex.getMessage());
			}
			getDataFXObject().incToMain();
		}
	}

}