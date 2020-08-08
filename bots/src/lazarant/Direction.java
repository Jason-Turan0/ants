package lazarant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a direction in which to move an ant.
 */
public enum Direction {
    /** North direction, or up. */
    NORTH(-1, 0, 'n'),
    
    /** East direction or right. */
    EAST(0, 1, 'e'),
    
    /** South direction or down. */
    SOUTH(1, 0, 's'),
    
    /** West direction or left. */
    WEST(0, -1, 'w'),

    /** Stay */
    NONE(0, 0, '?');

    public static final Direction[] values = {NORTH, EAST, SOUTH, WEST};
    
    public static final Direction[] evalues = {NORTH, EAST, SOUTH, WEST, NONE};

    public static final List<Direction> ONLY_NONE = Collections.singletonList(Direction.NONE);

    private static final Map<Character, Direction> symbolLookup = new HashMap<Character, Direction>();
    
    static {
        symbolLookup.put('n', NORTH);
        symbolLookup.put('e', EAST);
        symbolLookup.put('s', SOUTH);
        symbolLookup.put('w', WEST);
    }
    
    final int rowDelta;
    
    final int colDelta;
    
    final char symbol;
    
    Direction(int rowDelta, int colDelta, char symbol) {
        this.rowDelta = rowDelta;
        this.colDelta = colDelta;
        this.symbol = symbol;
    }

    /**
     * Returns direction associated with specified symbol.
     * 
     * @param symbol <code>n</code>, <code>e</code>, <code>s</code> or <code>w</code> character
     * 
     * @return direction associated with specified symbol
     */
    public static Direction fromSymbol(char symbol) {
        return symbolLookup.get(symbol);
    }
}
