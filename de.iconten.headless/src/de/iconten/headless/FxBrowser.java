package de.iconten.headless;

import java.util.Observable;
import java.util.Observer;

import com.sun.glass.ui.monocle.MonocleLauncher;

import de.iconten.headless.FxApplication.DataFXObject;

public class FxBrowser implements Observer {
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

	@Override
	public void update(Observable o, Object arg) {
		System.out.println("Main called in " + Thread.currentThread().getName());
		if (((String) arg).equalsIgnoreCase("main")) {
			System.out.println("Main called: " + Integer.toString(((DataFXObject) o).getWert()));
		}
	}

	public void runFxBrowser() throws Exception {
		FxApplication.getDataFXObject().addObserver(this);
		while (!FxApplication.isInitDone()) {
			Thread.sleep(100);
		}
		FxApplication.getDataFXObject().loadPage("https://www.google.de");
		Thread.sleep(5000);
		FxApplication.getDataFXObject().loadPage("https://www.github.com");
		Thread.sleep(5000);
		FxApplication.getDataFXObject().loadPage("https://stackoverflow.com");
	}

}
