package hippo;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a direction in which to move an ant.
 */
public enum Aim {
	NORTH(-1, 0, 'n'),	/** North direction, or up. */
	EAST(0, 1, 'e'),	/** East direction or right. */
	SOUTH(1, 0, 's'),	/** South direction or down. */
	WEST(0, -1, 'w'),	/** West direction or left. */
	NONE(0,0,'x');		/** Stay still. */

	private static final Map<Character, Aim> symbolLookup = new HashMap<Character, Aim>();

	public static Aim[][] permAim=new Aim[120][5];
	
	static {
		symbolLookup.put('n', NORTH);
		symbolLookup.put('e', EAST);
		symbolLookup.put('s', SOUTH);
		symbolLookup.put('w', WEST);
	}

	private final int rowDelta;
	private final int colDelta;
	private final char symbol;

	Aim(int rowDelta, int colDelta, char symbol) {
		this.rowDelta = rowDelta;
		this.colDelta = colDelta;
		this.symbol = symbol;
	}

	/**
	 * Returns rows delta.
	 * 
	 * @return rows delta.
	 */
	public int getRowDelta() {
		return rowDelta;
	}

	public Aim back() {
		switch (this) {
		case NORTH: return SOUTH;
		case EAST: return WEST;
		case SOUTH: return NORTH;
		case WEST: return EAST;
		}
		return NONE;
	}
	/**
	 * Returns columns delta.
	 * 
	 * @return columns delta.
	 */
	public int getColDelta() {
		return colDelta;
	}

	/**
	 * Returns symbol associated with this direction.
	 * 
	 * @return symbol associated with this direction.
	 */
	public char getSymbol() {
		return symbol;
	}

	/**
	 * Returns direction associated with specified symbol.
	 * 
	 * @param symbol
	 * <code>n</code>, <code>e</code>, <code>s</code> or <code>w</code> character
	 * 
	 * @return direction associated with specified symbol
	 */
	public static Aim fromSymbol(char symbol) {
		return symbolLookup.get(symbol);
	}
	
	public static void init() {
		permAim[0][0]=Aim.NORTH;permAim[0][1]=Aim.EAST;permAim[0][2]=Aim.SOUTH;permAim[0][3]=Aim.WEST;permAim[0][4]=Aim.NONE;
		int prevFact=1;
		for(int thisOrd=2;thisOrd<6;thisOrd++) {
			for(int i=prevFact;i<prevFact*thisOrd;i++) {
				int j=i/prevFact-1,m=i%prevFact;
				for(int k=0;k<5;k++) permAim[i][k]=permAim[m][k];
				permAim[i][thisOrd-1]=permAim[m][j];
				permAim[i][j]=permAim[m][thisOrd-1];
			}
			prevFact*=thisOrd;
		}
	}
}
