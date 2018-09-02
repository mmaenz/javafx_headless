package de.iconten.headless;

import java.util.concurrent.CountDownLatch;

import com.sun.glass.ui.monocle.MonocleLauncher;

import javafx.beans.value.ChangeListener;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;
import javafx.concurrent.WorkerStateEvent;
import javafx.scene.web.WebView;

public class FxBrowser {
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
						new String[] { Integer.toString(1024), Integer.toString(768), Boolean.toString(false) });
			} catch (final Throwable t) {
				System.out.println(t.getMessage());
			}
		}).start();

	}

	public static void main(String... args) throws Exception {
		final FxBrowser browser = FxBrowser.getFxBrowser();
		browser.runFxBrowser();
	}

	public String getTitleFromPage(String url) throws InterruptedException {
		final CountDownLatch latch = new CountDownLatch(1);
		final String title = "";
		final Task<String> google = new Task<String>() {
			@Override
			public String call() {
				final WebView view = FxApplication.getView();
				view.getEngine().getLoadWorker().stateProperty().addListener((ChangeListener<State>) (observable, oldValue, newValue) -> {
					if (newValue != Worker.State.SUCCEEDED) {
						return;
					}
					set(view.getEngine().getTitle());
					return;
				});
				view.getEngine().load(url);
				return null;
			}
		};

		google.addEventHandler(WorkerStateEvent.WORKER_STATE_SUCCEEDED, t -> {
			final String result = google.getValue();
		});

		latch.wait();
		return title;
	}

	public void runFxBrowser() throws Exception {
		while (!FxApplication.isInitDone()) {
			Thread.sleep(100);
		}
		String test = getTitleFromPage("https://www.google.de");
		test = test;
	}
}
