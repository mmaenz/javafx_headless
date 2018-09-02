package de.iconten.headless;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import com.sun.javafx.application.PlatformImpl;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

/**
 * SwingFXWebView
 */
public class SwingFXWebView extends JPanel {

	private Stage stage;
	private static WebView browser;
	private JFXPanel jfxPanel;
	private JButton swingButton;
	private WebEngine webEngine;

	public SwingFXWebView() {
		initComponents();
	}

	public static void main(String... args) throws Exception {
		// Run this later:
		SwingUtilities.invokeLater(() -> {
			final JFrame frame = new JFrame();

			frame.getContentPane().add(new SwingFXWebView());

			frame.setMinimumSize(new Dimension(640, 480));
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setVisible(true);
		});
		Thread.sleep(1000);
		browser.getEngine().load("https://www.ragnaruk.de");
	}

	private void initComponents() {

		jfxPanel = new JFXPanel();
		createScene();

		setLayout(new BorderLayout());
		add(jfxPanel, BorderLayout.CENTER);

		swingButton = new JButton();
		swingButton.addActionListener(e -> Platform.runLater(() -> webEngine.reload()));
		swingButton.setText("Reload");

		add(swingButton, BorderLayout.SOUTH);
	}

	/**
	 * createScene
	 * 
	 * Note: Key is that Scene needs to be created and run on "FX user thread"
	 * NOT on the AWT-EventQueue Thread
	 * 
	 */
	private void createScene() {
		PlatformImpl.startup(() -> {

			stage = new Stage();

			stage.setTitle("Hello Java FX");
			stage.setResizable(true);

			final Group root = new Group();
			final Scene scene = new Scene(root, 80, 20);
			stage.setScene(scene);

			// Set up the embedded browser:
			browser = new WebView();
			webEngine = browser.getEngine();
			webEngine.load("http://www.google.com");

			final ObservableList<Node> children = root.getChildren();
			children.add(browser);

			jfxPanel.setScene(scene);
		});
	}
}