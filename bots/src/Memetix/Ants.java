package Memetix;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Holds all game data and current game state.
 */
public class Ants {
	private boolean write_debug	= false; //Set to true to output debug to System.err
	/** Maximum map size. */
    public static final int MAX_MAP_SIZE = 256 * 2;
    private static final int ORDER_TIME = 5; //microseconds reserved to complete output of orders

    private final int turnTime;
    private final int rows;
    private final int cols;
    private final int turns;
    private final int loadTime;
    private final int viewRadius2;
    private final int attackRadius2;
    private final int spawnRadius2;
    
    private final Set<Tile> vision_offsets;
    private final Set<Tile> combat_offsets;
    private final Set<Tile> northArc;
    private final Set<Tile> eastArc;
    private final Set<Tile> southArc;
    private final Set<Tile> westArc;
    private final Set<Tile> base_defence;

    private long turnStartTime;
    private int	turn; //Turn number (starts at 0)

    private final Location map[][];
	private final Random seed;
	private final int players;
	private final Set<Tile> unknown;
	private int[] growth;		//Number of our ants over the last 10 turns
	private int myAnts;
	private boolean standOff;
    public List<Order> issuedOrders = new ArrayList<Order>();

    /**
     * Creates new {@link Ants} object.
     * 
     * @param loadTime timeout for initializing and setting up the bot on turn 0
     * @param turnTime timeout for a single game turn, starting with turn 1
     * @param rows game map height
     * @param cols game map width
     * @param turns maximum number of turns the game will be played
     * @param viewRadius2 squared view radius of each ant
     * @param attackRadius2 squared attack radius of each ant
     * @param spawnRadius2 squared spawn radius of each ant
     */
    public Ants(int loadTime, int turnTime, int rows, int cols, int turns, int viewRadius2,
            int attackRadius2, int spawnRadius2, long seed, int players) {
    	
    	this.loadTime = loadTime;
        this.turnTime = turnTime;
        this.rows = rows;
        this.cols = cols;
        this.turns = turns;
        this.viewRadius2 = viewRadius2;
        this.attackRadius2 = attackRadius2;
        this.spawnRadius2 = spawnRadius2;       
        this.turn = 0;
        this.seed = new Random(seed);
        this.players = players;
              
        growth = new int[10];
        unknown = new HashSet<Tile>();
        map = new Location[rows][cols];
        clearMap();
        standOff = false;
        
        // calc vision offsets
        vision_offsets = new HashSet<Tile>();
        int mx = (int)Math.sqrt(viewRadius2);
        for (int row = -mx; row <= mx; ++row) {
            for (int col = -mx; col <= mx; ++col) {
                int d = row * row + col * col;
                if (d <= viewRadius2) {
                    vision_offsets.add(new Tile(row, col));
                }
            }
        }
        // calc combat offset - immediate area of effect, then 1 move away in each direction (as a set of 4 arcs)
        combat_offsets = new HashSet<Tile>();
        northArc = new HashSet<Tile>();
        eastArc = new HashSet<Tile>();
        southArc = new HashSet<Tile>();
        westArc = new HashSet<Tile>();
        
        mx = (int)Math.sqrt(attackRadius2);
        for (int row = -mx; row <= mx; ++row) {
            for (int col = -mx; col <= mx; ++col) {
                int d = row * row + col * col;
                if (d <= attackRadius2) {
                	Tile t = new Tile(row, col);
                    combat_offsets.add(t);
                }
            }
        }
        
        
        for (Tile t: combat_offsets) {
        	Tile n = new Tile(t.getRow()-1, t.getCol());
        	if (!combat_offsets.contains(n)) {
        		northArc.add(n);
        	}
        	Tile e = new Tile(t.getRow(), t.getCol()+1);
        	if (!combat_offsets.contains(e)) {
        		eastArc.add(e);
        	}
        	Tile s = new Tile(t.getRow()+1, t.getCol());
        	if (!combat_offsets.contains(s)) {
        		southArc.add(s);
        	}
        	Tile w = new Tile(t.getRow(), t.getCol()-1);
        	if (!combat_offsets.contains(w)) {
        		westArc.add(w);
        	}
        }
        
        //Defines the border of a 5x5 square around the base with the corners missing
        base_defence = new HashSet<Tile>();
        base_defence.add(new Tile(-2, -1));
        base_defence.add(new Tile(-2, 0));
        base_defence.add(new Tile(-2, 1));
        base_defence.add(new Tile(-1, -2));
        base_defence.add(new Tile(-1, 2));
        base_defence.add(new Tile(0, -2));
        base_defence.add(new Tile(0, 2));
        base_defence.add(new Tile(1, -2));
        base_defence.add(new Tile(1, 2));
        base_defence.add(new Tile(2, -1));
        base_defence.add(new Tile(2, 0));
        base_defence.add(new Tile(2, 1));
    }

    /**
     * Returns timeout for initialising and setting up the bot on turn 0.
     * 
     * @return timeout for initialising and setting up the bot on turn 0
     */
    public int getLoadTime() {
        return loadTime;
    }

    /**
     * Returns timeout for a single game turn, starting with turn 1.
     * 
     * @return timeout for a single game turn, starting with turn 1
     */
    public int getTurnTime() {
        return turnTime;
    }

    /**
     * Returns game map height.
     * 
     * @return game map height
     */
    public int getRows() {
        return rows;
    }

    /**
     * Returns game map width.
     * 
     * @return game map width
     */
    public int getCols() {
        return cols;
    }

    /**
     * Returns maximum number of turns the game will be played.
     * 
     * @return maximum number of turns the game will be played
     */
    public int getTurns() {
        return turns;
    }

    /**
     * Returns squared view radius of each ant.
     * 
     * @return squared view radius of each ant
     */
    public int getViewRadius2() {
        return viewRadius2;
    }

    /**
     * Returns squared attack radius of each ant.
     * 
     * @return squared attack radius of each ant
     */
    public int getAttackRadius2() {
        return attackRadius2;
    }

    /**
     * Returns squared spawn radius of each ant.
     * 
     * @return squared spawn radius of each ant
     */
    public int getSpawnRadius2() {
        return spawnRadius2;
    }

    /**
     * Sets turn start time.
     * 
     * @param turnStartTime turn start time
     */
    public void setTurnStartTime(long turnStartTime) {
        this.turnStartTime = turnStartTime;
        this.turn++;
    }

    /**
     * Returns how much time the bot has still has to take its turn before timing out.
     * 
     * @return how much time the bot has still has to take its turn before timing out
     */
    private int getTimeRemaining() {
        return turnTime - (int)(System.currentTimeMillis() - turnStartTime);
    }

    /**
     * Returns ilk at the specified location.
     * 
     * @param tile location on the game map
     * 
     * @return ilk at the <cod>tile</code>
     */
    public Ilk getIlk(Tile tile) {
        return map[tile.getRow()][tile.getCol()].getIlk();
    }

    /**
     * Returns location in the specified direction from the specified location.
     * 
     * @param tile location on the game map
     * @param direction direction to look up
     * 
     * @return location in <code>direction</code> from <cod>tile</code>
     */
    private Tile getTile(Tile tile, Aim direction) {
        int row = (tile.getRow() + direction.getRowDelta()) % rows;
        if (row < 0) {
            row += rows;
        }
        int col = (tile.getCol() + direction.getColDelta()) % cols;
        if (col < 0) {
            col += cols;
        }
        return new Tile(row, col);
    }

    private Tile getTile(Tile tile, Tile offset) {
        int row = (tile.getRow() + offset.getRow()) % rows;
        if (row < 0) {
            row += rows;
        }
        int col = (tile.getCol() + offset.getCol()) % cols;
        if (col < 0) {
            col += cols;
        }
        return new Tile(row, col);
    }
    
    /**
     * Returns a set containing all my ants locations ordered by their score (lowest first)
     * 
     * @return a set containing all my ants locations ordered by their score (highest first)
     */
    private Set<Tile> getMyAnts() {
    	class TileScore implements Comparable<TileScore> {
    		public Tile		tile;
    		public double	score;	//The combined score of this tile and the 4 tiles 1 move away
    		
    		public TileScore(int x, int y) {
    			tile = new Tile(x, y);
    			score = 0;
    			for (Tile a: makeZonePlus1(tile))
    				score += map[a.getRow()][a.getCol()].getScore();   			
    		}
    		//Used to sort the list of our ants in score order - lowest first
    		public int compareTo(TileScore l) {
    			if (this.score == l.score) {
    				if (map[this.tile.getRow()][this.tile.getCol()].getRand() < map[l.tile.getRow()][l.tile.getCol()].getRand())
    					return -1;
    				else if (map[this.tile.getRow()][this.tile.getCol()].getRand() > map[l.tile.getRow()][l.tile.getCol()].getRand())
    					return 1;
    				return 0;
    			}
    			if (this.score < l.score)
    				return -1;
    			return 1;
    		}
    	}
    	Set<Tile> ants = new LinkedHashSet<Tile>();
    	List<TileScore> scores = new ArrayList<TileScore>();  
    	
        for (int x=0; x<rows; x++) {
        	for (int y=0; y<cols; y++) {       		
        		if (map[x][y].getAnt(turn) == 0) {      		
        			scores.add(new TileScore(x, y));
        		}
        	}
        }
        Collections.sort(scores); //Sort with lowest score first
        for (TileScore t: scores)
        	ants.add(t.tile);
        return ants;
    }
    
    /*
     * Compare the scores of 2 locations and returns true if the first is better than the second
     */
    private boolean betterThan(Location l1, Location l2) {
    	return (l1.getScore() > l2.getScore() || (l1.getScore() == l2.getScore() && l1.getRand() > l2.getRand()));
    }
    
    private int whichBit(Aim dir) {
    	switch (dir) {
    	case NORTH:
    		return 1;
    	case EAST:
    		return 2;
    	case SOUTH:
    		return 3;
    	case WEST:
    		return 4;
    	}
    	return 0;
    }
    
    private int reverseDir(Aim dir) {
    	switch (dir) {
    	case NORTH:
    		return 3;
    	case EAST:
    		return 4;
    	case SOUTH:
    		return 1;
    	case WEST:
    		return 2;
    	}
    	return 0;
    }
    
    /**
     * Clears game map state information
     */
    public void clearMap() {
    	growth[turn%growth.length] = myAnts;	//Store number of ants we had
    	for (int x=0; x<rows; x++)
    		for (int y=0; y<cols; y++) {
    			if (map[x][y] == null)
    				map[x][y] = new Location(x, y);
    			map[x][y].setScore(0);
    			map[x][y].setRand(seed.nextDouble());
    		}
    	myAnts = 0;
    	this.unknown.clear();
    }
    
    private boolean growing() {
    	double	total = 0;
    	for (int i: growth)
    		total += i;
    	double average = total/growth.length;
    	return (myAnts > average);
    }

    /*
     * Everything seen will have been added already - so anything in the visible area must be a plain old land
     */
    public void setVision() {
        for (int x= 0; x<rows; x++) {
        	for (int y=0; y<cols; y++) {
        		if (map[x][y].getAnt(turn) == 0) {
		            for (Tile o: vision_offsets) {
		            	Tile t = getTile(new Tile(x,y), o);
		                if (map[t.getRow()][t.getCol()].lastSeen() != turn) {
		                	map[t.getRow()][t.getCol()].clearIlk(turn);
		                }
		            }
        		}
        	}
        }
        /*
         * Find unknown tiles next to known tiles
         */
        for (int x=0; x<rows; x++) {
        	for (int y=0; y<cols; y++) {
        		Tile t = new Tile(x, y);      		
        		if (getIlk(t).isPassable()) { //If a land or food tile is next to an unknown tile, add it to the set
        			for (Aim direction: Aim.values()) {
        				Tile adj = getTile(t, direction);
        				if (getIlk(adj) == Ilk.UNKNOWN) {
        					unknown.add(t);
        					break;
        				}
        			}
        		}
        	}
        }
    }

    /**
     * Updates game state information about new ants and food locations.
     * 
     * @param ilk ilk to be updated
     * @param tile location on the game map to be updated
     */
    public void update(Ilk ilk, Tile tile) {
    	//debug("update %d %d = %s\n", tile.getRow(), tile.getCol(), ilk);
        map[tile.getRow()][tile.getCol()].setIlk(ilk, turn);
    }
    
    public void addAnt(int owner, Tile tile) {
    	//debug("a %d %d %d\n", tile.getRow(), tile.getCol(), owner);
    	map[tile.getRow()][tile.getCol()].setAnt(owner, turn);
    	if (owner == 0)
    		myAnts++;
    }
    
    public void removeAnt(int owner, Tile tile) {
    	//debug("d %d %d %d\n", tile.getRow(), tile.getCol(), owner);
    	if (map[tile.getRow()][tile.getCol()].lastSeen() < turn)
    		map[tile.getRow()][tile.getCol()].setAnt(-1, turn);
    }

    /**
     * Updates game state information about hills locations.
     *
     * @param owner owner of hill
     * @param tile location on the game map to be updated
     */
    public void updateHills(int owner, Tile tile) {
    	//debug("h %d %d %d\n", tile.getRow(), tile.getCol(), owner);
        map[tile.getRow()][tile.getCol()].setHill(owner, turn);
    }

    /**
     * Issues an order by sending it to the system output.
     * 
     * @param myAnt map tile with my ant
     * @param direction direction in which to move my ant
     */
    public void issueOrder(Tile myAnt, Aim direction) {
        issuedOrders.add(new Order(myAnt, direction));
        //System.out.println(new Order(myAnt, direction));
        //System.out.flush();
    }
    
    private void debug(String format, Object... args) {
    	if (!write_debug)
    		return;
    	FileOutputStream file;
		try {
			file = new FileOutputStream("debug.txt",true);
		} catch (FileNotFoundException e) {
			write_debug = false;
			return;
		}
    	PrintStream p = new PrintStream(file);
    	
    	p.printf(format, args);
    	try {
			file.close();
		} catch (IOException e) {
		}
    }
    
    /*
     * Ripple out from the given edge (set of points) until a given number of our ants have been found scoring each tile as we go - the nearer the higher the score
     */
    public Ripple ripple(Set<Tile> edge, double points, int ants) {
    	int SEEN = 1, SCORED = 2;
    	int[][] added = new int[rows][cols];
    	int[][] delay = new int[rows][cols];
    	Ripple result = new Ripple();
    	
    	//if (edge.size() > 1)
    	//	debug("Ripple %d tiles: points %f ants %d\n", edge.size(), points, ants);
 	
    	while (!edge.isEmpty()) {
    		result.distance++;
    		
        	for (Tile t: edge) {
        		if (added[t.getRow()][t.getCol()] == 0) {
        			int owner = map[t.getRow()][t.getCol()].getAnt(turn);
	        		if (owner == 0)  { //Count number of our ants that are affected
	            		result.ants++;
	            		if (result.nearestAnt < 0)
	            			result.nearestAnt = result.distance;
	        		} else if (owner > 0) {
	        			result.enemies++;
	        			if (result.nearestEnemy < 0)
	        				result.nearestEnemy = result.distance;
	        		}
	        		added[t.getRow()][t.getCol()] = SEEN;
        		}
        	}
        	
        	if (result.distance > 1 && result.ants >= ants) //We have reached the cutoff point
        		break;
    		
    		//Score the tiles and work out next edge set to process
    		Set<Tile> nextEdge = new HashSet<Tile>();
    		for (Tile me: edge) { 
    			//Score this tile
    			if (added[me.getRow()][me.getCol()] != SCORED) {
    				added[me.getRow()][me.getCol()] = SCORED;
    				map[me.getRow()][me.getCol()].addScore(points/(result.distance*result.distance));
    			}
    			//To avoid massive pile ups - any of my ants will slow down the progress of the edge according to how long they have been occupied
    			if (map[me.getRow()][me.getCol()].getAnt(turn) == 0 && delay[me.getRow()][me.getCol()] == 0)
    				delay[me.getRow()][me.getCol()] = map[me.getRow()][me.getCol()].occupiedFor(turn);
    			if (--delay[me.getRow()][me.getCol()] > 0)
    				nextEdge.add(me);	//Edge stays here
    			else { //Add in adjacent tiles if passable and not already processed	       				            	 			
		    		for (Aim direction: Aim.values()) {
		    			Tile t = getTile(me, direction);
		    			if (getIlk(t).isPassable() && added[t.getRow()][t.getCol()] == 0)
		    				nextEdge.add(t);	    			
		    		}
    			}
    		}
    		if (result.enemies > 0 && result.enemies >= result.ants && result.danger < 0)
    			result.danger = result.distance;
    		if (result.ants > result.enemies && result.safe < 0)
    			result.safe = result.distance;
    		
    		edge = nextEdge;
    	}

    	return result;
    }
    
    public Ripple ripple(Tile t, double points, int ants) {
    	Set<Tile> edge = new HashSet<Tile>();
    	edge.add(t);
    	//debug("Ripple (%d,%d): points %f ants %d\n", t.getRow(), t.getCol(), points, ants);
    	return ripple(edge, points, ants);
    }
   
    /*
     * Zone routines - creates a set of tiles from a given starting point with a set of offsets
     */
    private Set<Tile> makeZone(Tile t, Set<Tile>offsets) {
    	Set<Tile> result = new HashSet<Tile>();
    	for (Tile z: offsets) {
    		Tile a = getTile(t, z);
    		if (getIlk(a).isPassable())
    			result.add(a);
    	}
    	return result;
    }

    private Set<Tile> makeZonePlus1(Tile ant, Set<Tile>zone) {
    	Set<Tile> result = makeZone(ant, zone);
    	
    	for (Aim direction: Aim.values()) {
    		Tile adj = getTile(ant, direction);
    		if (getIlk(adj).isPassable()) {
				result.addAll(makeZone(adj, zone));
    		}
    	}
    	
		return result;
    }
    
    private Set<Tile> makeZonePlus1(Tile t) {
    	Set<Tile> zone = new HashSet<Tile>();
    	zone.add(new Tile (0,0));
    	return makeZonePlus1(t, zone);
    }

    
    private Set<Tile> getArc(Tile t, Aim dir) {
    	if (dir == Aim.NORTH)
    		return makeZone(t, northArc);
    	if (dir == Aim.EAST)
    		return makeZone(t, eastArc);
    	if (dir == Aim.SOUTH)
    		return makeZone(t, southArc);
    	
    	return makeZone(t, westArc);
    }

    /*
     * Calculate how many enemy ants could move into the combat zone of the given tile
     */
    private int enemyCount(Tile t, int owner) {
    	Set<Tile> zone = makeZone(t, combat_offsets);
    	Set<Tile> border = new HashSet<Tile>();
    	for (Aim dir: Aim.values())
    		border.addAll(getArc(t, dir));
    	
    	Set<Tile> attackers = new HashSet<Tile>();
    	for (Tile z: border) {
    		int ant = map[z.getRow()][z.getCol()].getAnt(turn);
    		if (ant >= 0 && ant != owner)
    			attackers.add(z);
    	}
    	
    	//Count enemies already in the zone
    	int enemies = 0;
    	for (Tile z: zone) {
    		int ant = map[z.getRow()][z.getCol()].getAnt(turn);
    		if (ant >= 0 && ant != owner)
    			enemies++;
    	}
    	
    	if (attackers.size() == 0)
    		return enemies;
    	
    	//Now scan the attackers and see which ants can move in
    	boolean moved = true;
    	Set<Tile> done = new HashSet<Tile>();	//A list of tiles to remove from the attackers list once we have finished with the iterator
    	while (moved && attackers.size() > 0) {
    		moved = false;
    		for (Tile a: attackers) {
    			Set<Tile> moves = makeZonePlus1(a);
    			moves.retainAll(zone);
    			if (moves.size() == 1) { //Only 1 valid move into the combat zone
    				enemies++;
    				zone.removeAll(moves);
    				done.add(a);
    				moved = true;
    			}
    		}
    		attackers.removeAll(done);
    		done.clear();
    	}
    	//Any attackers left either have no moves or more than 1 valid move
    	for (Tile a: attackers) {
			Set<Tile> moves = makeZonePlus1(a);
			moves.retainAll(zone);
			if (moves.size() > 0)
				enemies++;
		}
    	
    	return enemies;
    }
    
    
    /*
     * Given a set of enemies, work out if combat within a given range would be safe or not.
     * Mark all unsafe areas if we would lose with a score of -1 (if we die within killing another ant) or -0.5 if we die and kill another ant
     */
    private void combatResult(Set<Tile> enemies) { 
    	Set<Tile> combatZone = new HashSet<Tile>(); //All tiles reachable with 1 move where our ants could be in combat
    	int[][][] cache = new int[players][rows][cols]; //Used to cache the results from the enemyCount routine
    	int[][][] fighting = new int[players][rows][cols]; //For tile, a players best ant (lowest number of enemies being fought) within combat range
    	Set<Tile> ants = new HashSet<Tile>(); // All the ants we know about
    	
    	//Initialise the fighting array
    	for (int p=0; p<players; p++)
    		for (int r=0; r<rows; r++)
    			Arrays.fill(fighting[p][r], Integer.MAX_VALUE);
    	
		for (int x=0; x<rows; x++) {
    		for (int y=0; y<cols; y++) {
				int owner = map[x][y].getAnt(turn);				
				if (owner >= 0) {
					Tile t = new Tile(x,y);
					ants.add(t);
					for (Tile a: makeZonePlus1(t)) { //Consider each tile an ant can be in after 1 move	
			    		int enemy_count = enemyCount(a, owner);
			    		cache[owner][a.getRow()][a.getCol()] = enemy_count;		//Cache the result for later
						if (owner == 0 && enemy_count > 0)
							combatZone.add(a);
			    		for (Tile z: makeZone(a, combat_offsets)) {	//Work out combat area from this ant's tile			
		    				if (enemy_count < fighting[owner][z.getRow()][z.getCol()])
		    					fighting[owner][z.getRow()][z.getCol()] = enemy_count;
						}						
					}
				}
    		}
    	}
    	
    	/*
    	 * We can now work out if an ant would die in a tile
    	 * Do this for each ant in the combat zone and each tile it could move to and store the results in an array called status
    	 */	
    	int[][][] status = new int[players][rows][cols];
    	final int DIE = 3, KILL = 2, SAFE = 1; 
    	for (Tile t: ants) {
			int owner=map[t.getRow()][t.getCol()].getAnt(turn);
			for (Tile a: makeZonePlus1(t)) {
				int enemy_count = cache[owner][a.getRow()][a.getCol()];
				int best = Integer.MAX_VALUE; //How many ants the best enemy is fighting
				
				for (int p=0; p<players; p++) {
					if (p != owner && fighting[p][a.getRow()][a.getCol()] < best) 
						best = fighting[p][a.getRow()][a.getCol()];
				}
								
				if (best < enemy_count)
					status[owner][a.getRow()][a.getCol()] = DIE;
				else if (best == enemy_count)
					status[owner][a.getRow()][a.getCol()] = KILL;
				else
					status[owner][a.getRow()][a.getCol()] = SAFE;
			}
		}
    	
		/*
		 * For each enemy ant we work out if we should enter into unsafe tiles in order to get a kill during a stand off
		 * Remove any tiles considered safe from the combat zone
		 */  
    	if (standOff && growing()) {  
			for (Tile e: enemies) {
				Set<Tile> advance = new HashSet<Tile>();
				int owner = map[e.getRow()][e.getCol()].getAnt(turn);
				//Check to see which arcs we control by seeing if an enemy move towards us would result in death
				for (Aim direction: Aim.values()) {
					Tile adj = getTile(e, direction);	
					if (status[owner][adj.getRow()][adj.getCol()] >= KILL || !getIlk(adj).isPassable())
						advance.addAll(getArc(e, direction));
				}
				advance.addAll(makeZone(e, combat_offsets));
				//If the advance zone contains at least one SAFE or KILL tile we push forward
				for (Tile t: advance) {
					if (status[0][t.getRow()][t.getCol()] == SAFE || status[0][t.getRow()][t.getCol()] == KILL) {
						combatZone.removeAll(advance);
						break;
					}
				}				
			} 
    	}
		
		// Any tile in the combat zone where we would die is marked as unsafe
		for (Tile t: combatZone) {
			switch (status[0][t.getRow()][t.getCol()])	{
			case DIE:
				map[t.getRow()][t.getCol()].setScore(-1);
				break;
			case KILL:
				map[t.getRow()][t.getCol()].setScore(-0.5);
				break;
			case SAFE:
				map[t.getRow()][t.getCol()].addScore(0.3); //Make this tile more appealing
				break;
			}
		}
    } 
    
    private void issueOrders() {
    	//For each ant - send it to the highest scoring valid adjacent square
    	boolean[][] todo = new boolean[rows][cols];	//True if we are yet to move the ant in this location
    	boolean[][] filled = new boolean[rows][cols]; //Occupied after ants move
    	BitSet[][] pushed = new BitSet[rows][cols]; //This is a bit set that indicates if an ant in the given direction wants to come here
    	int unmoved = 0;	//Our ants that don't get moved - used to work out if we have a stand off situation
    	
    	Set<Tile> ants = getMyAnts();
    	for (Tile ant: ants) {
    		todo[ant.getRow()][ant.getCol()] = true;
    	}
    	
    	boolean issued_order = true;    	
    	while (issued_order && getTimeRemaining() > ORDER_TIME) {
    		issued_order = false;
	    	for (Tile ant: ants) {
	    		if (todo[ant.getRow()][ant.getCol()]) {
		    		Location best = null;
		    		Aim	order = null;		    		
		    		
		    		//Can we move to a better tile
		    		for (Aim direction: Aim.values()) {
		    			Tile adj = getTile(ant, direction);
		    			//Is this a valid direction to consider
		    			boolean valid = getIlk(adj).isUnoccupied() && !filled[adj.getRow()][adj.getCol()] &&
		    					(pushed[ant.getRow()][ant.getCol()] == null || !pushed[ant.getRow()][ant.getCol()].get(reverseDir(direction)));
		    			
		    			//Mark this as the best move if we can move to the cell and it is better than our current best move
		    			if (valid && (best == null || betterThan(map[adj.getRow()][adj.getCol()], best))) {
		    				best = map[adj.getRow()][adj.getCol()];
		    				order = direction;
		    			}
		    		}
		    		
		    		/*
		    		 * Order now contains the best direction to move in
		    		 */
		    		if (order == null || (pushed[ant.getRow()][ant.getCol()] != null && best.getScore() < 0)) {
		    			//No possible move found - stay still
		    			todo[ant.getRow()][ant.getCol()] = false;
		    			filled[ant.getRow()][ant.getCol()] = true;
		    			issued_order = true;
		    			unmoved++;
		    		} else {
		    			Tile adj = getTile(ant, order);
		    			// We only move if the new position is better than our current position or if we have been pushed out and the new position is not deadly	
		    			if ((pushed[ant.getRow()][ant.getCol()] != null && best.getScore() >= 0) || best.getScore() >  map[ant.getRow()][ant.getCol()].getScore()) {
		    				//If the destination is currently occupied issue a push request
			    			if (todo[adj.getRow()][adj.getCol()]) {
			    				if (pushed[adj.getRow()][adj.getCol()] == null)
			    					pushed[adj.getRow()][adj.getCol()] = new BitSet(5);
			    				if (!pushed[adj.getRow()][adj.getCol()].get(whichBit(order))) {
			    					pushed[adj.getRow()][adj.getCol()].set(whichBit(order));		    					
			    					issued_order = true; //This counts as a change of state - ensure we loop again
			    				}
			    			} else {	    			
				    			issueOrder(ant, order);
				    			filled[adj.getRow()][adj.getCol()] = true;
				    			todo[ant.getRow()][ant.getCol()] = false;
				    			issued_order = true;
				    		}
		    			}
		    		}
	    		}
	    	}
    	}
    	
    	/*
    	 * Any ants left in the to do list are not moved
    	 */    	
    	for (Tile t: ants)
    		if (todo[t.getRow()][t.getCol()])
    			unmoved++;
    	
    	//In the last 10% of the game we try to break standOffs quicker
    	standOff = (unmoved > ((turn * 10 > turns * 9)?16:32));
    }
    
    /*
     * Called by MyBot after we have received all the information in our visibility zone
     * This routine issues the orders for all our ants that we want to move
     */
    public void explore() {
    	final double SCORE_FOOD = 8.0;
    	final double SCORE_ENEMY_HILL = 16.0;
    	final double SCORE_ENEMY_ANT = 1.0;
    	final double SCORE_PROTECT_HOME = 3.0;
    	final double SCORE_UNKNOWN = 4.0;	//The border of our known space
    	final double SCORE_EXPIRED = 1.0;  //Tiles we haven't seen in a while
    	final int COMBAT_TIME = turnTime / 2;
    	
    	Set<Tile> hills = new HashSet<Tile>(); //My hills - may need protecting depending on distance to nearest threat
    	Set<Tile> expired = new HashSet<Tile>(); //Expired tiles that haven't been seen in a while and need to be revisited
    	Set<Tile> enemies = new HashSet<Tile>(); //Enemy ants in our visible range - used to work out if we should enter combat
    	Set<Tile> food = new HashSet<Tile>();
    	Set<Tile> enemyHills = new HashSet<Tile>();
    	
    	for (int x=0; x<rows; x++) {
    		for (int y=0; y<cols; y++) {
    			Tile t = new Tile(x, y);
    			
    			int hill = map[x][y].getHill();    			
        		if (hill > 0)	//Enemy hills
        			enemyHills.add(t);       			      		
        		else if (hill == 0)     //Home hills   			
        			hills.add(t);
        		
        		 //Expired squares
        		if (map[x][y].expired(turn))
            		expired.add(t);
        		else {       			           		
	    			//Food squares
	    			if (map[x][y].getIlk() == Ilk.FOOD)
	    				food.add(t);	    				
	        		//Enemy ants
	        		if (map[x][y].getAnt(turn) > 0)
	        			enemies.add(t);
        		}        		
    		}
    	}
    	
    	//Enemy hills - send all our ants to attack them once found
    	ripple(enemyHills, SCORE_ENEMY_HILL, Integer.MAX_VALUE);	
    	
    	//Food
    	for (Tile f: food) {
    		Ripple r = ripple(f, SCORE_FOOD, 1);
    		if (r.nearestEnemy > 0 && r.nearestEnemy < r.nearestAnt) //enemy is nearer so cancel the attraction
    			ripple(f, -SCORE_FOOD, 1);
    		
    	}	
      	
    	//Unexplored or expired squares
    	ripple(unknown, SCORE_UNKNOWN, Integer.MAX_VALUE);
    	if (expired.size() < 120)
    		for (Tile t: expired) { //Send 1 ant to each expired tile - often 1 ant will respond to many requests
		    	ripple(t, SCORE_EXPIRED, 1);
		    	if (getTimeRemaining() < COMBAT_TIME)
		    		break;
			}   		
    	else
    		ripple(expired, SCORE_EXPIRED, Math.min(myAnts/2, (rows*cols)/(viewRadius2*2)));
    	
    	//Home base - protect if enemies nearer than our protection force
    	for (Tile t: hills) {    		
    		int defend = myAnts/(hills.size()*3);
    		if (defend > 0) {
    			Set<Tile>	base = makeZone(t, base_defence);
	    		Ripple r = ripple(base, 0, Math.min(4, defend));
	    		if (r.danger > 0 || (r.nearestEnemy > 0 && r.nearestEnemy - r.nearestAnt < 4)) {
					if (r.nearestEnemy > 4 && r.danger > 4)   			   		
						ripple(base, SCORE_PROTECT_HOME, Math.min(r.enemies+2, defend));
					else
						ripple(t, SCORE_PROTECT_HOME, Math.min(r.enemies+2, defend));
	    		}
    		}
    		if (turn%3 != 0)
    			map[t.getRow()][t.getCol()].addScore(-1.0); //Try to keep the base clear so we can spawn
    	}
    	
    	//Enemy ants - only attack if we can out-number the opposition
    	for (Tile t: enemies) {
    		Set<Tile> z = makeZone(t, combat_offsets);
    		Ripple r = ripple(z, 0, 4);
    		if (r.safe > 0) //If we can out-number the enemy - attack it
    			ripple(z, SCORE_ENEMY_ANT, r.ants+2);		//DT - this should be enemies + 2 but this works better!
    		map[t.getRow()][t.getCol()].setScore(-1); //We cannot move onto an occupied tile
    	}
    	//Set the score on any enemy controlled tile to -1 (we DIE) or -0.5 (mutual kill)
    	combatResult(enemies); 
    	
    	for (Tile f: food)
    		map[f.getRow()][f.getCol()].setScore(-1); //We cannot actually move onto the food tile
    	
    	if (write_debug)
			for (int x = 0; x<rows; x++) {
				for (int y=0; y<cols; y++)
					debug("%5.5f ", map[x][y].getScore());
				debug("\n");
			}
    	
		issueOrders();
    }
}
