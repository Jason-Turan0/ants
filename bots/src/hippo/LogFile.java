package hippo;
import java.util.*;
import java.io.*;

// Handles logging data to disk

public class LogFile {

	private static String log_file_name = "game.log";
	private static List<String> buffer = new ArrayList<String>();
	// Never prints to screen
	public static void writeBuffered(String text) {
		if (AbstractSystemInputReader.logging) {
			//write(log_file_name, text);
			buffer.add(text);
		}
	}

	public static void write(String text) {
		if (AbstractSystemInputReader.logging) {
			write(log_file_name, text);
			//buffer.add(text);
		}
	}

	public static void writeFlush(String text) {
		if (AbstractSystemInputReader.logging) {
			//write(log_file_name, text);
			write(log_file_name, buffer, text);
			buffer.clear();
		}
	}
	
	public static void write(List<String> textRows, String text) {
		if (AbstractSystemInputReader.logging) {
			write(log_file_name, textRows, text);
		}
	}

	// writes data to disk
	public static void write(String log_file_name, String text) {
		try {
			// Open the file for append
			FileWriter fw = new FileWriter(log_file_name, true);
			fw.write(text + System.getProperty("line.separator"));

			// Close the file
			fw.flush();
			fw.close();

		} catch (FileNotFoundException ex) {
			System.err.println("LogFile: File not found: " + log_file_name);
		} catch (Exception e) {
			System.err.println("LogFile: Unknown error!");
		}
	}

	public static void write(String log_file_name, List<String> textRows, String text) {
		try {
			// Open the file for append
			FileWriter fw = new FileWriter(log_file_name, true);
			for(String txt:textRows) {
				fw.write(txt + System.getProperty("line.separator"));
			}
			fw.write(text + System.getProperty("line.separator"));
			// Close the file
			fw.flush();
			fw.close();

		} catch (FileNotFoundException ex) {
			System.err.println("LogFile: File not found: " + log_file_name);
		} catch (Exception e) {
			System.err.println("LogFile: Unknown error!");
		}
	}
}