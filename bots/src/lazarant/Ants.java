package lazarant;
import java.util.*;

/**
 * Holds all game data and current game state.
 */

public class Ants {
    /**
     * Maximum map size.
     */
    public static final int MAX_MAP_SIZE = 256 * 2;

    final int loadTime;

    final int turnTime;

    final int rows;

    final int cols;

    final int turns;

    final int viewRadius2;

    final int attackRadius2;

    final int spawnRadius2;

    final int controlRadius2 = 41;

    final Set<Tile> visionOffsets;

    final Set<Tile> attackOffsets;

    final Set<Tile> controlOffsets;

    final Map<Direction, Set<Tile>> additionalAttackOffsets;
    final Map<Direction, Set<Tile>> missingAttackOffsets;

    long turnStartTime;

    int turn = -1;

    final Ilk map[][];

    final Set<Tile> myAnts = new HashSet<Tile>();
    final Set<Tile> movedAnts = new HashSet<Tile>();

    final Set<Tile> enemyAnts = new HashSet<Tile>();
    Map<Tile, Integer> enemyColors = new HashMap<Tile, Integer>();

    final Set<Tile> firstLineAnts = new HashSet<Tile>();

    Set<Tile> myHills = new HashSet<Tile>();

    Set<Tile> enemyHills = new HashSet<Tile>();

    Set<Tile> foodTiles = new HashSet<Tile>();

    Set<Tile> waterTiles = new HashSet<Tile>();

    Set<Tile> dangerTiles = new HashSet<Tile>();

    final boolean visible[][];
    int visibleCells;
    int totalCells;

    Set<Tile> unseenTiles = new HashSet<Tile>();
    Set<Tile> neverSeenTiles = new HashSet<Tile>();
    Set<Tile> uncontrolledTiles = new HashSet<Tile>();

    Set<Tile> myAttackedTiles = new HashSet<Tile>();
    Set<Tile> enemyAttackedTiles = new HashSet<Tile>();

    Set<Tile> enemyAttackedForSureTiles = new HashSet<Tile>();


    Map<Tile, Set<Tile>> attackers = new HashMap<Tile, Set<Tile>>();
    Map<Tile, Set<Tile>> generalAttackers = new HashMap<Tile, Set<Tile>>();
    Map<Tile, Set<Tile>> friends = new HashMap<Tile, Set<Tile>>();

    Set<Tile> myHillsUnderAttack = new HashSet<Tile>();
    Set<Tile> hillAttackers = new HashSet<Tile>();

    public static int NEVER = -1000;
    final int lastSeen[][];
    final int lastControlled[][];

    final int blockCount[][];


    final int foodDist[][];
    final Tile foodDst[][];
    final int enemyHillDist[][];
    final Tile enemyHillDst[][];
    final int myHillDist[][];
    final Tile myHillDst[][];
    final int unseenDist[][];
    final Tile unseenDst[][];
    final int neverSeenDist[][];
    final Tile neverSeenDst[][];
    final int enemyDist[][];
    final Tile enemyDst[][];
    final int myAntsDist[][];
    final Tile myAntsDst[][];
    final int uncontrolledDist[][];
    final Tile uncontrolledDst[][];
    final int hillAttackersDist[][];
    final Tile hillAttackersDst[][];

    final int myAttackedAreaDist[][];
    final Tile myAttackedAreaDst[][];
    final int enemyAttackedAreaDist[][];
    final Tile enemyAttackedAreaDst[][];
    final int enemyAttackedForSureAreaDist[][];
    final Tile enemyAttackedForSureAreaDst[][];


    final int enemyAggression[][];
    final int myAggression[][];
    final int myControlCount[][];
    final int enemyControlCount[][];

    final static int UNREACHABLE = -1000;


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
    public Ants(int loadTime, int turnTime, int rows, int cols, int turns, int viewRadius2,
                int attackRadius2, int spawnRadius2) {
        this.loadTime = loadTime;
        this.turnTime = turnTime;
        this.rows = rows;
        this.cols = cols;
        this.turns = turns;
        this.viewRadius2 = viewRadius2;
        this.attackRadius2 = attackRadius2;
        this.spawnRadius2 = spawnRadius2;
        map = new Ilk[rows][cols];
        for (Ilk[] row : map) {
            Arrays.fill(row, Ilk.LAND);
        }
        visible = new boolean[rows][cols];
        for (boolean[] row : visible) {
            Arrays.fill(row, false);
        }

        lastSeen = new int[rows][cols];
        for (int[] row : lastSeen) {
            Arrays.fill(row, NEVER);
        }

        lastControlled = new int[rows][cols];
        for (int[] row : lastSeen) {
            Arrays.fill(row, NEVER);
        }

        blockCount = new int[rows][cols];
        for (int[] row : blockCount) {
            Arrays.fill(row, 0);
        }

        foodDist = new int[rows][cols];
        foodDst = new Tile[rows][cols];
        enemyHillDist = new int[rows][cols];
        enemyHillDst = new Tile[rows][cols];
        myHillDist = new int[rows][cols];
        myHillDst = new Tile[rows][cols];
        unseenDist = new int[rows][cols];
        unseenDst = new Tile[rows][cols];
        neverSeenDist = new int[rows][cols];
        neverSeenDst = new Tile[rows][cols];
        enemyDist = new int[rows][cols];
        enemyDst = new Tile[rows][cols];
        myAntsDist = new int[rows][cols];
        myAntsDst = new Tile[rows][cols];
        myAttackedAreaDist = new int[rows][cols];
        myAttackedAreaDst = new Tile[rows][cols];
        enemyAttackedAreaDist = new int[rows][cols];
        enemyAttackedAreaDst = new Tile[rows][cols];
        enemyAttackedForSureAreaDist = new int[rows][cols];
        enemyAttackedForSureAreaDst = new Tile[rows][cols];
        uncontrolledDist = new int[rows][cols];
        uncontrolledDst = new Tile[rows][cols];
        hillAttackersDist = new int[rows][cols];
        hillAttackersDst = new Tile[rows][cols];

        enemyAggression = new int[rows][cols];
        myAggression = new int[rows][cols];

        myControlCount = new int[rows][cols];
        enemyControlCount = new int[rows][cols];

        visionOffsets = calcOffsets(viewRadius2);
        attackOffsets = calcOffsets(attackRadius2);
        controlOffsets = calcOffsets(controlRadius2);

        additionalAttackOffsets = new HashMap<Direction, Set<Tile>>();
        for (Direction dir : Direction.values) {
            additionalAttackOffsets.put(dir, calcAdditionalAttackOffset(dir));
        }

        missingAttackOffsets = new HashMap<Direction, Set<Tile>>();
        for (Direction dir : Direction.values) {
            missingAttackOffsets.put(dir, calcMissingAttackOffset(dir));
        }
    }

    private Set<Tile> calcAdditionalAttackOffset(Direction dir) {
        Set<Tile> result = new HashSet<Tile>();
        for (Tile offset : attackOffsets) {
            Tile newOffset = getTile(offset, dir);
            if (!attackOffsets.contains(newOffset)) {
                result.add(newOffset);
            }
        }
        return result;
    }

    private Set<Tile> calcMissingAttackOffset(Direction dir) {
        Set<Tile> result = new HashSet<Tile>(attackOffsets);
        for (Tile offset : attackOffsets) {
            Tile newOffset = getTile(offset, dir);
            if (attackOffsets.contains(newOffset)) {
                result.remove(newOffset);
            }
        }
        return result;
    }

    private Set<Tile> calcOffsets(int radius2) {
        Set<Tile> result = new HashSet<Tile>();
        int mx = (int) Math.sqrt(radius2);
        for (int row = -mx; row <= mx; ++row) {
            for (int col = -mx; col <= mx; ++col) {
                int d = row * row + col * col;
                if (d <= radius2) {
                    result.add(createTile(row, col));
                }
            }
        }
        return result;
    }

    private Tile createTile(int row, int col) {
        row = (row + 16 * rows) % rows;
        col = (col + 16 * cols) % cols;
        return new Tile(row, col);
    }

    /**
     * Sets turn start time.
     *
     * @param turnStartTime turn start time
     */
    public void setTurnStartTime(long turnStartTime) {
        this.turnStartTime = turnStartTime;
    }

    /**
     * Returns how much time the bot has still has to take its turn before timing out.
     *
     * @return how much time the bot has still has to take its turn before timing out
     */
    public int getTimeRemaining() {
        return turnTime - (int) (System.currentTimeMillis() - turnStartTime);
    }

    /**
     * Returns location in the specified direction from the specified location.
     *
     * @param tile      location on the game map
     * @param direction direction to look up
     * @return location in <code>direction</code> from <cod>tile</code>
     */
    public Tile getTile(Tile tile, Direction direction) {
        return createTile(tile.row + direction.rowDelta, tile.col + direction.colDelta);
    }

    public Tile getTile(Tile tile, Tile offset) {
        return createTile(tile.row + offset.row, tile.col + offset.col);
    }

    public Tile calcOffset(Tile tile1, Tile tile2) {
        return createTile(tile1.row - tile2.row, tile1.col - tile2.col);
    }

    public int distanceMoves(Tile t1, Tile t2) {
        int rowDelta = Math.abs(t1.row - t2.row);
        int colDelta = Math.abs(t1.col - t2.col);
        rowDelta = Math.min(rowDelta, rows - rowDelta);
        colDelta = Math.min(colDelta, cols - colDelta);
        return rowDelta + colDelta;
    }

    public Direction getDirection(Tile src, Tile dst) {
        for (Direction dir : Direction.evalues) {
            Tile tile = getTile(src, dir);
            if (tile.equals(dst)) return dir;
        }
        return null;
    }


    /**
     * Clears game state information about my ants locations.
     */
    public void clearMyAnts() {
        for (Tile myAnt : myAnts) {
            map[myAnt.row][myAnt.col] = Ilk.LAND;
        }
        myAnts.clear();
        movedAnts.clear();
    }

    /**
     * Clears game state information about enemy ants locations.
     */
    public void clearEnemyAnts() {
        for (Tile enemyAnt : enemyAnts) {
            map[enemyAnt.row][enemyAnt.col] = Ilk.LAND;
        }
        enemyAnts.clear();
        enemyColors.clear();
    }


    private Set<Tile> previousFood = null;

    /**
     * Clears game state information about food locations.
     */
    public void clearFood() {
        for (Tile food : foodTiles) {
            map[food.row][food.col] = Ilk.LAND;
        }
        previousFood = foodTiles;
        foodTiles = new HashSet<Tile>();
    }

    public void addHiddenFood() {
        for (Tile food : previousFood) {
            if (!visible[food.row][food.col]) {
                addFood(food.row, food.col);
            }
        }
        previousFood = null;
    }


    private Set<Tile> previousMyHills = null;

    public void clearMyHills() {
        for (Tile hill : myHills) {
            map[hill.row][hill.col] = Ilk.LAND;
        }
        previousMyHills = myHills;
        myHills = new HashSet<Tile>();
    }

    public void addHiddenMyHills() {
        for (Tile hill : previousMyHills) {
            if (!visible[hill.row][hill.col]) {
                addHill(hill.row, hill.col, 0);
            }
        }
        previousMyHills = null;
    }


    private Set<Tile> previousEnemyHills = null;

    public void clearEnemyHills() {
        previousEnemyHills = enemyHills;
        enemyHills = new HashSet<Tile>();
    }

    public void addHiddenEnemyHills() {
        for (Tile hill : previousEnemyHills) {
            if (!visible[hill.row][hill.col]) {
                addHill(hill.row, hill.col, 1);
            }
        }
        previousEnemyHills = null;
    }

    /**
     * Clears visible information
     */
    public void clearVision() {
        for (int row = 0; row < rows; ++row) {
            for (int col = 0; col < cols; ++col) {
                visible[row][col] = false;
            }
        }
    }

    public void updateVision() {
        for (Tile antLoc : myAnts) {
            for (Tile locOffset : visionOffsets) {
                Tile newLoc = getTile(antLoc, locOffset);
                visible[newLoc.row][newLoc.col] = true;
                lastSeen[newLoc.row][newLoc.col] = turn;
            }
        }
        unseenTiles.clear();
        neverSeenTiles.clear();
        for (int row = 0; row < rows; ++row) {
            for (int col = 0; col < cols; ++col) {
                if (map[row][col] == Ilk.WATER) continue;
                if (lastSeen[row][col] + 10 < turn)
                    unseenTiles.add(new Tile(row, col));
                if (lastSeen[row][col] == NEVER)
                    neverSeenTiles.add(new Tile(row, col));

            }
        }

        visibleCells = 0;
        totalCells = 0;
        for (int row = 0; row < rows; ++row) {
            for (int col = 0; col < cols; ++col) {
                if (map[row][col] == Ilk.WATER) continue;
                totalCells++;
                if (visible[row][col]) visibleCells++;
            }
        }
    }

    public void updateControl() {
        for (Tile antLoc : myAnts) {
            for (Tile locOffset : controlOffsets) {
                Tile newLoc = getTile(antLoc, locOffset);
                lastControlled[newLoc.row][newLoc.col] = turn;
            }
        }
        uncontrolledTiles.clear();
        for (int row = 0; row < rows; ++row) {
            for (int col = 0; col < cols; ++col) {
                if (map[row][col] == Ilk.WATER) continue;
                if (lastControlled[row][col] + 7 < turn)
                    uncontrolledTiles.add(new Tile(row, col));
            }
        }
    }


    public void updateAttackedTiles() {
        updateAttackedTiles(myAttackedTiles, myAnts);
        updateAttackedTiles(enemyAttackedTiles, enemyAnts);

        enemyAttackedForSureTiles.clear();
        for (Tile ant : enemyAnts) {
            Set<Tile> offsets = new HashSet<Tile>(attackOffsets);
            for (Direction dir : Direction.values) {
                Tile tile = getTile(ant, dir);
                if (map[tile.row][tile.col].isPassable()) {
                    offsets.removeAll(missingAttackOffsets.get(dir));
                }
            }
            for (Tile attackOffset : offsets) {
                Tile newLoc = getTile(ant, attackOffset);

                enemyAttackedForSureTiles.add(newLoc);
            }
        }
    }

    private void updateAttackedTiles(Set<Tile> result, Set<Tile> ants) {
        result.clear();
        for (Tile ant : ants) {
            for (Tile attackOffset : attackOffsets) {
                Tile newLoc = getTile(ant, attackOffset);

                result.add(newLoc);
            }
        }
    }

    public void updateAttackersAndFriends() {
        //Update attackers
        attackers.clear();
        firstLineAnts.clear();
        for (Tile ant : myAnts) {

            for (Direction antDir : Direction.evalues) {
                Tile antTile = getTile(ant, antDir);
                if (!map[antTile.row][antTile.col].isPassable()) continue;

                for (Tile attackOffset : attackOffsets) {
                    Tile attackTile = getTile(antTile, attackOffset);
                    if (!map[attackTile.row][attackTile.col].isPassable()) continue;

                    for (Direction enemyReverseDir : Direction.evalues) {
                        Tile enemy = getTile(attackTile, enemyReverseDir);
                        if (!enemyAnts.contains(enemy)) continue;

                        if (antDir == Direction.NONE || enemyReverseDir == Direction.NONE) {
                            firstLineAnts.add(ant);
                            firstLineAnts.add(enemy);
                        }

                        Set<Tile> enemies = attackers.get(ant);
                        if (enemies == null) {
                            enemies = new HashSet<Tile>();
                            attackers.put(ant, enemies);
                        }
                        enemies.add(enemy);

                        Set<Tile> friends = attackers.get(enemy);
                        if (friends == null) {
                            friends = new HashSet<Tile>();
                            attackers.put(enemy, friends);
                        }
                        friends.add(ant);
                    }
                }
            }
        }

        //Update friends
        for (Tile ant : myAnts) {
            Set<Tile> enemies = attackers.get(ant);

            if (enemies == null) continue;

            Set<Tile> friends = new HashSet<Tile>();
            for (Tile enemy : enemies) {
                Set<Tile> localFriends = attackers.get(enemy);
                if (localFriends != null) friends.addAll(localFriends);
            }

            this.friends.put(ant, friends);
        }

        //Update general attackers
        generalAttackers.clear();
        for (Tile ant : enemyAnts) {

            for (Direction antDir : Direction.evalues) {
                Tile antTile = getTile(ant, antDir);
                if (!map[antTile.row][antTile.col].isPassable()) continue;

                for (Tile attackOffset : attackOffsets) {
                    Tile attackTile = getTile(antTile, attackOffset);
                    //if (!map[attackTile.row][attackTile.col].isPassable()) continue;

                    for (Direction enemyReverseDir : Direction.evalues) {
                        Tile enemy = getTile(attackTile, enemyReverseDir);
                        if (!enemyAnts.contains(enemy)) continue;

                        int cl1 = enemyColors.get(ant);
                        int cl2 = enemyColors.get(enemy);

                        if (cl1 == cl2) continue;

                        Set<Tile> enemies = generalAttackers.get(ant);
                        if (enemies == null) {
                            enemies = new HashSet<Tile>();
                            generalAttackers.put(ant, enemies);
                        }
                        enemies.add(enemy);

                        Set<Tile> friends = generalAttackers.get(enemy);
                        if (friends == null) {
                            friends = new HashSet<Tile>();
                            generalAttackers.put(enemy, friends);
                        }
                        friends.add(ant);
                    }
                }
            }
        }

    }

    public void updateAttackedHills() {
        myHillsUnderAttack.clear();
        hillAttackers.clear();

        for (Tile enemy : enemyAnts) {
            if (myHillDist[enemy.row][enemy.col] != Ants.UNREACHABLE && myHillDist[enemy.row][enemy.col] <= 10) {
                myHillsUnderAttack.add(myHillDst[enemy.row][enemy.col]);
                hillAttackers.add(enemy);
            }
        }

        updateDist(hillAttackersDist, hillAttackersDst, hillAttackers, false);
    }

    public void updateAggressions() {
        updateAgression(enemyAnts, enemyAggression);
        updateAgression(myAnts, myAggression);
    }

    private void updateAgression(Set<Tile> ants, int[][] aggression) {
        for (int[] row : aggression) {
            Arrays.fill(row, 0);
        }

        for (Tile ant : ants) {
            for (Tile offset : attackOffsets) {
                Tile tile = getTile(ant, offset);
                aggression[tile.row][tile.col] += 10;
            }
            for (Direction dir : Direction.values) {
                Tile newAnt = getTile(ant, dir);

                if (map[newAnt.row][newAnt.col].isPassable()) {
                    for (Tile offset : additionalAttackOffsets.get(dir)) {
                        Tile tile = getTile(ant, offset);
                        aggression[tile.row][tile.col] += 6;
                    }
                }
            }
        }
    }

    public void updateControlCount() {
        for (int[] row : myControlCount) {
            Arrays.fill(row, 0);
        }
        for (int[] row : enemyControlCount) {
            Arrays.fill(row, 0);
        }

        for (Tile ant : myAnts) {
            for (Tile offset : visionOffsets) {
                Tile tile = getTile(ant, offset);
                myControlCount[tile.row][tile.col]++;
            }
        }

        for (Tile ant : enemyAnts) {
            for (Tile offset : visionOffsets) {
                Tile tile = getTile(ant, offset);
                enemyControlCount[tile.row][tile.col]++;
            }
        }
    }

    public void updateDists() {
        updateDist(foodDist, foodDst, foodTiles, true);

        Set<Tile> possibleEnemyHills = new HashSet<Tile>(enemyHills);
        possibleEnemyHills.addAll(neverSeenTiles);
        updateDist(enemyHillDist, enemyHillDst, possibleEnemyHills, true);

        updateDist(myHillDist, myHillDst, myHills, false);
        updateDist(unseenDist, unseenDst, unseenTiles, true);
        updateDist(neverSeenDist, neverSeenDst, neverSeenTiles, true);
        updateDist(enemyDist, enemyDst, enemyAnts, false);
        updateDist(myAntsDist, myAntsDst, myAnts, false);
        updateDist(myAttackedAreaDist, myAttackedAreaDst, myAttackedTiles, false);
        updateDist(enemyAttackedAreaDist, enemyAttackedAreaDst, enemyAttackedTiles, false);
        updateDist(enemyAttackedForSureAreaDist, enemyAttackedForSureAreaDst, enemyAttackedForSureTiles, false);
        updateDist(uncontrolledDist, uncontrolledDst, uncontrolledTiles, true);
    }

    private void updateDist(int[][] dist, Tile[][] dst, Collection<Tile> startTiles, boolean skipBlocked) {
        for (int[] row : dist) {
            Arrays.fill(row, UNREACHABLE);
        }
        for (Tile[] row : dst) {
            Arrays.fill(row, null);
        }
        LinkedList<Tile> q = new LinkedList<Tile>();

        for (Tile tile : startTiles) {
            if (!map[tile.row][tile.col].isPassable()) continue;

            q.add(tile);
            dist[tile.row][tile.col] = 0;
            dst[tile.row][tile.col] = tile;
        }

        while (!q.isEmpty()) {
            Tile tile = q.removeFirst();
            for (Direction dir : Direction.values) {
                Tile newTile = getTile(tile, dir);
                if (dist[newTile.row][newTile.col] != UNREACHABLE) continue;
                if (!map[newTile.row][newTile.col].isPassable()) continue;
                dist[newTile.row][newTile.col] = dist[tile.row][tile.col] + 1;
                dst[newTile.row][newTile.col] = dst[tile.row][tile.col];

                //stop search
                if (myHills.contains(newTile)) continue;
                if (skipBlocked && blockCount[newTile.row][newTile.col] > 3) continue;
                q.add(newTile);
            }
        }

    }


    public void issueOrder(Order order) {
        movedAnts.add(order.tile);

        System.out.println(order.toCommand());
        System.out.flush();
    }

    public void addWater(int row, int col) {
        map[row][col] = Ilk.WATER;
        Tile water = new Tile(row, col);
        waterTiles.add(water);

        dangerTiles.remove(water);
        for (Direction dir : Direction.values) {
            Tile tile = getTile(water, dir);
            checkForDanger(tile);
        }

    }

    private void checkForDanger(Tile tile) {
        List<Direction> waterDirs = new ArrayList<Direction>();
        for (Direction dir : Direction.values) {
            Tile next = getTile(tile, dir);
            if (map[next.row][next.col] == Ilk.WATER) {
                waterDirs.add(dir);
            }
        }
        if (waterDirs.size() < 2) return;
        if (waterDirs.size() > 2) {
            dangerTiles.add(tile);
            return;
        }
        if ((waterDirs.get(1).ordinal() - waterDirs.get(0).ordinal()) % 2 == 1) {
            dangerTiles.add(tile);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void addAnt(int row, int col, int owner) {
        if (owner == 0) {
            map[row][col] = Ilk.MY_ANT;
            myAnts.add(new Tile(row, col));
        } else {
            map[row][col] = Ilk.ENEMY_ANT;
            Tile tile = new Tile(row, col);
            enemyAnts.add(tile);
            enemyColors.put(tile, owner);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void addFood(int row, int col) {
        map[row][col] = Ilk.FOOD;
        foodTiles.add(new Tile(row, col));
    }

    /**
     * {@inheritDoc}
     */
    public void addDeadAnt(int row, int col, int owner) {
        if (owner != 0) {
            Tile tile = new Tile(row, col);
            enemyColors.put(tile, owner);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void addHill(int row, int col, int owner) {
        if (owner > 0)
            enemyHills.add(new Tile(row, col));
        else {
            myHills.add(new Tile(row, col));
            map[row][col] = Ilk.MY_HILL;
        }
    }

    public void updateTurn() {
        turn++;
    }

    public void beforeUpdate() {
        updateTurn();
        setTurnStartTime(System.currentTimeMillis());
        clearMyAnts();
        clearEnemyAnts();
        clearMyHills();
        clearEnemyHills();
        clearFood();
        clearVision();
    }

    public void afterUpdate() {
        updateVision();
        addHiddenFood();
        addHiddenEnemyHills();
        addHiddenMyHills();
        updateAttackedTiles();
        updateControl();
        updateAggressions();
        updateControlCount();
        updateAttackersAndFriends();

        updateDists();
        updateAttackedHills(); //must be after updateDists
    }

    public void afterTurn() {
        updateBlockCount();
    }

    private void updateBlockCount() {
        for (int row = 0; row < rows; ++row) {
            for (int col = 0; col < cols; ++col) {
                Tile tile = new Tile(row, col);
                if (!myAnts.contains(tile) || movedAnts.contains(tile)) {
                    blockCount[row][col] = 0;
                } else {
                    blockCount[row][col]++;
                }
            }
        }
    }
}
