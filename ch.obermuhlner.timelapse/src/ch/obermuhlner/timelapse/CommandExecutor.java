package ch.obermuhlner.timelapse;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

public class CommandExecutor {

	private List<String> command;
	private String directory;
	private CommandExecutorListener listener;

	public CommandExecutor(List<String> command, String directory, CommandExecutorListener listener) {
		this.command = command;
		this.directory = directory;
		this.listener = listener;
	}
	
	public void runAsync() {
		new Thread(() -> run()).start(); 
	}

	public void run() {
		ProcessBuilder processBuilder = new ProcessBuilder(command);
		
		processBuilder.directory(new File(directory));
		
		try {
			Process process = processBuilder.start();
			
			BufferedReader output = new BufferedReader(new InputStreamReader(process.getInputStream()));
			BufferedReader error = new BufferedReader(new InputStreamReader(process.getErrorStream()));
			String line = null;

			while (process.isAlive()) {
				if (error.ready()) {
					line = error.readLine();
					if (line != null) {
						listener.addError(line + "\n");
					}
				}

				if (output.ready()) {
					line = output.readLine();
					if (line != null) {
						listener.addOutput(line + "\n");
					}
				}
				
				Thread.sleep(1);
			}
			
			process.waitFor();
			
			if (error.ready()) {
				line = error.readLine();
				while (line != null) {
					listener.addOutput(line + "\n");
					
					line = error.readLine();
				}
			}
			
			if (output.ready()) {
				while (line != null) {
					listener.addError(line + "\n");
					
					line = output.readLine();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		listener.finished();
	}

	public static interface CommandExecutorListener {
		void addOutput(String output);
		void addError(String error);
		
		void finished();
	}
}
