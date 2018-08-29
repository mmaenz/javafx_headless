package de.iconten.headless;

import com.sun.glass.ui.monocle.MonocleLauncher;

public class HeadlessBrowser {

	public static void main(String... args) throws Exception {
		new Thread(() -> {
			try {
				new MonocleLauncher().launch(HeadlessApp.class,
						new String[] { Integer.toString(1024), Integer.toString(768), Boolean.toString(false) });
			} catch (final Throwable t) {
				System.out.println(t.getMessage());
			}
		}).start();
	}

	public HeadlessBrowser() {
	}
}
