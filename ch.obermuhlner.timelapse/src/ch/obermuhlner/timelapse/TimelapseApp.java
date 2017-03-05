package ch.obermuhlner.timelapse;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.Format;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.geometry.Orientation;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

public class TimelapseApp extends Application {

	private static final DecimalFormat INTEGER_FORMAT = new DecimalFormat("##0");

	private StringProperty imageDirectoryProperty = new SimpleStringProperty();
	private StringProperty imagePatternProperty = new SimpleStringProperty("IMG_%04d.JPG");
	private IntegerProperty imageStartNumberProperty = new SimpleIntegerProperty();
	private StringProperty outputDirectoryProperty = new SimpleStringProperty();
	private StringProperty videoFileNameProperty = new SimpleStringProperty("output.mp4");

	private StringProperty commandProperty = new SimpleStringProperty();
	private StringProperty commandOutputProperty = new SimpleStringProperty();

	private Stage primaryStage;
	
	@Override
	public void start(Stage primaryStage) throws Exception {
        this.primaryStage = primaryStage;
        
		Group root = new Group();
        Scene scene = new Scene(root);

        BorderPane mainBorderPane = new BorderPane();
        root.getChildren().add(mainBorderPane);
		
        mainBorderPane.setTop(createToolbar());
        
        mainBorderPane.setLeft(createEditor());
                
		primaryStage.setScene(scene);
        primaryStage.show();
	}

	private Node createToolbar() {
        FlowPane toolbarFlowPane = new FlowPane(Orientation.HORIZONTAL);
        toolbarFlowPane.setHgap(4);
        toolbarFlowPane.setVgap(4);
    
        Button runButton = new Button("Run");
        toolbarFlowPane.getChildren().add(runButton);
        runButton.addEventHandler(ActionEvent.ACTION, event -> {
        	List<String> command = new ArrayList<>();
        	command.add("ffmpeg");
        	command.add("-r");
        	command.add("1");
        	command.add("-start_number");
        	command.add(String.valueOf(imageStartNumberProperty.get()));
        	command.add("-i");
        	String input = "";
        	if (imageDirectoryProperty.get() != null && !imageDirectoryProperty.get().isEmpty()) {
        		input = imageDirectoryProperty.get() + "/";
        	}
        	input += imagePatternProperty.get();
        	command.add(input);
        	command.add("-s");
        	command.add("hd1080");
        	command.add("-vf");
        	command.add("framerate=fps=30:interp_start=0:interp_end=255:scene=100");        	
        	command.add("-vcodec");
        	command.add("mpeg4");
        	command.add("-q:v");
        	command.add("1");
        	command.add(videoFileNameProperty.get());

        	commandProperty.set(command.toString());
        	runCommand(command, commandOutputProperty);
        });
        
        return toolbarFlowPane;
	}
	
	private Node createEditor() {
        GridPane gridPane = new GridPane();
        gridPane.setHgap(2);
        gridPane.setVgap(2);
        
        int rowIndex = 0;
        addDirectoryChooser(gridPane, rowIndex++, "Image Directory", imageDirectoryProperty);
        addTextField(gridPane, rowIndex++, "Image Pattern", imagePatternProperty);
        addTextField(gridPane, rowIndex++, "Image Start Number", imageStartNumberProperty, INTEGER_FORMAT);
        addDirectoryChooser(gridPane, rowIndex++, "Output Directory", outputDirectoryProperty);
        addTextField(gridPane, rowIndex++, "Output Video File", videoFileNameProperty);

        addTextArea(gridPane, rowIndex++, "Command", commandProperty, 1);
        addTextArea(gridPane, rowIndex++, "Command Output", commandOutputProperty, 10);

        imageDirectoryProperty.addListener(changeEvent -> {
        	
        });
        
		return gridPane;
	}

	private void addTextField(GridPane gridPane, int rowIndex, String label, StringProperty stringProperty) {
        gridPane.add(new Text(label), 0, rowIndex);

        TextField textField = new TextField();
        Bindings.bindBidirectional(textField.textProperty(), stringProperty);
        gridPane.add(textField, 1, rowIndex);
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

	private void runCommand(List<String> command, StringProperty outputProperty) {
		new Thread() {
			@Override
			public void run() {
				runCommandInternal(command, outputProperty);
			}
		}.start();
	}
	
	private void runCommandInternal(List<String> command, StringProperty outputProperty) {
		outputProperty.set("");

		ProcessBuilder processBuilder = new ProcessBuilder(command);
		
		try {
			Process process = processBuilder.start();
			
			BufferedReader output = new BufferedReader(new InputStreamReader(process.getInputStream()));
			BufferedReader error = new BufferedReader(new InputStreamReader(process.getErrorStream()));
			while (process.isAlive()) {
				String line = error.readLine();
				if (line != null) {
					append(outputProperty, line + "\n");
				}

				line = output.readLine();
				if (line != null) {
					append(outputProperty, line + "\n");
				}
				
				Thread.sleep(1);
			}
			
			int resultCode = process.waitFor();
			
			String line = error.readLine();
			while (line != null) {
				append(outputProperty, line + "\n");
				
				line = error.readLine();
			}
			
			while (line != null) {
				append(outputProperty, line + "\n");
				
				line = output.readLine();
			}
			
			System.out.println(resultCode);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	private void append(StringProperty property, String string) {
		property.set(property.get() + string);
	}

	public static void main(String[] args) {
		launch(args);
	}
}
