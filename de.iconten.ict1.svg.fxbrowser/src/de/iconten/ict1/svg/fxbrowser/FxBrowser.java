package de.iconten.ict1.svg.fxbrowser;

import java.util.concurrent.CountDownLatch;

import com.sun.glass.ui.monocle.MonocleLauncher;

//import com.sun.glass.ui.monocle.MonocleLauncher;

import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

public class FxBrowser {
	private static final int defaultWidth = 1024;
	private static final int defaultHeight = 768;
	private static final boolean defaultHeadless = true;
	private static FxBrowser instance;

	private static class FxData {
		private static Object content = null;

		public static void set(Object obj) {
			content = obj;
		}

		public static Object get() {
			return content;
		}
	}

	public static void exit() {
		Platform.exit();
	}

	public static WebView getWebView() throws Exception {
		if (instance == null) {
			getFxBrowser();
		}
		return FxApplication.getView();
	}

	public static FxBrowser getFxBrowser(String... args) throws Exception {
		if (args.length == 0) {
			if (instance == null) {
				instance = new FxBrowser(defaultWidth, defaultHeight, defaultHeadless);
				return instance;
			} else {
				if (FxApplication.getView().getWidth() != defaultWidth || FxApplication.getView().getHeight() != defaultHeight) {
					FxApplication.resize(defaultWidth, defaultHeight);
				}
				return instance;
			}
		} else if (args.length == 3) {
			if (instance != null) {
				FxApplication.resize(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
				return instance;
			} else {
				instance = new FxBrowser(Integer.parseInt(args[0]), Integer.parseInt(args[1]), Boolean.parseBoolean(args[2]));
				return instance;
			}
		} else {
			throw new Exception("Unknown arguments. Use int, int, boolean.");
		}
	}

	public FxBrowser(int width, int height, boolean headless) throws Exception {
		new Thread(() -> {
			try {
				new MonocleLauncher().launch(FxApplication.class,
						new String[] { Integer.toString(width), Integer.toString(height), Boolean.toString(headless) });
			} catch (final Throwable t) {
				System.out.println(t.getMessage());
			}
		}).start();
		while (FxApplication.isInitDone() == false) {
			Thread.sleep(100);
		}
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
		return FxData.get().toString();
	}

	public void runDomainTitleOutput() throws Exception {
		//System.out.println(getTitleFromPage("https://www.github.com"));
		//System.out.println(getTitleFromPage("https://www.google.de"));
		//System.out.println(getTitleFromPage("https://www.yahoo.com"));
		//System.out.println(getTitleFromPage("https://www.stackoverflow.com"));
	}

	public static void main(String... args) throws Exception {
		final FxBrowser browser = FxBrowser.getFxBrowser();
		browser.runDomainTitleOutput();
		Platform.exit();
	}
}
