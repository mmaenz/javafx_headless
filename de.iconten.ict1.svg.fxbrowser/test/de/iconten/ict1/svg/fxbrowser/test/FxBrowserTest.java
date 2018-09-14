package de.iconten.ict1.svg.fxbrowser.test;

import org.junit.jupiter.api.Test;

import de.iconten.ict1.svg.fxbrowser.FxBrowser;
import javafx.scene.web.WebView;

public class FxBrowserTest {

	@Test
	public void testFxBrowserDefault() throws Exception {
		final WebView webView = FxBrowser.getFxBrowser().getWebView();
		System.out.println("Default: " + webView.getWidth() + "x" + webView.getHeight());
	}

	@Test
	public void testFxBrowserSmall() throws Exception {
		final WebView webView = FxBrowser.getFxBrowser("320", "240", "true").getWebView();
		System.out.println("Small: " + webView.getWidth() + "x" + webView.getHeight());
	}

	@Test
	public void testFxBrowserLarge() throws Exception {
		final WebView webView = FxBrowser.getFxBrowser("1600", "1200", "true").getWebView();
		System.out.println("Large: " + webView.getWidth() + "x" + webView.getHeight());
	}
}
