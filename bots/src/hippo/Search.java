package hippo;
import java.util.Arrays;

public class Search {
	private static Ants ants;
	private static int circle;
	public static final Tile breakTile = new Tile(-1,-1); 
	// Queue have to contain at most one break tile, breakTile is added before remove so remove could expect there is exactly one breakTile there 

	private Tile[] tileQueue;
	private int[][] visitedHash;
	private int hashGeneration=0;
	private int normalIn=0,normalOut=0,delayedIn=0,delayedOut=0; 
	// delayed decrements, normal increments
	// delayed decrements first than writes, normal writes than increments
	// after break is added delayedIn=normalOut 
	
	private static int indexDec(int index) {
		if (index==0) {
			index=circle;
		}
		index--; 
		return index;
	} 
	
	private static int indexInc(int index) {
		index++; 
		if (index>=circle) return 0;
		return index;
	} 
	
	public static void init(Ants ants_) {
		ants=ants_;
		circle = ants.getRows()*ants.getCols()+1;
	}
	
	public Search() {
		tileQueue = new Tile[circle];
		visitedHash = new int[ants.getRows()][ants.getCols()];
		for(int[] row:visitedHash) {
			Arrays.fill(row, 0);
		}		
	}
	
	public void clear() {
		delayedIn=delayedOut=normalIn=normalOut=0;
		hashGeneration++;
	}

	public boolean isEmpty() {
		return ((normalIn==normalOut)&&(delayedIn==delayedOut));
	}
	
	private void add(Tile tile) {
		tileQueue[normalIn]=tile;
		normalIn=indexInc(normalIn);
	}
	
	private void addDelayed(Tile tile) {
		delayedIn=indexDec(delayedIn);
		tileQueue[delayedIn]=tile;
	}
	
	public void addBreak() {
		add(breakTile);
		while (delayedIn!=delayedOut) {
			Tile tmp=tileQueue[delayedIn];
			delayedIn=indexInc(delayedIn);
			add(tmp);
		}
		delayedIn=delayedOut=normalOut;
	}
	
	public boolean visited(Tile tile) {
		return visitedHash[tile.getRow()][tile.getCol()]==hashGeneration;
	}
	
	public boolean notVisited(Tile tile) {
		return visitedHash[tile.getRow()][tile.getCol()]!=hashGeneration;
	}
	
	public void addNotVisited(Tile tile) {
		if (visitedHash[tile.getRow()][tile.getCol()]!=hashGeneration) {
			visitedHash[tile.getRow()][tile.getCol()]=hashGeneration;
			add(tile);
		}
	}
	
	public void addNotVisitedDelayed(Tile tile) {
		if (visitedHash[tile.getRow()][tile.getCol()]!=hashGeneration) {
			visitedHash[tile.getRow()][tile.getCol()]=hashGeneration;
			addDelayed(tile);
		}
	}
	
	public void visit(Tile tile) {
		visitedHash[tile.getRow()][tile.getCol()]=hashGeneration;
	}
	public void unVisit(Tile tile) {
		visitedHash[tile.getRow()][tile.getCol()]=hashGeneration-1;
	}
	public Tile remove() {// there is exactly one breakTile in the Queue
		// due to implicit delete this is a bit more complicated
		boolean OK=false;Tile tmp=null;
		while (!OK) {
			tmp=tileQueue[normalOut];
			normalOut=indexInc(normalOut);
			if (tmp.equals(breakTile)) {
				OK=true;
			} else if (!notVisited(tmp)) {
				OK=true;
			}
		}
		return tmp;
	}
	public Tile removeNoFilter() {// there is exactly one breakTile in the Queue
		// due to implicit delete this is a bit more complicated
		boolean OK=false;Tile tmp=null;
		tmp=tileQueue[normalOut];
		normalOut=indexInc(normalOut);
		return tmp;
	}
	
	public String toString() {
		StringBuilder ret=new StringBuilder();
		for (int i=normalOut;i<normalIn;i++) {
			Tile t=tileQueue[i];
			ret.append("("+t.getRow()+","+t.getCol()+")");
		}
		return ret.toString();
	}
} 
