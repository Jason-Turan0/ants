package SpeedyBot;
import java.io.FileNotFoundException;
import java.io.PrintStream;

public class Logger {

	private static PrintStream logStream;
	private static boolean activated = false;
	private static String logSuffix = "";

	public static void printLine(String line) {
		if (Logger.activated) {
			if (Logger.logStream == null) {
				try {
					Logger.logStream = new PrintStream("log" + Logger.logSuffix
							+ ".txt");
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
			}
			logStream.println(line);
		}
	}

	public static void printException(Exception e) {
		if (Logger.activated)
			e.printStackTrace(Logger.logStream);
	}

	public static boolean isActivated() {
		return Logger.activated;
	}

	public static void setActivated(boolean a) {
		Logger.activated = a;
	}

	public static void setLogSuffix(String s) {
		Logger.logSuffix = s;
	}
}
