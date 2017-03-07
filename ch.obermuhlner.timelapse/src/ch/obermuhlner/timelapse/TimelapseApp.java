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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TabPane.TabClosingPolicy;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

public class TimelapseApp extends Application {

	private static final int GRID_GAP = 4;

	private static final DecimalFormat INTEGER_FORMAT = new DecimalFormat("##0");
	
	private static final Pattern RESOLUTION_PATTERN = Pattern.compile("([0-9]+)x([0-9]+)");

	private StringProperty imageDirectoryProperty = new SimpleStringProperty();
	private StringProperty imagePatternProperty = new SimpleStringProperty();
	private IntegerProperty imageStartNumberProperty = new SimpleIntegerProperty();
	private StringProperty videoFileNameProperty = new SimpleStringProperty("output.mp4");
	private IntegerProperty videoRateProperty = new SimpleIntegerProperty(1);
	private StringProperty videoResolutionProperty = new SimpleStringProperty();
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
        gridPane.setHgap(GRID_GAP);
        gridPane.setVgap(GRID_GAP);
        
        int rowIndex = 0;
        addDirectoryChooser(gridPane, rowIndex++, "Image Directory", imageDirectoryProperty);
        addTextField(gridPane, rowIndex++, "Image Pattern", imagePatternProperty);
        addTextField(gridPane, rowIndex++, "Image Start Number", imageStartNumberProperty, INTEGER_FORMAT);
        TextArea infoTextArea = addTextArea(gridPane, rowIndex++, "Input Info", inputValidationMessage, 1);
        infoTextArea.setEditable(false);

        imageDirectoryProperty.addListener(changeEvent -> {
        	updateImageDirectory();
        });
        
		return gridPane;
	}

	private Node createFilterTab() {
        GridPane gridPane = new GridPane();
        gridPane.setHgap(GRID_GAP);
        gridPane.setVgap(GRID_GAP);
        
        int rowIndex = 0;

        
        return gridPane;
	}

	private Node createOutputTab() {
        GridPane gridPane = new GridPane();
        gridPane.setHgap(GRID_GAP);
        gridPane.setVgap(GRID_GAP);
        
        int rowIndex = 0;

        addTextField(gridPane, rowIndex++, "Output Video File", videoFileNameProperty);
        addTextField(gridPane, rowIndex++, "Frame Rate", videoRateProperty, INTEGER_FORMAT);
        addRadioToggleGroup(gridPane, rowIndex++, "Video Resolution", videoResolutionProperty, 
        		"Full HD (1920x1080)",
        		"HD (1366x768)",
        		"Quad HD (2560x1440)",
        		"4K Ultra HD (3840x2160)",
        		"WXGA (1280x720)",
        		"XGA (1024x768)", 
        		"SVGA (800x600)", 
        		"VGA (640x480)", 
        		"Custom");
        TextField widthTextField = addTextField(gridPane, rowIndex++, "Video Width", videoResolutionWidthProperty, INTEGER_FORMAT);
        TextField heightTextField = addTextField(gridPane, rowIndex++, "Video Height", videoResolutionHeightProperty, INTEGER_FORMAT);
        
        updateVideoResolution(widthTextField, heightTextField);
        videoResolutionProperty.addListener((observable, oldValue, newValue) -> {
        	updateVideoResolution(widthTextField, heightTextField);
        });

        return gridPane;
	}

	private void updateVideoResolution(TextField widthTextField, TextField heightTextField) {
		String resolution = videoResolutionProperty.get();
		
		if ("Custom".equals(resolution)) {
			widthTextField.setDisable(false);
			heightTextField.setDisable(false);
		} else {
			Matcher matcher = RESOLUTION_PATTERN.matcher(resolution);
			if (matcher.find()) {
				videoResolutionWidthProperty.set(Integer.parseInt(matcher.group(1)));
				videoResolutionHeightProperty.set(Integer.parseInt(matcher.group(2)));
			}
			
			widthTextField.setDisable(true);
			heightTextField.setDisable(true);
		}
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
        
        TextArea commandTextArea = addTextArea(gridPane, rowIndex++, "Command", commandProperty, 1);
        commandTextArea.setEditable(false);
        
        commandOutputTextArea = addTextArea(gridPane, rowIndex++, "Command Output", null, 1);
        commandOutputTextArea.setEditable(false);
        commandOutputTextArea.setPrefRowCount(10);
        commandOutputTextArea.setScrollTop(Double.MAX_VALUE);
        
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

	private TextArea addTextArea(GridPane gridPane, int rowIndex, String label, StringProperty stringProperty, int rowCount) {
        addTopLabel(gridPane, rowIndex, label);

        TextArea textArea = new TextArea();
        textArea.setPrefRowCount(rowCount);
        if (stringProperty != null) {
        	Bindings.bindBidirectional(textArea.textProperty(), stringProperty);
        }
        gridPane.add(textArea, 1, rowIndex);
        
        return textArea;
	}

	private <T> ComboBox<T> addComboBox(GridPane gridPane, int rowIndex, String label, Property<T> property, @SuppressWarnings("unchecked") T... values) {
        gridPane.add(new Text(label), 0, rowIndex);

        ComboBox<T> comboBox = new ComboBox<>();
        comboBox.getItems().addAll(values);
        Bindings.bindBidirectional(comboBox.valueProperty(), property);
        comboBox.valueProperty().set(values[0]);
        
        gridPane.add(comboBox, 1, rowIndex);

        comboBox.setOnMouseEntered(event -> {
            comboBox.requestFocus();
        });
        
        return comboBox;
	}

	private void addRadioToggleGroup(GridPane gridPane, int rowIndex, String label, StringProperty property, String... values) {
		addTopLabel(gridPane, rowIndex, label);
        
        ToggleGroup toggleGroup = new ToggleGroup();
        VBox box = new VBox();
        
		for (String value : values) {
			RadioButton button = new RadioButton(value);
			box.getChildren().add(button);
			button.setToggleGroup(toggleGroup);
			button.setSelected(value.equals(values[0]));
			button.setOnAction(event -> {
				property.set(value);
			});
		}
		
		property.set(values[0]);
        
        gridPane.add(box, 1, rowIndex);

	}
	
	private <T> TextField addTextField(GridPane gridPane, int rowIndex, String label, Property<T> property, Format format) {
        gridPane.add(new Text(label), 0, rowIndex);

        TextField textField = new TextField();
        Bindings.bindBidirectional(textField.textProperty(), property, format);
        gridPane.add(textField, 1, rowIndex);
        
        return textField;
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

	private void addTopLabel(GridPane gridPane, int rowIndex, String label) {
		Text labelText = new Text(label);
		gridPane.add(labelText, 0, rowIndex);
		GridPane.setMargin(labelText, new Insets(4));
		GridPane.setValignment(labelText, VPos.TOP);
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
