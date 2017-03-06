package ch.obermuhlner.timelapse;

public class ImageFilenameParser {

	private String numberPart;
	
	private String filePattern;

	public ImageFilenameParser(String filename) {
		StringBuilder numberPartBuilder = new StringBuilder();
		StringBuilder filePatternBuilder = new StringBuilder();
		
		for(char c : filename.toCharArray()) {
			if (Character.isDigit(c)) {
				numberPartBuilder.append(String.valueOf(c));
			} else {
				if (numberPart == null && numberPartBuilder.length() > 0) {
					numberPart = numberPartBuilder.toString();
					filePatternBuilder.append("%0");
					filePatternBuilder.append(numberPart.length());
					filePatternBuilder.append("d");
				}
				numberPartBuilder.setLength(0);
				
				filePatternBuilder.append(String.valueOf(c));
			}
		}
		
		filePattern = filePatternBuilder.toString();
	}
	
	public boolean isValid() {
		return numberPart != null;
	}
	
	public String getNumberPart() {
		return numberPart;
	}
	
	public int getNumber() {
		return Integer.parseInt(numberPart);
	}
	
	public String getFilePattern() {
		return filePattern;
	}
}
