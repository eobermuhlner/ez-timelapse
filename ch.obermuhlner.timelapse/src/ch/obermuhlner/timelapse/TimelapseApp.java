package ch.obermuhlner.timelapse;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.Format;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import ch.obermuhlner.timelapse.CommandExecutor.CommandExecutorListener;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TabPane.TabClosingPolicy;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

public class TimelapseApp extends Application {

	private static final DecimalFormat INTEGER_FORMAT = new DecimalFormat("##0");

	private StringProperty imageDirectoryProperty = new SimpleStringProperty();
	private StringProperty imagePatternProperty = new SimpleStringProperty();
	private IntegerProperty imageStartNumberProperty = new SimpleIntegerProperty();
	private StringProperty videoFileNameProperty = new SimpleStringProperty("output.mp4");
	private IntegerProperty videoRateProperty = new SimpleIntegerProperty(1);
	private IntegerProperty videoResolutionWidthProperty = new SimpleIntegerProperty(1920);
	private IntegerProperty videoResolutionHeightProperty = new SimpleIntegerProperty(1080);

	private StringProperty inputValidationMessage = new SimpleStringProperty();
	
	private StringProperty commandProperty = new SimpleStringProperty();
	private TextArea commandOutputTextArea;

	private Stage primaryStage;
	
	@Override
	public void start(Stage primaryStage) throws Exception {
        this.primaryStage = primaryStage;
        
		Group root = new Group();
        Scene scene = new Scene(root);

        BorderPane mainBorderPane = new BorderPane();
        root.getChildren().add(mainBorderPane);
		
        mainBorderPane.setCenter(createEditor());
                
		primaryStage.setScene(scene);
        primaryStage.show();
	}

	private Node createEditor() {
        TabPane tabPane = new TabPane();
        tabPane.setTabClosingPolicy(TabClosingPolicy.UNAVAILABLE);

    	tabPane.getTabs().add(new Tab("Images", createInputTab()));
    	tabPane.getTabs().add(new Tab("Filter", createFilterTab()));
    	tabPane.getTabs().add(new Tab("Video", createOutputTab()));
    	tabPane.getTabs().add(new Tab("Create", createCreateTab()));

        return tabPane;
	}
	
	private Node createInputTab() {
        GridPane gridPane = new GridPane();
        gridPane.setHgap(2);
        gridPane.setVgap(2);
        
        int rowIndex = 0;
        addDirectoryChooser(gridPane, rowIndex++, "Image Directory", imageDirectoryProperty);
        addTextField(gridPane, rowIndex++, "Image Pattern", imagePatternProperty);
        addTextField(gridPane, rowIndex++, "Image Start Number", imageStartNumberProperty, INTEGER_FORMAT);
        addLabel(gridPane, rowIndex++, "Input Info", inputValidationMessage);

        imageDirectoryProperty.addListener(changeEvent -> {
        	updateImageDirectory();
        });
        
		return gridPane;
	}

	private Node createFilterTab() {
        GridPane gridPane = new GridPane();
        gridPane.setHgap(2);
        gridPane.setVgap(2);
        
        int rowIndex = 0;

        
        return gridPane;
	}

	private Node createOutputTab() {
        GridPane gridPane = new GridPane();
        gridPane.setHgap(2);
        gridPane.setVgap(2);
        
        int rowIndex = 0;

        addTextField(gridPane, rowIndex++, "Output Video File", videoFileNameProperty);
        addTextField(gridPane, rowIndex++, "Frame Rate", videoRateProperty, INTEGER_FORMAT);
        addTextField(gridPane, rowIndex++, "Video Width", videoResolutionWidthProperty, INTEGER_FORMAT);
        addTextField(gridPane, rowIndex++, "Video Height", videoResolutionHeightProperty, INTEGER_FORMAT);

        return gridPane;
	}

	private Node createCreateTab() {
        GridPane gridPane = new GridPane();
        gridPane.setHgap(2);
        gridPane.setVgap(2);
        
        int rowIndex = 0;

        {
	        Button runButton = new Button("Create Video");
	        gridPane.add(runButton, 1, rowIndex++);
	        
	        runButton.addEventHandler(ActionEvent.ACTION, event -> {
	        	List<String> command = new ArrayList<>();
	        	command.add("ffmpeg");
	        	command.add("-y");
	        	command.add("-r");
	        	command.add(String.valueOf(videoRateProperty.get()));
	        	command.add("-start_number");
	        	command.add(String.valueOf(imageStartNumberProperty.get()));
	        	command.add("-i");
	        	command.add(imagePatternProperty.get());
	        	command.add("-s");
	        	command.add(videoResolutionWidthProperty.get() + "x" + videoResolutionHeightProperty.get());
	        	command.add("-vf");
	        	command.add("framerate=fps=30:interp_start=0:interp_end=255:scene=100");        	
	        	command.add("-vcodec");
	        	command.add("mpeg4");
	        	command.add("-q:v");
	        	command.add("1");
	        	command.add(videoFileNameProperty.get());
	
	        	commandProperty.set(command.toString());
	        	
	        	commandOutputTextArea.setText("");
	        	runButton.setDisable(true);
	        	runCommand(
					command,
					imageDirectoryProperty.get(),
					(output) -> commandOutputTextArea.appendText(output),
					(success) -> runButton.setDisable(false));
	        });
        }
        
        addTextArea(gridPane, rowIndex++, "Command", commandProperty, 1);
        {
	        gridPane.add(new Text("Command Output"), 0, rowIndex);
	
	        commandOutputTextArea = new TextArea();
	        commandOutputTextArea.setPrefRowCount(10);
	        commandOutputTextArea.setScrollTop(Double.MAX_VALUE);
	        gridPane.add(commandOutputTextArea, 1, rowIndex);
	        rowIndex++;
        }
        
        {
	        Button showButton = new Button("Show Video");
	        gridPane.add(showButton, 1, rowIndex++);
	        
	        showButton.addEventHandler(ActionEvent.ACTION, event -> {
	        	try {
					File videoFile = Paths.get(imageDirectoryProperty.get(), videoFileNameProperty.get()).toFile();
					Desktop.getDesktop().open(videoFile);
				} catch (Exception e) {
					e.printStackTrace();
				}
	        });        	
        }
        
        return gridPane;
	}

	private void updateImageDirectory() {
		Path directoryPath = Paths.get(imageDirectoryProperty.get());
		Integer lowestNumber = null;
		String filePattern = null;
		int imageCount = 0;
		
		try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(directoryPath, "*.jpg")) {
			for (Path filePath : directoryStream) {
				ImageFilenameParser parser = new ImageFilenameParser(filePath.getFileName().toString());
				if (parser.isValid()) {
					if (lowestNumber == null || parser.getNumber() < lowestNumber) {
						lowestNumber = parser.getNumber();
						filePattern = parser.getFilePattern();
					}
					if (parser.getFilePattern().equals(filePattern)) {
						imageCount++;
					}
				}
			}
			
			if (lowestNumber != null) {
				imagePatternProperty.set(filePattern);
				imageStartNumberProperty.set(lowestNumber);
				inputValidationMessage.set(imageCount + " images found.");
			}
		} catch (NotDirectoryException e) {
			inputValidationMessage.set("Not a directory: " + directoryPath);
		} catch (IOException e) {
			inputValidationMessage.set("Failed to read directory: " + e.getMessage());
		}
	}

	private void addTextField(GridPane gridPane, int rowIndex, String label, StringProperty stringProperty) {
        gridPane.add(new Text(label), 0, rowIndex);

        TextField textField = new TextField();
        Bindings.bindBidirectional(textField.textProperty(), stringProperty);
        gridPane.add(textField, 1, rowIndex);
	}

	private void addLabel(GridPane gridPane, int rowIndex, String label, StringProperty stringProperty) {
        gridPane.add(new Text(label), 0, rowIndex);

        Text text = new Text();
        Bindings.bindBidirectional(text.textProperty(), stringProperty);
        gridPane.add(text, 1, rowIndex);
	}

	private void addTextArea(GridPane gridPane, int rowIndex, String label, StringProperty stringProperty, int rowCount) {
        gridPane.add(new Text(label), 0, rowIndex);

        TextArea textArea = new TextArea();
        textArea.setPrefRowCount(rowCount);
        Bindings.bindBidirectional(textArea.textProperty(), stringProperty);
        gridPane.add(textArea, 1, rowIndex);
	}
	
	private <T> void addTextField(GridPane gridPane, int rowIndex, String label, Property<T> property, Format format) {
        gridPane.add(new Text(label), 0, rowIndex);

        TextField textField = new TextField();
        Bindings.bindBidirectional(textField.textProperty(), property, format);
        gridPane.add(textField, 1, rowIndex);
	}

	private void addDirectoryChooser(GridPane gridPane, int rowIndex, String label, StringProperty directoryProperty) {
        gridPane.add(new Text(label), 0, rowIndex);

        BorderPane borderPane = new BorderPane();
        gridPane.add(borderPane, 1, rowIndex);
        
        TextField textField = new TextField();
        borderPane.setCenter(textField);
        Bindings.bindBidirectional(textField.textProperty(), directoryProperty);        
        
        Button button = new Button("Dir...");
        borderPane.setRight(button);
        button.setOnAction(event -> {
            DirectoryChooser directoryChooser = new DirectoryChooser();
            File selectedDirectory = directoryChooser.showDialog(primaryStage);
             
            if(selectedDirectory != null){
            	directoryProperty.set(selectedDirectory.getAbsolutePath());
            }
        });
	}

	private void runCommand(List<String> command, String directory, Consumer<String> outputConsumer, Consumer<Boolean> finishedConsumer) {
		CommandExecutor commandExecutor = new CommandExecutor(command, directory, new CommandExecutorListener() {
			@Override
			public void addOutput(String output) {
				Platform.runLater(() -> {
					outputConsumer.accept(output);
				});
			}
			
			@Override
			public void addError(String error) {
				Platform.runLater(() -> {
					outputConsumer.accept(error);
				});
			}
			
			@Override
			public void finished() {
				finishedConsumer.accept(true);
			}
		});
		
		commandExecutor.runAsync();
	}
	
	public static void main(String[] args) {
		launch(args);
	}
}
