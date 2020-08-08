package hippo;
import java.util.*;

public class TileList {
	// list allowing inserts till the start of first traversal, it's size is limited by maxsize ... usually rows*cols
	// than tiles could be only removed from the list till final clear starting another round.
	// actually structure allows inserts even during the traversal or after it, but this will not be used.
	// there could be filter function, but I don't know how to implement it in java so only Ilk set and HillType set filtering at first
	// during traversal Tiles which does not match the filter function are removed from the List.
	// I could make my own iterator, but let us do it manually
	private static Ants ants;
	public static void init(Ants ants_) {
		ants=ants_;
	}
	
	private Tile[] list;
	private int insertTo;
	private int currentIter=0;
	private int shrinkTo=0;
	private Set<Ilk> filterIlk;
	private Set<HillTypes> filterHillTypes;
	
	public TileList(Set<Ilk> fIlk, Set<HillTypes> fHillTypes) {
		list=new Tile[ants.getRows()*ants.getCols()];
		insertTo=0;
		filterIlk=fIlk;
		filterHillTypes=fHillTypes;
	}
	
	public TileList(int maxsize, Set<Ilk> fIlk, Set<HillTypes> fHillTypes) {
		list=new Tile[maxsize];
		insertTo=0;
		filterIlk=fIlk;
		filterHillTypes=fHillTypes;
	}
	
	public int size() {
		return insertTo;
	}
	
	public void clear() {
		currentIter=shrinkTo=insertTo=0;
	}
	
	public void add(Tile tile) {
		list[insertTo]=tile;
		insertTo++;
	}
	
	public void startTraversal() {
		currentIter=shrinkTo=0;
	}
	public Tile getFirstFilter() {
		startTraversal();
		return getNextFilter();
	}

	public Tile getFirstNoFilter() {
		startTraversal();
		return getNextNoFilter();
	}

	private boolean filter(Tile tile) {
		boolean result=true;
		if (filterIlk!=null) {
			result = result && filterIlk.contains(ants.getIlk(tile));
		}
		if (filterHillTypes!=null) {
			result = result && filterHillTypes.contains(ants.getHill(tile));
		}
		return result;
	}
	
	// returns null at the end of traversal
	public Tile getNextNoFilter() {
		Tile ret;
		if (currentIter==insertTo) {
			insertTo=shrinkTo;
			return null;
		}
		ret=list[shrinkTo]=list[currentIter];
		shrinkTo++;currentIter++;
		return ret;
	}

	// returns null at the end of traversal
	public Tile getNextFilter() {
		Tile ret;
		while ((currentIter<insertTo)&&(!filter(list[currentIter]))) {
			currentIter++;
		}
		if (currentIter==insertTo) {
			insertTo=shrinkTo;
			return null;
		}
		ret=list[shrinkTo]=list[currentIter];
		shrinkTo++;currentIter++;
		return ret;
	}
	
	public String toString() {// for debugging
		StringBuilder res=new StringBuilder();
		boolean empty=true;
		for(int i=0;i<insertTo;i++) {
			res.append((empty?"":",")+(i==currentIter?">":"")+"("+list[i].getRow()+","+list[i].getCol()+")");
			empty=false;
			if (!filter(list[i])) {
				res.append('*');
			}
		}
		return res.toString();
	}
}
