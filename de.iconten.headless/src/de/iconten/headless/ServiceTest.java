package de.iconten.headless;

import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.RotateTransition;
import javafx.application.Application;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.Worker.State;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.BoxBlur;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.HBox;
import javafx.scene.layout.HBoxBuilder;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.StackPaneBuilder;
import javafx.scene.layout.VBox;
import javafx.scene.layout.VBoxBuilder;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

public class ServiceTest extends Application {
	public static void main(String[] args) throws Exception {
		launch(args);
	}

	@Override
	public void start(final Stage stage) throws Exception {
		// set up some controls.
		final Label statusLabel = new Label();
		final Label celebrateLabel = new Label();
		final Button searchButton = new Button("Search");
		searchButton.setTooltip(new Tooltip("Look for friends"));
		final ListView<String> peopleView = new ListView<>();
		peopleView.setMaxHeight(Double.MAX_VALUE);
		peopleView.setTooltip(new Tooltip("Friends we find are displayed here"));
		final ProgressBar progressBar = new ProgressBar();
		progressBar.prefWidthProperty().bind(peopleView.widthProperty());

		// create a service.
		final Service friendFinder = new Service<ObservableList<String>>() {
			@Override
			protected Task createTask() {
				return new Task<ObservableList<String>>() {
					@Override
					protected ObservableList<String> call() throws InterruptedException {
						updateMessage("Finding friends . . .");
						updateProgress(0, 10);
						for (int i = 0; i < 10; i++) {
							Thread.sleep(300);
							updateProgress(i + 1, 10);
						}
						updateMessage("Found them.");
						return FXCollections.observableArrayList("John", "Jim", "Geoff", "Jill", "Suki", "Chiang", "Lin");
					}
				};
			}
		};

		// bind interesting service properties to the controls.
		statusLabel.textProperty().bind(friendFinder.messageProperty());
		searchButton.disableProperty().bind(friendFinder.runningProperty());
		peopleView.itemsProperty().bind(friendFinder.valueProperty());
		progressBar.progressProperty().bind(friendFinder.progressProperty());
		progressBar.visibleProperty()
				.bind(friendFinder.progressProperty().isNotEqualTo(new SimpleDoubleProperty(ProgressBar.INDETERMINATE_PROGRESS)));

		// kick off the service on an action.
		searchButton.setOnAction(actionEvent -> {
			if (!friendFinder.isRunning()) {
				friendFinder.reset();
				friendFinder.start();
			}
		});

		// create a distracting square with different colors for different states.
		final Color normalDistractorColor = Color.BURLYWOOD;
		final Color runningDistractorColor = Color.DARKGREEN;
		final Color highlightedDistractorColor = Color.FIREBRICK;
		final Rectangle distractor = new Rectangle(25, 25, normalDistractorColor);
		distractor.setUserData(false); // maintains whether or not the mouse is in the distractor.
		distractor.setOpacity(0.8);
		Tooltip.install(distractor, new Tooltip("If you are looking for friends, you can click on me to stop searching"));

		// rotate the distractor to show that animation and stuff still continues while the work is being done.
		final RotateTransition rt = new RotateTransition(Duration.millis(3000), distractor);
		rt.setByAngle(360);
		rt.setCycleCount(Animation.INDEFINITE);
		rt.setInterpolator(Interpolator.LINEAR);
		rt.play();

		// create some effects for the animation.
		final BoxBlur normal = new BoxBlur();
		final BoxBlur highlighted = new BoxBlur();
		highlighted.setInput(new DropShadow());
		distractor.setEffect(normal);

		// the distracting animation can be used to cancel the friend lookup.
		distractor.setOnMouseClicked(mouseEvent -> {
			if (friendFinder.isRunning()) {
				friendFinder.cancel();
			}
		});
		distractor.setOnMouseEntered(mouseEvent -> {
			distractor.setUserData(true);
			if (friendFinder.isRunning()) {
				distractor.setEffect(highlighted);
				distractor.setFill(highlightedDistractorColor);
			}
		});
		distractor.setOnMouseExited(mouseEvent -> {
			distractor.setUserData(false);
			distractor.setEffect(normal);
			distractor.setFill(normalDistractorColor);
		});

		// do something when the service has succeeded.
		friendFinder.stateProperty().addListener((ChangeListener<State>) (observableValue, oldState, newState) -> {
			switch (newState) {
			case SCHEDULED:
				celebrateLabel.setVisible(false);
				progressBar.progressProperty().bind(friendFinder.progressProperty()); // workaround, we should be able to permanently bind to the progress, but unless we do this sometimes the progress does not always reach the end.
				break;
			case READY:
			case RUNNING:
				celebrateLabel.setVisible(false);
				break;
			case SUCCEEDED:
				celebrateLabel.setVisible(true);
				celebrateLabel.setText("Let's grab a beer.");
				progressBar.progressProperty().unbind();
				progressBar.setProgress(1); // workaround, we should be able to permanently bind to the progress, but unless we do this sometimes the progress does not always reach the end. (even this workaround didn't work, so I have no idea about this...)
				break;
			case CANCELLED:
			case FAILED:
				celebrateLabel.setVisible(true);
				celebrateLabel.setText("Bummer dude, party's over.");
				break;
			}
		});

		// maintain the distractor colors while the service is running.
		friendFinder.runningProperty().addListener((ChangeListener<Boolean>) (observableValue, aBoolean, isRunning) -> {
			if (isRunning) {
				if (!(Boolean) distractor.getUserData()) {
					distractor.setEffect(normal);
					distractor.setFill(runningDistractorColor);
				} else {
					distractor.setEffect(highlighted);
					distractor.setFill(highlightedDistractorColor);
				}
			} else {
				distractor.setEffect(normal);
				distractor.setFill(normalDistractorColor);
			}
		});

		// layout the scene.
		stage.setTitle("Friend Finder");
		VBox.setVgrow(peopleView, Priority.ALWAYS);
		HBox.setMargin(statusLabel, new Insets(3, 0, 0, 0)); // workaround because setting HBox alignment BASELINE_LEFT causes layout glitches.
		HBox.setMargin(celebrateLabel, new Insets(3, 0, 0, 0)); // workaround because setting HBox alignment BASELINE_LEFT causes layout glitches.
		HBox.setHgrow(statusLabel, Priority.SOMETIMES);
		StackPane.setAlignment(distractor, Pos.TOP_RIGHT);
		final Pane layout = StackPaneBuilder.create().alignment(Pos.TOP_RIGHT).children(VBoxBuilder.create().spacing(8)
				.children(VBoxBuilder.create().spacing(5)
						.children(HBoxBuilder.create().spacing(10).children(searchButton, statusLabel, celebrateLabel).build(), progressBar).build(),
						peopleView)
				.build(), distractor).build();
		layout.setStyle("-fx-background-color: cornsilk; -fx-padding:10; -fx-font-size: 16;");
		final Scene scene = new Scene(layout, 480, 360);
		stage.setScene(scene);
		stage.show();
	}
}