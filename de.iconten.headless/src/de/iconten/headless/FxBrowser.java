package de.iconten.headless;

import java.util.concurrent.CountDownLatch;

import com.sun.glass.ui.monocle.MonocleLauncher;

import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.scene.web.WebEngine;

public class FxBrowser {
	public static class FxData {
		private static String content = "";

		public static void set(String s) {
			content = s;
		}

		public static String get() {
			return content;
		}
	}

	private static FxBrowser instance;

	public static FxBrowser getFxBrowser() {
		if (instance == null) {
			instance = new FxBrowser();
			return instance;
		} else {
			return instance;
		}
	}

	public FxBrowser() {
		new Thread(() -> {
			try {
				new MonocleLauncher().launch(FxApplication.class,
						new String[] { Integer.toString(1024), Integer.toString(768), Boolean.toString(true) });
			} catch (final Throwable t) {
				System.out.println(t.getMessage());
			}
		}).start();

	}

	public static void main(String... args) throws Exception {
		final FxBrowser browser = FxBrowser.getFxBrowser();
		browser.runFxBrowser();
		Platform.exit();
	}

	public String getTitleFromPage(String url) throws InterruptedException {
		final CountDownLatch latch = new CountDownLatch(1);
		final String title;
		final Task<String> google = new Task<String>() {
			@Override
			protected String call() throws Exception {
				try {
					final WebEngine engine = FxApplication.getView().getEngine();
					engine.load(url);

					engine.getLoadWorker().stateProperty()
							.addListener((ObservableValue<? extends Worker.State> observable, Worker.State oldValue, Worker.State newValue) -> {
								if (newValue != Worker.State.SUCCEEDED) {
									return;
								}

								FxData.set(engine.getTitle());
								latch.countDown();
							});
					return null;
				} catch (final Exception ex) {
					System.out.println(ex.getMessage());
				}
				return null;
			}
		};

		Platform.runLater(google);
		latch.await();
		return FxData.get();
	}

	public void runFxBrowser() throws Exception {
		while (!FxApplication.isInitDone()) {
			Thread.sleep(100);
		}
		final String test = getTitleFromPage("https://www.google.de");
		System.out.println(test);
	}
}
