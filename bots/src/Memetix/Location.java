package Memetix;
public class Location implements Comparable<Location> {
	private int		row;
	private int		col;
	private	int		lastSeen;	// turn number this location was last seen - 0 = never
	private int		lastEmpty;	// turn number this location was last unoccupied by an ant
	private	Ilk		ilk;		// Water, Food, Land, Unknown
	private boolean	isHill;		//True if occupied by a hill
	private boolean isAnt;		//True if occupied by an ant
	private int		owner;		// 0 if hill/ant is ours, >0 for enemy
	private double	score;		//How valuable this tile is
	private double	rand;		//Random number assigned to determining result if scores are equal
	
	public Location(int row, int col) {
		this.row = row;
		this.col = col;
		this.ilk = Ilk.UNKNOWN;
		this.lastSeen = 0;
		this.lastEmpty = 0;
		this.isHill = false;
		this.isAnt = false;
		this.owner = -1;
		this.score = 0;
		this.rand = 0;
	}
	
	public Tile getTile() {
		return new Tile(row, col);
	}
	
	public Ilk getIlk() {
		return this.ilk;
	}
	
	public boolean expired(int turn) {
		return (this.ilk.isPassable() && this.lastSeen > 0 && turn - this.lastSeen > 3);
	}
	
	public int occupiedFor(int turn) {
		if (isAnt && lastSeen == turn)
			return turn - lastEmpty;
		return 0;
	}
	
	public void setIlk(Ilk ilk, int turn) {
		this.lastSeen = turn;
		this.lastEmpty = turn;
		this.ilk = ilk;
		this.isHill = false;
		this.isAnt = false;		
		this.owner = 0;
	}
	
	public void clearIlk(int turn) {
		this.lastSeen = turn;
		this.lastEmpty = turn;
		this.isHill = false;
		this.isAnt = false;
		this.owner = 0;
		if (this.ilk != Ilk.WATER)
			this.ilk = Ilk.LAND;
	}
	
	public int getHill() {
		if (this.isHill)
			return this.owner;
		return -1;
	}
	
	public void setHill(int owner, int turn) {
		this.owner = owner;
		this.isHill = true;
		if (this.lastSeen < turn) {
			this.lastSeen = turn;
			this.isAnt = false;
			this.lastEmpty = turn;
		}
		this.ilk = Ilk.LAND;
	}
	
	public int getAnt(int turn) {
		if (this.lastSeen == turn && this.isAnt)
			return this.owner;
		return -1;
	}
	
	public void setAnt(int owner, int turn) {
		this.owner = owner;
		this.isAnt = (owner >= 0);
		this.ilk = Ilk.LAND;
		this.lastSeen = turn;
	}
	
	/*
	 * Used to sort the list of our ants in score order - highest first
	 */
	public int compareTo(Location l) {
		//Check for equals - tie breaks are broken using the cached random values for each location
		if (this.score == l.score) {
			if (this.rand == l.rand)
				return 0;
			if (this.rand > l.rand)
				return -1;
			return 1;
		}
		
		if (this.score > l.score)
			return -1;
		return 1;
	}
	
	public double getScore() {
		return this.score;
	}
	
	public void setRand(double rand) {
		this.rand = rand;
	}
	
	public double getRand() {
		return this.rand;
	}
	
	public void setScore(double score) {
		this.score = score;
	}
	
	public void addScore(double score) {
		this.score += score;
	}
	
	public int lastSeen() {
		return this.lastSeen;
	}
}
