package hippo;
import java.io.*;

/**
 * Handles system input stream reading.
 */
public abstract class AbstractSystemInputReader {
	/**
	 * Reads system input stream line by line. All characters are converted to
	 * lower case and each line is passed for processing to
	 * {@link #processLine(String)} method.
	 * 
	 * @throws IOException	if an I/O error occurs
	 */
	public static boolean turnOutput=false, turnInput=false, logging=false;
	
	public void readConfig() {
		String configFileName="Hippo.cfg";
		try {
			BufferedReader in = new BufferedReader(new FileReader(configFileName));
			String line;
			while ((line = in.readLine()) != null) {
				processConfigLine(line);
			}
			in.close();
		} catch (IOException e) {
		}
	}

	private void processConfigLine(String line) {
		if (line.equals("turnOutput")) {turnOutput=true;}
		if (line.equals("turnInput")) {turnInput=true;}
		if (line.equals("logging")) {logging=true;}
	}
	
	public void readSystemInput() throws IOException {
		StringBuilder line = new StringBuilder();
		int c;
		while ((c = System.in.read()) >= 0) {
			if (c == '\r' || c == '\n') {
				processLine(line.toString().toLowerCase().trim());
				line.setLength(0);
			} else {
				line = line.append((char) c);
			}
		}
	}

	public boolean readTurnFileInput() {
		String turnFileName="c:\\Eclipse\\workspace\\ants\\Turns\\"+getTurnFileName();
		try {
			BufferedReader in = new BufferedReader(new FileReader(turnFileName));
			String line;
			while ((line = in.readLine()) != null) {
				processLine(line);
			}
			in.close();
			return true;
		} catch (IOException e) {
			return false;
		}
	}
	
	public abstract String getTurnFileName();

	/**
	 * Process a line read out by {@link #readSystemInput()} method in a way
	 * defined by subclass implementation.
	 * 
	 * @param line	single, trimmed line of system input
	 */
	public abstract void processLine(String line);
}
