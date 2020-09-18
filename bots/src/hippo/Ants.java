package hippo;

import java.util.*;

/**
 * Holds all game data and current game state.
 * TODO make it static ?? I don't think alpha beta could be used on such big objects ...
 */
public class Ants {
    /**
     * Maximum map size.
     */
    public static final int MAX_MAP_SIZE = 256;
    public Search search;
    public int safeModShift;
    private final int loadTime;
    private final int turnTime;
    private final int safetyTime;
    private final int viewRadius2;
    private final int attackRadius2;
    private final int spawnRadius2;
    private final int view1;
    private final int attack1;
    private final int spawn1;
    private static long turnStartTime;
    public int myAttackLevel[][];
    public int enemyAttackLevel[][];
    private Tile attackedByEnemy[][][];// one of ants able to attack given coordinate
    private Tile attackedByMy[][][];// one of ants able to attack given coordinate
    public int enemyAttackSearchLevel;
    private TileList enemyAttackTiles;
    private TileList myAttackTiles;
    public static Random random = new Random(123456);
    public Pondering pondering;
    /*
     *  0 cannot be attacked by visible enemyies,
     *  1 can be attacked by 1 visible enemy
     *  2 is attacked by 1 visible enemy
     *  3 could be attacked by at least 2 visible enemies
     *  2k>=4 is attacked by k visible enemies
     */
    private final Ilk map[][];
    /* foodDiscovery for symmetry food generation
     *  IF EXPECTED_FOOD or FOOD or SYMMETRY_FOOD k means food from turn at most k ... remains even when the tile is not seen
     *  otherwise the value has no meaning
     *  for symmetry food ... mirror expected food provided lastSeen(sym)<food(ori) and food(sym)<food(ori)
     */
    public int foodDiscovery[][];
    public int myHive = 0; // to be computed if needed!
    public int myPrevHive = 0; // to be computed if needed!
    public HillTypes hills[][];
    public int maxOwner = 0;
    public int safeLevel = 2;
    public int owners[][];
    public int lastSeen[][];
    public int visibilityQCircle[];
    public int attackQCircle[];
    public int spawnQCircle[];
    public int turn = 0;
    private List<Order> orders = new ArrayList<Order>();
    private MyBot mybot;

    /**
     * Creates new {@link Ants} object.
     *
     * @param loadTime      timeout for initializing and setting up the bot on turn 0
     * @param turnTime      timeout for a single game turn, starting with turn 1
     * @param rows          game map height
     * @param cols          game map width
     * @param turns         maximum number of turns the game will be played
     * @param viewRadius2   squared view radius of each ant
     * @param attackRadius2 squared attack radius of each ant
     * @param spawnRadius2  squared spawn radius of each ant
     */
    public Ants(int loadTime, int turnTime, int rows, int cols, int turns,
                int viewRadius2, int attackRadius2, int spawnRadius2, MyBot mybot) {
        this.loadTime = loadTime;
        this.turnTime = turnTime;
        this.safetyTime = turnTime / 5;
        this.rows = rows;
        this.cols = cols;
        this.turns = turns;
        this.viewRadius2 = viewRadius2;
        this.attackRadius2 = attackRadius2;
        this.spawnRadius2 = spawnRadius2;
        this.mybot = mybot;
        hills = new HillTypes[rows][cols];
        for (HillTypes[] row : hills) {
            Arrays.fill(row, HillTypes.LAND);
        }
        foodDiscovery = new int[rows][cols];
        for (int[] row : foodDiscovery) {
            Arrays.fill(row, 0);
        }
        map = new Ilk[rows][cols];
        for (Ilk[] row : map) {
            Arrays.fill(row, Ilk.UNKNOWN);
        }
        myAttackLevel = new int[rows][cols];
        for (int[] row : myAttackLevel) {
            Arrays.fill(row, -1);
        }
        enemyAttackLevel = new int[rows][cols];
        for (int[] row : enemyAttackLevel) {
            Arrays.fill(row, -1);
        }
        attackedByEnemy = new Tile[rows][cols][2];
        for (Tile[][] row : attackedByEnemy) {
            Arrays.fill(row, null);
        }
        attackedByMy = new Tile[rows][cols][2];
        for (Tile[][] row : attackedByMy) {
            Arrays.fill(row, null);
        }
        enemyFree = new boolean[rows][cols];
        for (boolean[] row : enemyFree) {
            Arrays.fill(row, false);
        }
        owners = new int[rows][cols];// neednot be initialised
        lastSeen = new int[rows][cols];
        for (int[] row : lastSeen) {
            Arrays.fill(row, -100);
        }
        view1 = (int) Math.sqrt(viewRadius2);
        visibilityQCircle = new int[1 + view1];
        for (int i = 0; i <= view1; i++) {
            visibilityQCircle[i] = (int) Math.sqrt(viewRadius2 - i * i);
        }
        attack1 = (int) Math.sqrt(attackRadius2);
        attackQCircle = new int[1 + attack1];
        for (int i = 0; i <= attack1; i++) {
            attackQCircle[i] = (int) Math.sqrt(attackRadius2 - i * i);
        }
        spawn1 = (int) Math.sqrt(spawnRadius2);
        spawnQCircle = new int[1 + spawn1];
        for (int i = 0; i <= spawn1; i++) {
            spawnQCircle[i] = (int) Math.sqrt(spawnRadius2 - i * i);
        }
        Aim.init();
        Search.init(this);// must precede Symmetry
        search = new Search();
        TileList.init(this);
        pondering = new Pondering();
        pondering.init(this, mybot);
        mybot.init(this);
        Set<Ilk> mA = new HashSet<Ilk>();
        mA.add(Ilk.MY_ANT);
        myAnts = new TileList(mA, null);
        Set<Ilk> mTA = new HashSet<Ilk>();
        mTA.add(Ilk.MY_TMP_ANT);
        myTmpAnts = new TileList(mTA, null);
        Set<Ilk> mAP = new HashSet<Ilk>();
        mAP.add(Ilk.MY_ANT_PLANNED);
        myAntsPlanned = new TileList(mAP, null);
        Set<Ilk> eA = new HashSet<Ilk>();
        eA.add(Ilk.ENEMY_ANT);
        enemyAnts = new TileList(eA, null);
        Set<HillTypes> mH = new HashSet<HillTypes>();
        mH.add(HillTypes.MY_HILL);
        myHills = new TileList(null, mH);
        Set<HillTypes> eMH = new HashSet<HillTypes>();
        eMH.add(HillTypes.EXPECTED_MY_HILL);
        myExpectedHills = new TileList(null, eMH);
        Set<HillTypes> eH = new HashSet<HillTypes>();
        eH.add(HillTypes.ENEMY_HILL);
        eH.add(HillTypes.SYMMETRY_ENEMY_HILL);
        enemyHills = new TileList(null, eH);
        Set<HillTypes> eEH = new HashSet<HillTypes>();
        eEH.add(HillTypes.EXPECTED_ENEMY_HILL);
        eEH.add(HillTypes.SYMMETRY_ENEMY_HILL);
        enemyExpectedHills = new TileList(null, eEH);
        Set<HillTypes> dH = new HashSet<HillTypes>();
        dH.add(HillTypes.DEAD);
        deadHills = new TileList(null, dH);
        Set<Ilk> fT = new HashSet<Ilk>();
        fT.add(Ilk.FOOD);
        foodTiles = new TileList(fT, null);
        Set<Ilk> pFT = new HashSet<Ilk>();
        pFT.add(Ilk.PLANNED_FOOD);
        plannedFoodTiles = new TileList(pFT, null);
        Set<Ilk> pSFT = new HashSet<Ilk>();
        pSFT.add(Ilk.PLANNED_SYMMETRY_FOOD);
        plannedSymmetryFoodTiles = new TileList(pSFT, null);
        Set<Ilk> pXFT = new HashSet<Ilk>();
        pXFT.add(Ilk.PLANNED_EXPLORATION_FOOD);
        plannedExplorationFoodTiles = new TileList(pXFT, null);
        Set<Ilk> eFT = new HashSet<Ilk>();
        eFT.add(Ilk.EXPECTED_FOOD);
        expectedFoodTiles = new TileList(eFT, null);
        Set<Ilk> sFT = new HashSet<Ilk>();
        sFT.add(Ilk.SYMMETRY_FOOD);
        symmetryFoodTiles = new TileList(sFT, null);
        Set<Ilk> xFT = new HashSet<Ilk>();
        xFT.add(Ilk.EXPLORATION_FOOD);
        explorationFoodTiles = new TileList(xFT, null);
        Set<Ilk> dA = new HashSet<Ilk>();
        dA.add(Ilk.DEAD);
        deadAnts = new TileList(dA, null);// not used yet, owner may be important
        enemyAttackTiles = new TileList(null, null);// owner may be important used for enemyAttack computation
        myAttackTiles = new TileList(null, null);//  used for myAttack computation
        frontLine = new TileList(null, null);
        safeModShift = 4 * cols * rows;
    }

    /**
     * Returns timeout for initializing and setting up the bot on turn 0.
     *
     * @return timeout for initializing and setting up the bot on turn 0
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

    private final int rows;

    /**
     * Returns game map height.
     *
     * @return game map height
     */
    public int getRows() {
        return rows;
    }

    private final int cols;

    /**
     * Returns game map width.
     *
     * @return game map width
     */
    public int getCols() {
        return cols;
    }

    private final int turns;

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
     * Returns squared view radius of each ant.
     *
     * @return squared view radius of each ant
     */
    public int getView1() {
        return view1;
    }

    /**
     * Returns squared attack radius of each ant.
     *
     * @return squared attack radius of each ant
     */
    public int getAttack1() {
        return attack1;
    }

    /**
     * Returns squared spawn radius of each ant.
     *
     * @return squared spawn radius of each ant
     */
    public int getSpawn1() {
        return spawn1;
    }

    /**
     * Sets turn start time.
     *
     * @param turnStartTime turn start time
     */
    public static void setTurnStartTime(long turnStartTime_) {
        turnStartTime = turnStartTime_;
    }

    /**
     * Returns how much time the bot has still has to take its turn before
     * timing out.
     *
     * @return how much time the bot has still has to take its turn before
     * timing out
     */
    public int getTimeRemaining() {
        return turnTime - safetyTime - (int) (System.currentTimeMillis() - turnStartTime);
    }

    public HillTypes getHill(int row, int col) {
        return hills[(row + rows) % rows][(col + cols) % cols];
    }

    public HillTypes getHill(Tile tile) {
        return getHill(tile.getRow(), tile.getCol());
    }

    private int getLastSeenModOK(int r, int c) {
        return lastSeen[r][c];
    }

    public int getLastSeen(int row, int col) {
        int r = (row + safeModShift) % rows, c = (col + safeModShift) % cols;
        return getLastSeenModOK(r, c);
    }

    public int getLastSeen(Tile tile) {
        return getLastSeenModOK(tile.getRow(), tile.getCol());
    }

    private int getFoodDiscoveryModOK(int r, int c) {
        return foodDiscovery[r][c];
    }

    public int getFoodDiscovery(int row, int col) {
        int r = (row + safeModShift) % rows, c = (col + safeModShift) % cols;
        return getFoodDiscoveryModOK(r, c);
    }

    public int getFoodDiscovery(Tile tile) {
        return getFoodDiscoveryModOK(tile.getRow(), tile.getCol());
    }

    public void setFoodDiscovery(Tile tile, int turn) {
        foodDiscovery[tile.getRow()][tile.getCol()] = turn;
    }

    private Ilk getIlkModOK(int r, int c) {
        if (map[r][c] == Ilk.UNKNOWN) {
            return pondering.getUnknownIlk(r, c);
        } else {
            return map[r][c];
        }
    }

    public Ilk getIlk(int row, int col) {
        int r = (row + safeModShift) % rows, c = (col + safeModShift) % cols;
        return getIlkModOK(r, c);
    }

    /**
     * Returns ilk at the specified location.
     *
     * @param tile location on the game map
     * @return ilk at the <cod>tile</code>
     */
    public Ilk getIlk(Tile tile) {
        return getIlkModOK(tile.getRow(), tile.getCol());
    }

    /**
     * Returns ilk at the location in the specified direction from the specified
     * location.
     *
     * @param tile      location on the game map
     * @param direction direction to look up
     * @return ilk at the location in <code>direction</code> from
     * <cod>tile</code>
     */
    public Ilk getIlk(Tile tile, Aim direction) {
        return getIlk(getTile(tile, direction));
    }

    private void setIlkModOK(int r, int c, Ilk ilk) {
        //if ((map[r][c]==Ilk.EXPLORATION_FOOD)||(ilk==Ilk.EXPLORATION_FOOD)||(map[r][c]==Ilk.PLANNED_EXPLORATION_FOOD)||(ilk==Ilk.PLANNED_EXPLORATION_FOOD)) {
        //	LogFile.write("T("+r+","+c+") "+map[r][c]+"->"+ilk);
        //}
        map[r][c] = ilk;
    }

    public void setIlk(int row, int col, Ilk ilk) {
        int r = (row + safeModShift) % rows, c = (col + safeModShift) % cols;
        setIlkModOK(r, c, ilk);
    }

    /**
     * Sets ilk at the specified location.
     *
     * @param tile location on the game map
     * @param ilk  ilk to be set at <code>tile</code>
     */
    public void setIlk(Tile tile, Ilk ilk) {
        setIlkModOK(tile.getRow(), tile.getCol(), ilk);
    }

    private boolean enemyFree[][];

    private boolean getEnemyFreeModOK(int r, int c) {
        return enemyFree[r][c];
    }

    public boolean getEnemyFree(int row, int col) {
        int r = (row + safeModShift) % rows, c = (col + safeModShift) % cols;
        return getEnemyFreeModOK(r, c);
    }

    public boolean getEnemyFree(Tile tile) {
        return getEnemyFreeModOK(tile.getRow(), tile.getCol());
    }

    private void setEnemyFreeModOK(int r, int c, boolean isFree) {
        //LogFile.write("enemyFree["+r+","+c+"]="+isFree);
        enemyFree[r][c] = isFree;
    }

    public void setEnemyFree(int row, int col, boolean isFree) {
        int r = (row + safeModShift) % rows, c = (col + safeModShift) % cols;
        setEnemyFreeModOK(r, c, isFree);
    }

    public void setEnemyFree(Tile tile, boolean isFree) {
        setEnemyFreeModOK(tile.getRow(), tile.getCol(), isFree);
    }

    public void see(int row, int col) {
        int mrow = (row + rows) % rows, mcol = (col + cols) % cols;
        Ilk ilk;
        Tile t = new Tile(mrow, mcol);
        pondering.see(t);    // update EXPLORATION_FOOD before the symmetry is lost
        if (turn == 1) {// update of ants follows update of hills!
            if (getHill(mrow, mcol) == HillTypes.LAND) {
                setHill(mrow, mcol, HillTypes.NO_HILL);
            }
        }
        ilk = getIlk(mrow, mcol);
        if (getLastSeen(mrow, mcol) < 0) {
            pondering.newlySeen(t);
            if (ilk == Ilk.SYMMETRY_WATER) {// water update preceeds ants update
                //LogFile.write("ant seeing SYMMETRY_WATER after WATTER update");
                pondering.collision(t);
                setIlk(mrow, mcol, Ilk.LAND);
            } else if ((ilk == Ilk.SYMMETRY_LAND) || (ilk == Ilk.UNKNOWN)) {
                setIlk(mrow, mcol, Ilk.LAND);
                pondering.newKnowledge();
            }
        }
        lastSeen[mrow][mcol] = turn;
        if (ilk != Ilk.WATER) {
            if (!getEnemyFree(mrow, mcol)) {
                setEnemyFree(mrow, mcol, true);
                frontLine.add(new Tile(mrow, mcol));
            }
        }
        //if ((ilk==Ilk.EXPECTED_FOOD)||(ilk==Ilk.SYMMETRY_FOOD)) {// expectedFoodTiles contains the location
        //	setIlk(mrow,mcol,Ilk.LAND);// update of ants preceeds update of food!
        //}
    }

    public void setMyAttackModOK(int r, int c, int d, Tile attacker) {
        myAttackLevel[r][c] = d;
        if (attacker == null) {
            return;
        }
        if (attackedByMy[r][c] == null) {
            attackedByMy[r][c] = new Tile[]{attacker};
        } else {
            Tile[] attackers = new Tile[attackedByMy[r][c].length + 1];
            attackers[0] = attacker;
            for (int i = 0; i < attackedByMy[r][c].length; i++) {
                if (attacker.equals(attackers[i + 1] = attackedByMy[r][c][i])) {
                    return;
                }
            }
            attackedByMy[r][c] = attackers;
        }
    }

    public void setMyAttack(int row, int col, int d, Tile attacker) {
        setMyAttackModOK((row + safeModShift) % rows, (col + safeModShift) % cols, d, attacker);
    }

    public void setMyAttack(Tile t, int d, Tile attacker) {
        setMyAttackModOK(t.getRow(), t.getCol(), d, attacker);
    }

    private void setEnemyAttackModOK(int r, int c, int d, Tile attacker) {
        enemyAttackLevel[r][c] = d;
        if (attacker == null) {
            return;
        }
        if (attackedByEnemy[r][c] == null) {
            attackedByEnemy[r][c] = new Tile[]{attacker};
        } else {
            Tile[] attackers = new Tile[attackedByEnemy[r][c].length + 1];
            attackers[0] = attacker;
            for (int i = 0; i < attackedByEnemy[r][c].length; i++) {
                if (attacker.equals(attackers[i + 1] = attackedByEnemy[r][c][i])) {
                    return;
                }
            }
            attackedByEnemy[r][c] = attackers;
        }
    }

    public void setEnemyAttack(int row, int col, int d, Tile attacker) {
        setEnemyAttackModOK((row + safeModShift) % rows, (col + safeModShift) % cols, d, attacker);
    }

    public void setEnemyAttack(Tile t, int d, Tile attacker) {
        setEnemyAttackModOK(t.getRow(), t.getCol(), d, attacker);
    }

    private int getMyAttackModOK(int r, int c) {
        return myAttackLevel[r][c];
    }

    public int getMyAttack(int row, int col) {
        return getMyAttackModOK((row + safeModShift) % rows, (col + safeModShift) % cols);
    }

    public int getMyAttack(Tile t) {
        return getMyAttack(t.getRow(), t.getCol());
    }

    private int getEnemyAttackModOK(int r, int c) {
        return enemyAttackLevel[r][c];
    }

    public int getEnemyAttack(int row, int col) {
        return getEnemyAttackModOK((row + safeModShift) % rows, (col + safeModShift) % cols);
    }

    public int getEnemyAttack(Tile t) {
        return getEnemyAttack(t.getRow(), t.getCol());
    }

    private Tile[] getAttackedByMyModOK(int r, int c) {
        return attackedByMy[r][c];
    }

    public Tile[] getAttackedByMy(Tile t) {
        return getAttackedByMyModOK(t.getRow(), t.getCol());
    }

    private Tile[] getAttackedByEnemyModOK(int r, int c) {
        return attackedByEnemy[r][c];
    }

    public Tile[] getAttackedByEnemy(Tile t) {
        return getAttackedByEnemyModOK(t.getRow(), t.getCol());
    }

    private void startMyAttackModOK(int r, int c) {
        if (myAttackLevel[r][c] < 0) {
            myAttackLevel[r][c] = 0;
            attackedByMy[r][c] = null;
            myAttackTiles.add(new Tile(r, c));
        }
    }

    public void startMyAttack(int row, int col) {
        startMyAttackModOK((row + rows) % rows, (col + cols) % cols);
    }

    public void startMyAttack(Tile t) {
        startMyAttackModOK(t.getRow(), t.getCol());
    }

    public void twoToMyAttackModOK(int r, int c, Tile attackedFrom) {
        startMyAttackModOK(r, c);
        setMyAttackModOK(r, c, getMyAttackModOK(r, c) + 2, attackedFrom);
    }

    public void twoToMyAttack(int row, int col, Tile attackedFrom) {
        twoToMyAttackModOK((row + rows) % rows, (col + cols) % cols, attackedFrom);
    }

    private void startEnemyAttackModOK(int r, int c) {
        if (enemyAttackLevel[r][c] < 0) {
            enemyAttackLevel[r][c] = 0;
            attackedByEnemy[r][c] = null;
            enemyAttackTiles.add(new Tile(r, c));
        }
    }

    public void startEnemyAttack(int row, int col) {
        startEnemyAttackModOK((row + rows) % rows, (col + cols) % cols);
    }

    public void startEnemyAttack(Tile t) {
        startEnemyAttackModOK(t.getRow(), t.getCol());
    }

    public void twoToEnemyAttackModOK(int r, int c, Tile attackedFrom) {
        startEnemyAttackModOK(r, c);
        setEnemyAttackModOK(r, c, getEnemyAttackModOK(r, c) + 2, attackedFrom);
    }

    public void twoToEnemyAttack(int row, int col, Tile attackedFrom) {
        twoToEnemyAttackModOK((row + rows) % rows, (col + cols) % cols, attackedFrom);
    }

    private void resetMyAttack() {// too many ants to reset each neighbourhood separately as for enemy ants
        for (int[] row : myAttackLevel) {
            Arrays.fill(row, -1);
        }
    }

    private void resetEnemyAttack() {// too many ants to reset each neighbourhood separately as for enemy ants
        for (int[] row : enemyAttackLevel) {
            Arrays.fill(row, -1);
        }
    }

    public void myAttackUpdate() {
        for (Tile myAttack = getMyAttackTiles().getFirstNoFilter(); myAttack != null; myAttack = getMyAttackTiles().getNextNoFilter()) {
            if (getMyAttack(myAttack) > 1) {
                Tile[] myAttackers = getAttackedByMy(myAttack);
                for (Aim direction : Aim.permAim[0]) {
                    Tile t = getTile(myAttack, direction);
                    startMyAttack(t);
                    int attack = getMyAttack(t);
                    if (attack <= 2) {
                        for (Tile attacker : myAttackers) {
                            if (attack <= 1) {
                                setMyAttack(t, 1, attacker);
                            } else {
                                setMyAttack(t, 2, attacker);
                            }
                        }
                    }
                }
            }
        }
        for (Tile myAttack = getMyAttackTiles().getFirstNoFilter(); myAttack != null; myAttack = getMyAttackTiles().getNextNoFilter()) {
            if (getMyAttack(myAttack) == 1) {
                Tile[] myAttackers = getAttackedByMy(myAttack);
                for (Aim direction : Aim.permAim[0]) {
                    Tile t = getTile(myAttack, direction);
                    startMyAttack(t);
                    if (getMyAttack(t) <= 0) {
                        for (Tile attacker : myAttackers) {
                            setMyAttack(t, 0, attacker);
                        }
                    }
                }
            }
        }
        for (Tile myAttack = getMyAttackTiles().getFirstNoFilter(); myAttack != null; myAttack = getMyAttackTiles().getNextNoFilter()) {
            int attack = getMyAttack(myAttack);
            Tile[] myAttackers = getAttackedByMy(myAttack);
            if ((attack >= 1) && (attack <= 2)) {
                if (myAttackers.length > 1) {
                    if (atLeast2PossibleAttackers(myAttack, Ilk.MY_ANT)) {
                        setMyAttack(myAttack, 3, null);
                    }
                }
            }
        }
    }

    public void enemyAttackUpdate() {
        for (Tile enemyAttack = getEnemyAttackTiles().getFirstNoFilter(); enemyAttack != null; enemyAttack = getEnemyAttackTiles().getNextNoFilter()) {
            if (getEnemyAttack(enemyAttack) > 1) {
                Tile[] enemyAttackers = getAttackedByEnemy(enemyAttack);
                for (Aim direction : Aim.permAim[0]) {
                    Tile t = getTile(enemyAttack, direction);
                    startEnemyAttack(t);
                    int attack = getEnemyAttack(t);
                    if (attack <= 2) {
                        for (Tile attacker : enemyAttackers) {
                            if (attack <= 1) {
                                setEnemyAttack(t, 1, attacker);
                            } else {
                                setEnemyAttack(t, 2, attacker);
                            }
                        }
                    }
                }
            }
        }
        for (Tile enemyAttack = getEnemyAttackTiles().getFirstNoFilter(); enemyAttack != null; enemyAttack = getEnemyAttackTiles().getNextNoFilter()) {
            if (getEnemyAttack(enemyAttack) == 1) {
                Tile[] enemyAttackers = getAttackedByEnemy(enemyAttack);
                for (Aim direction : Aim.permAim[0]) {
                    Tile t = getTile(enemyAttack, direction);
                    startEnemyAttack(t);
                    if (getEnemyAttack(t) <= 0) {
                        for (Tile attacker : enemyAttackers) {
                            setEnemyAttack(t, 0, attacker);
                        }
                    }
                }
            }
        }
        for (Tile enemyAttack = getEnemyAttackTiles().getFirstNoFilter(); enemyAttack != null; enemyAttack = getEnemyAttackTiles().getNextNoFilter()) {
            int attack = getEnemyAttack(enemyAttack);
            if ((attack >= 1) && (attack <= 2)) {
                Tile[] enemyAttackers = getAttackedByEnemy(enemyAttack);
                if (enemyAttackers.length > 1) {
                    if (atLeast2PossibleAttackers(enemyAttack, Ilk.ENEMY_ANT)) {
                        setEnemyAttack(enemyAttack, 3, null);
                    }
                }
            }
        }
    }

    public void attackUpdate() {
        LogFile.write("attack update " + getTimeRemaining());
        enemyAttackUpdate();
        myAttackUpdate();
        for (Tile enemyAttack = getEnemyAttackTiles().getFirstNoFilter(); enemyAttack != null; enemyAttack = getEnemyAttackTiles().getNextNoFilter()) {
            int attack = getEnemyAttack(enemyAttack);
            Tile[] attackers = getAttackedByEnemy(enemyAttack);
            if ((attack >= 1) && (attack <= 2)) {
                boolean attackPlaceFound = false;
                for (Tile attacker : attackers) {
                    for (Aim direction : Aim.permAim[0]) {
                        Tile attackPlace = getTile(attacker, direction);
                        Ilk attackPlaceIlk = getIlk(attackPlace);
                        if ((attackPlaceIlk == Ilk.DEAD) || (attackPlaceIlk == Ilk.LAND) || (attackPlaceIlk == Ilk.ENEMY_ANT)) {// could be stepped to
                            if (getMyAttack(attackPlace) < 3) {
                                if (getDistance2(attackPlace, enemyAttack) <= attackRadius2) {
                                    attackPlaceFound = true;
                                    //LogFile.write("small attack "+enemyAttack+" attacked by "+ attacker+" from "+
                                    //	attackPlace+" "+attackPlaceIlk+"("+getMyAttack(attackPlace)+")"+getDistance2(attackPlace,enemyAttack));
                                    break;
                                }
                            }
                        }
                    }
                    if (attackPlaceFound) {
                        break;
                    }
                }
                if (!attackPlaceFound) {//this place seems to be safe
                    setEnemyAttack(enemyAttack, 0, null);
                }
                //} else if (attack<=4) {
                //boolean attackPlaceFound=false;
                //for (Aim direction : Aim.permAim[0]) {
                //	Tile attackPlace=getTile(attacker,direction);
                //	Ilk attackPlaceIlk=getIlk(attackPlace);
                //	if ((attackPlaceIlk==Ilk.DEAD)||(attackPlaceIlk==Ilk.LAND)||(attackPlaceIlk==Ilk.ENEMY_ANT)) {// could be stepped to
                //		if (getMyAttack(attackPlace)<6) {
                //			if (getDistance2(attackPlace,enemyAttack)<=attackRadius2) {
                //				attackPlaceFound=true;
                //				//LogFile.write("small attack "+enemyAttack+" attacked by "+ attacker+" from "+
                //				//	attackPlace+" "+attackPlaceIlk+"("+getMyAttack(attackPlace)+")"+getDistance2(attackPlace,enemyAttack));
                //				break;
                //			}
                //		}
                //	} else if (attackPlaceIlk==Ilk.ENEMY_ANT) {
                //		if (getMyAttack(attackPlace)<3) {
                //			if (getDistance2(attackPlace,enemyAttack)<=attackRadius2) {
                //				attackPlaceFound=true;
                //				//LogFile.write("small attack "+enemyAttack+" attacked by "+ attacker+" from "+
                //				//	attackPlace+" "+attackPlaceIlk+"("+getMyAttack(attackPlace)+")"+getDistance2(attackPlace,enemyAttack));
                //				break;
                //			}
                //		}
                //	}
                //}
                //if (!attackPlaceFound) {//this place seems to be safe ... using frontal attack
                //	setEnemyAttack(enemyAttack,0,attacker);
                //}
            }
        }
        for (Tile myAttack = getMyAttackTiles().getFirstNoFilter(); myAttack != null; myAttack = getMyAttackTiles().getNextNoFilter()) {
            int attack = getMyAttack(myAttack);
            Tile[] attackers = getAttackedByMy(myAttack);
            if ((attack >= 1) && (attack <= 2)) {
                boolean attackPlaceFound = false;
                for (Tile attacker : attackers) {
                    for (Aim direction : Aim.permAim[0]) {
                        Tile attackPlace = getTile(attacker, direction);
                        Ilk attackPlaceIlk = getIlk(attackPlace);
                        //LogFile.write("testing "+myAttack+" attacker "+attacker+" atatck place "+attackPlace+" "+attackPlaceIlk+getMyAttack(attackPlace));
                        if ((attackPlaceIlk == Ilk.DEAD) || (attackPlaceIlk == Ilk.LAND) || (attackPlaceIlk == Ilk.MY_ANT)) {// could be stepped to
                            if (getEnemyAttack(attackPlace) < 3) {
                                if (getDistance2(attackPlace, myAttack) <= attackRadius2) {
                                    attackPlaceFound = true;
                                    //LogFile.write("small attack "+myAttack+" attacked by "+ attacker+" from "+
                                    //	attackPlace+" "+attackPlaceIlk+"("+getMyAttack(attackPlace)+")"+getDistance2(attackPlace,myAttack));
                                    break;
                                }
                            }
                        }
                    }
                    if (attackPlaceFound) {
                        break;
                    }
                }
                if (!attackPlaceFound) {//this place seems to be safe
                    //LogFile.write("no attack place found for "+myAttack);
                    setMyAttack(myAttack, 0, null);
                }
                //} else if (attack<=4) {
                //	boolean attackPlaceFound=false;
                //	for (Aim direction : Aim.permAim[0]) {
                //		Tile attackPlace=getTile(attacker,direction);
                //		Ilk attackPlaceIlk=getIlk(attackPlace);
                //		if ((attackPlaceIlk==Ilk.DEAD)||(attackPlaceIlk==Ilk.LAND)) {// could be stepped to
                //			if (getMyAttack(attackPlace)<6) {
                //				if (getDistance2(attackPlace,myAttack)<=attackRadius2) {
                //					attackPlaceFound=true;
                //					//LogFile.write("small attack "+myAttack+" attacked by "+ attacker+" from "+
                //					//	attackPlace+" "+attackPlaceIlk+"("+getMyAttack(attackPlace)+")"+getDistance2(attackPlace,myAttack));
                //					break;
                //				}
                //			}
                //		} else if (attackPlaceIlk==Ilk.MY_ANT) {
                //			if (getMyAttack(attackPlace)<3) {
                //				if (getDistance2(attackPlace,myAttack)<=attackRadius2) {
                //					attackPlaceFound=true;
                //					//LogFile.write("small attack "+myAttack+" attacked by "+ attacker+" from "+
                //					//	attackPlace+" "+attackPlaceIlk+"("+getMyAttack(attackPlace)+")"+getDistance2(attackPlace,myAttack));
                //					break;
                //				}
                //			}
                //		}
                //	}
                //	if (!attackPlaceFound) {//this place seems to be safe
                //		setMyAttack(myAttack,0,attacker);
                //	}
            }
        }
        LogFile.write("attack update ends: " + getTimeRemaining());
    }

    public void setHill(int row, int col, HillTypes hill) {
        hills[(row + rows) % rows][(col + cols) % cols] = hill;
    }

    public void setHill(Tile tile, HillTypes hill) {
        hills[tile.getRow()][tile.getCol()] = hill;
    }

    /**
     * Returns location in the specified direction from the specified location.
     *
     * @param tile      location on the game map
     * @param direction direction to look up
     * @return location in <code>direction</code> from <cod>tile</code>
     */
    public Tile getTile(Tile tile, Aim direction) {
        int row = (tile.getRow() + direction.getRowDelta() + safeModShift) % rows;
        int col = (tile.getCol() + direction.getColDelta() + safeModShift) % cols;
        return new Tile(row, col);
    }

    public TileList getMyAttackTiles() {// trims myAnts before return
        return myAttackTiles;
    }

    public TileList getEnemyAttackTiles() {// trims myAnts before return
        return enemyAttackTiles;
    }

    private TileList myAnts;

    /**
     * Returns a set containing all my ants locations.
     *
     * @return a set containing all my ants locations
     */
    public TileList getMyAnts() {// trims myAnts before return
        return myAnts;
    }

    private TileList myTmpAnts;

    public TileList getMyTmpAnts() {// trims myAnts before return
        return myTmpAnts;
    }

    private TileList myAntsPlanned;

    public TileList getMyAntsPlanned() {
        return myAntsPlanned;
    }

    private TileList enemyAnts;

    /**
     * Returns a set containing all enemy ants locations.
     *
     * @return a set containing all enemy ants locations
     */
    public TileList getEnemyAnts() {
        return enemyAnts;
    }

    private TileList myHills;

    /**
     * Returns a set containing all my hills locations.
     *
     * @return a set containing all my hills locations
     */
    public TileList getMyHills() {
        return myHills;
    }

    private TileList myExpectedHills;

    /**
     * Returns a set containing all my expected hills locations.
     *
     * @return a set containing all my expected hills locations
     */
    public TileList getMyExpectedHills() {
        return myExpectedHills;
    }

    private TileList enemyHills;

    /**
     * Returns a set containing all enemy hills locations.
     *
     * @return a set containing all enemy hills locations
     */
    public TileList getEnemyHills() {
        return enemyHills;
    }

    private TileList deadHills; //hills I have seen which does not exist now

    public TileList getDeadHills() {
        return deadHills;
    }

    private TileList enemyExpectedHills;

    /**
     * Returns a set containing all expected enemy hills locations.
     *
     * @return a set containing all expected enemy hills locations
     */
    public TileList getEnemyExpectedHills() {
        return enemyExpectedHills;
    }

    private TileList foodTiles;

    /**
     * Returns a set containing all food locations.
     *
     * @return a set containing all food locations
     */
    public TileList getFoodTiles() {
        return foodTiles;
    }

    private TileList plannedFoodTiles;

    /**
     * Returns a set containing all planned food locations.
     *
     * @return a set containing all planned food locations
     */
    public TileList getPlannedFoodTiles() {
        return plannedFoodTiles;
    }

    private TileList plannedSymmetryFoodTiles;

    /**
     * Returns a set containing all planned symmetry food locations.
     *
     * @return a set containing all planned symmetry food locations
     */
    public TileList getPlannedSymmetryFoodTiles() {
        return plannedSymmetryFoodTiles;
    }

    private TileList plannedExplorationFoodTiles;

    /**
     * Returns a set containing all planned exploration food locations.
     *
     * @return a set containing all planned exploration food locations
     */
    public TileList getPlannedExplorationFoodTiles() {
        return plannedExplorationFoodTiles;
    }

    private TileList deadAnts;

    /**
     * Returns a set containing all known dead ants locations.
     *
     * @return a set containing all known dead ants locations
     */
    public TileList getDeadAnts() {
        return deadAnts;
    }

    private TileList expectedFoodTiles;

    /**
     * Returns a set containing all expected food locations.
     *
     * @return a set containing all expected food locations
     */
    public TileList getExpectedFoodTiles() {
        return expectedFoodTiles;
    }

    private TileList symmetryFoodTiles;

    /**
     * Returns a set containing all symmetry food locations.
     *
     * @return a set containing all symmetry food locations
     */
    public TileList getSymmetryFoodTiles() {
        return symmetryFoodTiles;
    }

    private TileList explorationFoodTiles;

    /**
     * Returns a set containing all exploration food locations.
     *
     * @return a set containing all exploration food locations
     */
    public TileList getExplorationFoodTiles() {
        return explorationFoodTiles;
    }

    private TileList frontLine;

    /**
     * Returns a set containing all possible places where enemy ants could appear.
     *
     * @return a set containing all possible places where enemy ants could appear
     */
    public TileList getFrontLine() {
        return frontLine;
    }

    /**
     * Returns all orders sent so far.
     *
     * @return all orders sent so far
     */
    public List<Order> getOrders() {
        return orders;
    }

    /**
     * Calculates distance between two locations on the game map.
     * this is not the walking distance!
     *
     * @param t1 one location on the game map
     * @param t2 another location on the game map
     * @return distance between <code>t1</code> and <code>t2</code>
     */
    public int getDistance2(Tile t1, Tile t2) {
        int rowDelta = Math.abs(t1.getRow() - t2.getRow());
        int colDelta = Math.abs(t1.getCol() - t2.getCol());
        rowDelta = Math.min(rowDelta, rows - rowDelta);
        colDelta = Math.min(colDelta, cols - colDelta);
        return rowDelta * rowDelta + colDelta * colDelta;
    }

    public int getDistance1(Tile t1, Tile t2) {
        int rowDelta = Math.abs(t1.getRow() - t2.getRow());
        int colDelta = Math.abs(t1.getCol() - t2.getCol());
        rowDelta = Math.min(rowDelta, rows - rowDelta);
        colDelta = Math.min(colDelta, cols - colDelta);
        return rowDelta + colDelta;
    }

    public Tile targetNeighbour;

    public int travelDistance(Tile t1, Tile t2, int infty) {// distance avoiding watter. Going through enemy ants is OK
        int dist = 0;
        search.clear();
        search.addNotVisited(t1);
        boolean insertBreak = true;
        while (!search.isEmpty()) {
            if (getTimeRemaining() < 50) break;
            if (insertBreak) {
                search.addBreak();
                insertBreak = false;
            }
            targetNeighbour = search.remove();
            if (targetNeighbour.equals(Search.breakTile)) {
                dist++;
                insertBreak = true;
            } else {
                if (getDistance1(targetNeighbour, t2) + dist < infty) {
                    int permId = (Math.abs((int) random.nextLong()) % 24);
                    for (Aim direction : Aim.permAim[permId]) {
                        Tile moved = getTile(targetNeighbour, direction);
                        if (moved.equals(t2)) return dist + 1;
                        if (isPassableOnDangerLevel(moved)) {
                            search.addNotVisited(moved);
                        }
                    }
                }
            }
        }
        return infty;
    }

    /**
     * @param t1
     * @param ilk
     * @param infty
     * @return nearest tile of type ilk and it's neighbour in the direction
     */
    public Tile Nearest(Tile t1, Ilk ilk, int infty, boolean delayingAllowed) {
        int dist = 0;
        search.clear();
        search.addNotVisited(t1);
        boolean insertBreak = true;
        while (!search.isEmpty() && (dist < infty)) {
            if (getTimeRemaining() <= 0) break;
            if (insertBreak) {
                search.addBreak();
                insertBreak = false;
            }
            targetNeighbour = search.remove();
            if (targetNeighbour.equals(Search.breakTile)) {
                dist++;
                insertBreak = true;
            } else {
                int permId = (Math.abs((int) random.nextLong()) % 24);
                for (Aim direction : Aim.permAim[permId]) {
                    Tile moved = getTile(targetNeighbour, direction);
                    if (getIlk(moved).equals(ilk)) {
                        return moved;
                    }
                    if (isPassableOnDangerLevel(moved)) {
                        if (ilk != Ilk.MY_ANT) {// searching from ants
                            if (mybot.prefereStaying(targetNeighbour, moved) && delayingAllowed) {
                                search.addNotVisitedDelayed(moved);
                                //LogFile.write("Delaying from ant ... "+moved);
                            } else {
                                search.addNotVisited(moved);
                            }
                        } else {// searching to ants
                            if (mybot.prefereStaying(moved, targetNeighbour) && delayingAllowed) {
                                search.addNotVisitedDelayed(moved);
                                //LogFile.write("Delaying to ant ... "+moved);
                            } else {
                                search.addNotVisited(moved);
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * @param t1
     * @param infty
     * @return all myAnts in the neighbourhood
     */
    public Queue<Tile> myNearbyAnts(Tile t1, int infty) {// distance avoiding watter. Going through enemy ants is OK
        Queue<Tile> result = new LinkedList<Tile>();
        int dist = 0;
        search.clear();
        search.addNotVisited(t1);
        boolean insertBreak = true;
        while (!search.isEmpty() && (dist < infty)) {
            if (getTimeRemaining() < 50) break;
            if (insertBreak) {
                search.addBreak();
                insertBreak = false;
            }
            targetNeighbour = search.remove();
            if (targetNeighbour.equals(Search.breakTile)) {
                dist++;
                insertBreak = true;
            } else {
                int permId = (Math.abs((int) random.nextLong()) % 24);
                for (Aim direction : Aim.permAim[permId]) {
                    Tile moved = getTile(targetNeighbour, direction);
                    if (search.notVisited(moved)) {
                        if (getIlk(moved).equals(Ilk.MY_ANT)) {
                            result.add(moved);
                            result.add(targetNeighbour);
                            // pairs ... my ant, place to go ... be carefull not to colide
                        }
                        if (isPassableOnDangerLevel(moved)) {
                            search.addNotVisited(moved);
                        }
                    }
                }
            }
        }
        return result;
    }

    /**
     * Returns one or two orthogonal directions from one location to the
     * another.
     *
     * @param t1 one location on the game map
     * @param t2 another location on the game map
     * @return orthogonal directions from <code>t1</code> to <code>t2</code>
     */
    public List<Aim> getDirections(Tile t1, Tile t2) {// TODO simplify
        List<Aim> directions = new ArrayList<Aim>();
        if (t1.getRow() < t2.getRow()) {
            if (t2.getRow() - t1.getRow() >= rows / 2) {
                directions.add(Aim.NORTH);
            } else {
                directions.add(Aim.SOUTH);
            }
        } else if (t1.getRow() > t2.getRow()) {
            if (t1.getRow() - t2.getRow() >= rows / 2) {
                directions.add(Aim.SOUTH);
            } else {
                directions.add(Aim.NORTH);
            }
        }
        if (t1.getCol() < t2.getCol()) {
            if (t2.getCol() - t1.getCol() >= cols / 2) {
                directions.add(Aim.WEST);
            } else {
                directions.add(Aim.EAST);
            }
        } else if (t1.getCol() > t2.getCol()) {
            if (t1.getCol() - t2.getCol() >= cols / 2) {
                directions.add(Aim.EAST);
            } else {
                directions.add(Aim.WEST);
            }
        }
        return directions;
    }

    /**
     * Returns direction from t1 to t2 of neighbouring tiles
     *
     * @param t1
     * @param t2
     * @return
     */
    public Aim getDirection(Tile t1, Tile t2) {
        for (Aim direction : Aim.values()) {
            Tile test = getTile(t1, direction);
            if (test.equals(t2)) {
                return direction;
            }
        }
        //LogFile.write("getDirection failed "+t1+"->"+t2);
        return null;
    }

    public void frontLineUpdate(TileList tmpFree, TileList tmpNotFree) {
        LogFile.write("frontLineUpdate: " + getTimeRemaining());
        if (!mybot.maintainFrontLine) {
            frontLine.clear();
            return;
        }
        for (Tile front = frontLine.getFirstNoFilter(); front != null; front = frontLine.getNextNoFilter()) {
            if (getEnemyFree(front)) {
                tmpFree.add(front);
            } else {
                tmpNotFree.add(front);
            }
        }
        frontLine.clear();
        for (Tile front = tmpNotFree.getFirstNoFilter(); front != null; front = tmpNotFree.getNextNoFilter()) {
            boolean delayed = false;
            if (getTimeRemaining() < 100) return;
            for (Aim direction : Aim.permAim[0]) {
                Tile moved = getTile(front, direction);
                Ilk ilk = getIlk(moved);
                if (ilk.isStepable()) {
                    if (getEnemyFree(moved)) {
                        frontLine.add(moved);
                        setEnemyFree(moved, false);
                    }
                } else if ((ilk == Ilk.FOOD) || (ilk == Ilk.PLANNED_FOOD)) {
                    delayed = true;
                }
            }
            if (delayed) {
                if (getIlk(front).isStepable()) {
                    frontLine.add(front);
                }
            }
        }
        tmpNotFree.clear();
        for (Tile front = tmpFree.getFirstNoFilter(); front != null; front = tmpFree.getNextNoFilter()) {
            boolean free = true;
            if (getTimeRemaining() < 100) return;
            if (!getEnemyFree(front)) {
                continue;
            }
            for (Aim direction : Aim.permAim[0]) {
                Tile moved = getTile(front, direction);
                Ilk ilk = getIlk(moved);
                if (ilk.isStepable()) {
                    if (!getEnemyFree(moved)) {
                        free = false;
                        break;
                    }
                }
            }
            if (!free) {
                if (getEnemyFree(front)) {
                    frontLine.add(front);
                }
            }
        }
        tmpFree.clear();
        for (Tile front = frontLine.getFirstNoFilter(); front != null; front = frontLine.getNextNoFilter()) {
            if (getTimeRemaining() < 100) return;
            setEnemyFree(front, false);
        }
    }

    /**
     * Clears game map state information
     */
    public void clearMap() {
        for (Tile myAnt = myAnts.getFirstFilter(); myAnt != null; myAnt = myAnts.getNextFilter()) {
            //MY_ANT
            setIlk(myAnt, Ilk.MY_ANT_PLANNED);
            myAntsPlanned.add(myAnt);
        }
        myAnts.clear();
        for (Tile myAnt = myTmpAnts.getFirstFilter(); myAnt != null; myAnt = myTmpAnts.getNextFilter()) {
            //MY_TMP_ANT
            setIlk(myAnt, Ilk.MY_ANT_PLANNED);
            myAntsPlanned.add(myAnt);
        }
        myTmpAnts.clear();
        resetMyAttack();
        myAttackTiles.clear();
        resetEnemyAttack();
        enemyAttackTiles.clear();
        for (Tile food = foodTiles.getFirstFilter(); food != null; food = foodTiles.getNextFilter()) {
            //FOOD:
            setIlk(food, Ilk.EXPECTED_FOOD);
            expectedFoodTiles.add(food);
        }
        foodTiles.clear();
        for (Tile food = plannedFoodTiles.getFirstFilter(); food != null; food = plannedFoodTiles.getNextFilter()) {
            //PLANNED_FOOD
            setIlk(food, Ilk.EXPECTED_FOOD);
            expectedFoodTiles.add(food);
        }
        plannedFoodTiles.clear();
        for (Tile food = plannedSymmetryFoodTiles.getFirstFilter(); food != null; food = plannedSymmetryFoodTiles.getNextFilter()) {
            //PLANNED_SYMMETRY_FOOD
            setIlk(food, Ilk.SYMMETRY_FOOD);
            symmetryFoodTiles.add(food);
        }
        plannedSymmetryFoodTiles.clear();
        for (Tile explFood = explorationFoodTiles.getFirstFilter(); explFood != null; explFood = explorationFoodTiles.getNextFilter()) {
            //EXPLORATION_FOOD
            setIlk(explFood, Ilk.PLANNED_EXPLORATION_FOOD);
            plannedExplorationFoodTiles.add(explFood);
        }
        explorationFoodTiles.clear();
        for (Tile dead = deadAnts.getFirstFilter(); dead != null; dead = deadAnts.getNextFilter()) {
            //DEAD
            setIlk(dead, Ilk.LAND);
        }
        deadAnts.clear();
        for (Tile enemy = enemyAnts.getFirstFilter(); enemy != null; enemy = enemyAnts.getNextFilter()) {
            setIlk(enemy, Ilk.LAND);
        }
        enemyAnts.clear();
        for (Tile myHill = myHills.getFirstFilter(); myHill != null; myHill = myHills.getNextFilter()) {
            //MY_HILL
            setHill(myHill, HillTypes.EXPECTED_MY_HILL);
            myExpectedHills.add(myHill);
        }
        myHills.clear();
        for (Tile enemyHill = enemyHills.getFirstFilter(); enemyHill != null; enemyHill = enemyHills.getNextFilter()) {
            //ENEMY_HILL,SYMMETRY_ENEMY_HILL
            if (getHill(enemyHill) == HillTypes.ENEMY_HILL) {
                setHill(enemyHill, HillTypes.EXPECTED_ENEMY_HILL);
            }
            enemyExpectedHills.add(enemyHill);
        }
        enemyHills.clear();
    }

    /**
     * Updates game state information about new ants and food locations.
     *
     * @param ilk  ilk to be updated
     * @param tile location on the game map to be updated
     */
    public void update(Ilk ilk, Tile tile) {
        Ilk oriIlk = getIlk(tile);
        setIlk(tile, ilk);
        switch (ilk) {
            case WATER:
                if ((oriIlk == Ilk.SYMMETRY_FOOD) || (oriIlk == Ilk.SYMMETRY_LAND)) {
                    //LogFile.write("WATER on "+oriIlk);
                    pondering.collision(tile);
                }
                if (oriIlk == Ilk.UNKNOWN) {
                    pondering.newKnowledge();
                }
                break;
            case FOOD:
                if ((oriIlk != Ilk.EXPECTED_FOOD) && (oriIlk != Ilk.SYMMETRY_FOOD)) {
                    //LogFile.write("Food discovery: ("+tile.getRow()+","+tile.getCol()+") on "+oriIlk);
                    foodDiscovery[tile.getRow()][tile.getCol()] = turn;
                }
                foodTiles.add(tile);
                break;
            case MY_ANT:
                myAnts.add(tile);
                if (oriIlk != Ilk.MY_ANT_PLANNED) {
                    myHive--;
                    //LogFile.write("myHive-- ("+tile.getRow()+","+tile.getCol()+"):"+myHive);
                    if (hills[tile.getRow()][tile.getCol()] != HillTypes.MY_HILL) {
                        LogFile.write("hive error at [" + tile.getRow() + "][" + tile.getCol() + "]");
                    }
                }
                break;
            case ENEMY_ANT:
                enemyAnts.add(tile);
                break;
        }
    }

    /**
     * Updates game state information about hills locations.
     *
     * @param owner owner of hill
     * @param tile  location on the game map to be updated
     */
    public void updateHills(int owner, Tile tile) {
        if (owner > 0) {
            if (owner > maxOwner) {
                maxOwner = owner;
                if (maxOwner > 1) {
                    safeLevel = 1;
                }
            }
            enemyHills.add(tile);
            hills[tile.getRow()][tile.getCol()] = HillTypes.ENEMY_HILL;
        } else {
            myHills.add(tile);
            hills[tile.getRow()][tile.getCol()] = HillTypes.MY_HILL;
        }
    }

    public boolean atLeast2PossibleAttackers(Tile tile, Ilk attackerIlk) {
        Tile[] attackers;
        if (attackerIlk == Ilk.MY_ANT) {
            attackers = this.getAttackedByMy(tile);
        } else {
            attackers = this.getAttackedByEnemy(tile);
        }
        int[] attackPlaceCount = new int[attackers.length];
        Arrays.fill(attackPlaceCount, 0);
        Tile[] anAttackPlace = new Tile[attackers.length];
        Arrays.fill(anAttackPlace, null);
        for (int i = 0; i < attackers.length; i++) {
            for (Aim dir : Aim.permAim[0]) {
                Tile moved = getTile(attackers[i], dir);
                Ilk movedIlk = getIlk(moved);
                if (movedIlk.isStepable() || (movedIlk == attackerIlk)) {
                    if (getDistance2(moved, tile) <= attackRadius2) {
                        attackPlaceCount[i]++;
                        anAttackPlace[i] = moved;
                    }
                }
            }
        }
        int attackersCnt = 0, doubleAttackersCnt = 0;
        for (int i = 0; i < attackers.length; i++) {
            if (attackPlaceCount[i] > 0) {
                attackersCnt++;
            }
            if (attackPlaceCount[i] > 1) {
                doubleAttackersCnt++;
            }
        }
        if (attackersCnt < 2) return false;
        if (doubleAttackersCnt > 0) return true;
        Tile attackerPlace = null;
        for (int i = 0; i < attackers.length; i++) {
            if (attackPlaceCount[i] > 0) {
                if (attackerPlace == null) {
                    attackerPlace = anAttackPlace[i];
                } else {
                    if (!attackerPlace.equals(anAttackPlace[i])) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean isPassableOnDangerLevel(Tile tile) {
        if (!getIlk(tile).isPassable()) return false;
        if (enemyAttackSearchLevel < 99) {
            return (getEnemyAttack(tile) <= enemyAttackSearchLevel);
        } else {// not used
            return (getEnemyAttack(tile) < getMyAttack(tile));
        }
    }

    public int attackers(Tile tile) {
        int res = 0, row = tile.getRow(), col = tile.getCol();
        for (int i = -attackQCircle.length + 1; i < attackQCircle.length; i++) {
            for (int j = -attackQCircle[Math.abs(i)]; j <= attackQCircle[Math.abs(i)]; j++) {
                res += getIlk(row + i, col + j) == Ilk.ENEMY_ANT ? 1 : 0;
            }
        }
        return res;
    }

    /**
     * Issues an order by sending it to the system output.
     *
     * @param myAnt     map tile with my ant
     * @param direction direction in which to move my ant
     */
    public void issueOrder(Tile myAnt, Aim direction) {
        Ilk ilk;
        Tile nextAnt;
        if ((ilk = getIlk(myAnt, direction)).isStepable()) {
            Order order = new Order(myAnt, direction);
            orders.add(order);
            //System.out.println(order);
            LogFile.write(">" + order);
            if ((hills[myAnt.getRow()][myAnt.getCol()] == HillTypes.MY_HILL) && (myHive >= myHills.size())) {
                setIlk(myAnt, Ilk.MY_ANT_PLANNED);
                myAntsPlanned.add(myAnt);
                myHive--;
                //LogFile.write("MyHive-- hill ("+myAnt.getRow()+","+myAnt.getCol()+") >"+myHive);
            } else {
                setIlk(myAnt, Ilk.LAND);
            }
            setIlk(nextAnt = getTile(myAnt, direction), Ilk.MY_ANT_PLANNED);
            myAntsPlanned.add(nextAnt);
        } else {
            if (ilk == Ilk.FOOD || ilk == Ilk.PLANNED_FOOD) {
                setIlk(myAnt, Ilk.MY_ANT_PLANNED);
                myAntsPlanned.add(myAnt);
            }
        }
    }
}