package lazarant;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

/**
 * Provides basic game state handling.
 */
public abstract class Bot {
    private static final String READY = "ready";
    private static final String GO = "go";
    private static final char COMMENT_CHAR = '#';
    Ants ants;
    protected List<Order> orders = new ArrayList<Order>();

    public List<Order> getOrders() {
        return orders;
    }

    public void clearOrders() {
        orders.clear();
    }

    private final List<String> input = new ArrayList<String>();

    private static Pattern compilePattern(Class<? extends Enum> clazz) {
        StringBuilder builder = new StringBuilder("(");
        for (Enum enumConstant : clazz.getEnumConstants()) {
            if (enumConstant.ordinal() > 0) {
                builder.append("|");
            }
            builder.append(enumConstant.name());
        }
        builder.append(")");
        return Pattern.compile(builder.toString());
    }

    /**
     * Reads system input stream line by line. All characters are converted to lower case and each
     * line is passed for processing to {@link #processLine(String)} method.
     *
     * @throws java.io.IOException if an I/O error occurs
     */
    public void startGame() throws IOException {
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

    /**
     * Collects lines read from system input stream until a keyword appears and then parses them.
     */
    public void processLine(String line) {
        if (line.equals(READY)) {
            parseSetup(input);
            initialize();
            //doTurn();
            finishTurn();
            input.clear();
        } else if (line.equals(GO)) {
            parseUpdate(input);
            doTurn();
            ants.afterTurn();
            finishTurn();
            input.clear();
        } else if (!line.isEmpty()) {
            input.add(line);
        }
    }

    protected abstract void initialize();

    /**
     * Parses the setup information from system input stream.
     *
     * @param input setup information
     */
    public void parseSetup(List<String> input) {
        int loadTime = 0;
        int turnTime = 0;
        int rows = 0;
        int cols = 0;
        int turns = 0;
        int viewRadius2 = 0;
        int attackRadius2 = 0;
        int spawnRadius2 = 0;
        for (String line : input) {
            line = removeComment(line);
            if (line.isEmpty()) {
                continue;
            }
            MyScanner scanner = new MyScanner(line);
            if (!scanner.hasNext()) {
                continue;
            }
            String token = scanner.next().toUpperCase();
            if (!SetupToken.PATTERN.matcher(token).matches()) {
                continue;
            }
            SetupToken setupToken = SetupToken.valueOf(token);
            switch (setupToken) {
                case LOADTIME:
                    loadTime = scanner.nextInt();
                    break;
                case TURNTIME:
                    turnTime = scanner.nextInt();
                    break;
                case ROWS:
                    rows = scanner.nextInt();
                    break;
                case COLS:
                    cols = scanner.nextInt();
                    break;
                case TURNS:
                    turns = scanner.nextInt();
                    break;
                case VIEWRADIUS2:
                    viewRadius2 = scanner.nextInt();
                    break;
                case ATTACKRADIUS2:
                    attackRadius2 = scanner.nextInt();
                    break;
                case SPAWNRADIUS2:
                    spawnRadius2 = scanner.nextInt();
                    break;
            }
        }
        this.ants = new Ants(loadTime, turnTime, rows, cols, turns, viewRadius2, attackRadius2,
                spawnRadius2);
    }

    /**
     * Parses the update information from system input stream.
     *
     * @param input update information
     */
    public void parseUpdate(List<String> input) {
        ants.beforeUpdate();
        for (String line : input) {
            line = removeComment(line);
            if (line.isEmpty()) {
                continue;
            }
            MyScanner scanner = new MyScanner(line);
            if (!scanner.hasNext()) {
                continue;
            }
            String token = scanner.next().toUpperCase();
            if (!UpdateToken.PATTERN.matcher(token).matches()) {
                continue;
            }
            UpdateToken updateToken = UpdateToken.valueOf(token);
            int row = scanner.nextInt();
            int col = scanner.nextInt();
            switch (updateToken) {
                case W:
                    ants.addWater(row, col);
                    break;
                case A:
                    if (scanner.hasNextInt()) {
                        ants.addAnt(row, col, scanner.nextInt());
                    }
                    break;
                case F:
                    ants.addFood(row, col);
                    break;
                case D:
                    if (scanner.hasNextInt()) {
                        ants.addDeadAnt(row, col, scanner.nextInt());
                    }
                    break;
                case H:
                    if (scanner.hasNextInt()) {
                        ants.addHill(row, col, scanner.nextInt());
                    }
                    break;
            }
        }
        ants.afterUpdate();
    }

    /**
     * Subclasses are supposed to use this method to process the game state and send orders.
     */
    public abstract void doTurn();

    /**
     * Finishes turn.
     */
    public void finishTurn() {
        //System.out.println("go");
        //System.out.flush();
    }

    private String removeComment(String line) {
        int commentCharIndex = line.indexOf(COMMENT_CHAR);
        String lineWithoutComment;
        if (commentCharIndex >= 0) {
            lineWithoutComment = line.substring(0, commentCharIndex).trim();
        } else {
            lineWithoutComment = line;
        }
        return lineWithoutComment;
    }

    private enum SetupToken {
        LOADTIME, TURNTIME, ROWS, COLS, TURNS, VIEWRADIUS2, ATTACKRADIUS2, SPAWNRADIUS2;

        private static final Pattern PATTERN = compilePattern(SetupToken.class);
    }

    private enum UpdateToken {
        W, A, F, D, H;

        private static final Pattern PATTERN = compilePattern(UpdateToken.class);
    }

    public static class MyScanner extends StringTokenizer {

        public MyScanner(String line) {
            super(line);
        }

        public boolean hasNext() {
            return (hasMoreTokens());
        }

        public boolean hasNextInt() {
            return (hasMoreTokens());
        }

        public String next() {
            return (nextToken());
        }

        public int nextInt() {
            return (Integer.valueOf(next()));
        }
    }
}
