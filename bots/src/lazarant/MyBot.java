package lazarant;
import java.io.IOException;
import java.util.*;

/**
 * Starter bot implementation.
 */
public class MyBot extends Bot {
    /**
     * Main method executed by the game engine for starting the bot.
     *
     * @param args command line arguments
     * @throws IOException if an I/O error occurs
     */
    public static void main(String[] args) throws IOException {
        new MyBot().startGame();
    }

    private Set<Tile> attackIntensionUnprocessedAnts;

    private Map<Tile, MoveIntention> moveIntentions;

    private Map<Tile, Order> antOrders;

    private Map<Tile, Tile> foodMap;
    private Map<Tile, Tile> unseenMap;
    private Map<Tile, Tile> uncontrolledMap;

    StatisticsModule statisticsModule = null;

    Map<Tile, AttackIntension> attackIntensions;
    Map<Tile, Direction> attackDirections;

    private Set<Tile> hillDefenders = new HashSet<Tile>();

    boolean angryMode;

    private Random rnd = new Random(13);

    @Override
    protected void initialize() {
        statisticsModule = new StatisticsModule(this);
    }

    @Override
    public void doTurn() {
        debug("");
        debug("turn = " + ants.turn);
        debug("Time remaining at start of turn: " + ants.getTimeRemaining());

        antOrders = new LinkedHashMap<Tile, Order>();

        updateAngryMode();

        updateEnemyStatistics();

        ensureHills();

        createAttackIntensions();

        createAimMaps();

        moveIntentions = new HashMap<Tile, MoveIntention>();
        for (Tile ant : ants.myAnts) {
            MoveIntention moveIntention = makeDecision(ant);
            moveIntentions.put(ant, moveIntention);
        }

        for (Tile ant : ants.myAnts) {
            seenAnts1.clear();
            seenAnts2.clear();
            if (!processIntention(ant, true)) {
                throw new IllegalStateException();
            }
        }

        flushOrders();
    }

    private void updateAngryMode() {
        angryMode = false;

        if (ants.visibleCells * 100  < ants.totalCells * 67) return;
        if (ants.myAnts.size() < ants.enemyAnts.size() * 2) return;

        debug("***Angry mode!");

        angryMode = true;
    }

    Set<Tile> seenAnts1 = new HashSet<Tile>();
    Set<Tile> seenAnts2 = new HashSet<Tile>();

    private boolean processIntention(Tile ant, boolean forceMove) {
        if (forceMove) {
            if (seenAnts1.contains(ant)) return false;
            seenAnts1.add(ant);
        } else {
            if (seenAnts2.contains(ant)) return false;
            if (seenAnts1.contains(ant)) return false;
            seenAnts2.add(ant);
        }

        MoveIntention moveIntention = moveIntentions.get(ant);

        //Free move
        for (Direction move : moveIntention.moves) {
            Tile tile = ants.getTile(ant, move);
            if (!antOrders.containsKey(tile)) {
                antOrders.put(tile, new Order(ant, move, moveIntention.priority));
                return true;
            }
        }

        //Friends
        for (Direction move : moveIntention.moves) {
            Tile tile = ants.getTile(ant, move);
            Order order = antOrders.get(tile);

            if (order.tile.equals(ant)) continue;

            if (processIntention(order.tile, false)) {
                antOrders.put(tile, new Order(ant, move, moveIntention.priority));
                return true;
            }
        }

        //Force
        if (forceMove) {
            for (Direction move : moveIntention.moves) {
                Tile tile = ants.getTile(ant, move);
                Order order = antOrders.get(tile);

                if (order.tile.equals(ant)) continue;

                if (order.priority > moveIntention.priority) {
                    if (processIntention(order.tile, true)) {
                        antOrders.put(tile, new Order(ant, move, moveIntention.priority));
                        return true;
                    }
                }
            }
        }

        //No move
        if (forceMove) {
//            //Free move
//            for (Direction move : Direction.evalues) {
//                Tile tile = ants.getTile(ant, move);
//                if (!antOrders.containsKey(tile)) {
//                    antOrders.put(tile, new Order(ant, move, Integer.MAX_VALUE));
//                    return true;
//                }
//            }
//
            //Stay on ground
            Order order = antOrders.get(ant);
            antOrders.put(ant, new Order(ant, Direction.NONE, 0));

            if (order == null) return true;
            if (order.tile.equals(ant)) return false;

            return processIntention(order.tile, true);
        }

        return false;
    }

    private void updateEnemyStatistics() {
        statisticsModule.updateEnemyStatistics();
    }

    private void createAttackIntensions() {
        attackIntensions = new HashMap<Tile, AttackIntension>();
        attackIntensionUnprocessedAnts = new LinkedHashSet<Tile>();
        for (Tile ant : ants.myAnts) {
            if (ants.attackers.get(ant) != null) {
                attackIntensions.put(ant, AttackIntension.ATTACK);
                attackIntensionUnprocessedAnts.add(ant);
            }
        }

        while (!attackIntensionUnprocessedAnts.isEmpty()) {
            Tile ant = attackIntensionUnprocessedAnts.iterator().next();
            attackIntensionUnprocessedAnts.remove(ant);
            updateAttackIntension(ant);
        }

        updateAttackDirections();

    }

    private void updateAttackDirections() {
        attackDirections = new HashMap<Tile, Direction>();

        Map<Tile, Set<Direction>> possibleAttackDirections = new HashMap<Tile, Set<Direction>>();
        for (Tile ant : ants.myAnts) {
            AttackIntension attackIntension = attackIntensions.get(ant);
            if (attackIntension != null && attackIntension.isAggression()) {
                moveDirections = Arrays.asList(Direction.evalues);
                if (attackAntDecision(ant)) {
                    debug(ant + " " + moveDirections);
                    attackIntensionUnprocessedAnts.add(ant);
                    possibleAttackDirections.put(ant, new HashSet<Direction>(moveDirections));
                } else {
                    debug(ant + " SKIP");
                    attackIntensions.put(ant, AttackIntension.ESCAPE);
                }
            }

        }

        while (!attackIntensionUnprocessedAnts.isEmpty()) {
            Tile ant = attackIntensionUnprocessedAnts.iterator().next();
            attackIntensionUnprocessedAnts.remove(ant);

            Direction attackDirection = attackDirections.get(ant);
            Set<Direction> possibleAntAttackDirections = possibleAttackDirections.get(ant);
            if (attackDirection == null && possibleAntAttackDirections.size() == 1) {
                attackDirection = possibleAntAttackDirections.iterator().next();
                attackDirections.put(ant, attackDirection);
            }

            if (attackDirection != null) {
                possibleAntAttackDirections.remove(attackDirection);

                for (Tile friend : ants.friends.get(ant)) {
                    if (!attackIntensions.get(friend).isAggression()) continue;
                    Set<Direction> possibleFriendDirections = possibleAttackDirections.get(friend);
                    if (!possibleFriendDirections.contains(attackDirection)) continue;

                    attackDirections.put(friend, attackDirection);
                    attackIntensionUnprocessedAnts.add(friend);
                }
            }
        }
    }

    private void updateAttackIntension(Tile ant) {
        AttackIntension oldIntension = attackIntensions.get(ant);
        if (oldIntension == AttackIntension.ESCAPE) return;

        AttackIntension newIntension = calcAttackIntension(ant);

        if (newIntension != oldIntension) {
            attackIntensions.put(ant, newIntension);
            if (newIntension != AttackIntension.SKIP && newIntension.ordinal() > oldIntension.ordinal()) {
                attackIntensionUnprocessedAnts.addAll(ants.friends.get(ant));
            }
        }
    }

    private AttackIntension calcAttackIntension(Tile ant) {
        if (ants.enemyDist[ant.row][ant.col] > 5) return AttackIntension.ESCAPE;

        Set<Tile> enemies = ants.attackers.get(ant);
        Set<Tile> friends = ants.friends.get(ant);

        if (friends.size() == 1 && enemies.size() == 1 && !hillDefenders.contains(ant)) {
            if (attackIntensions.get(ant) != AttackIntension.ATTACK) return attackIntensions.get(ant);

            return statisticsModule.oneOnOneIntension(ant);
        }

        int myDist = ants.enemyAttackedAreaDist[ant.row][ant.col];

        int myFirstLineCnt = 0;
        int mySecondLineCnt = 0;
        int enemyFirstLineCnt = 0;
        int enemySecondLineCnt = 0;
        int myAttackForSureCnt = 0;

        for (Tile friend : friends) {
            int dist = ants.enemyAttackedAreaDist[friend.row][friend.col];
            AttackIntension friendAttackIntension = attackIntensions.get(friend);
            if (dist == 0 || dist == 1) {
                if (friendAttackIntension.isAttack()) {
                    myFirstLineCnt++;
                } else {
                    //mySecondLineCnt++;
                    if (myDist < 2) mySecondLineCnt++;
                }
            }

            if (dist == 2 && friendAttackIntension.isAggression()) mySecondLineCnt++;

            int attackForSureDist = ants.enemyAttackedForSureAreaDist[friend.row][friend.col];
            if (attackForSureDist < 2) myAttackForSureCnt++;
        }

        for (Tile enemy : enemies) {
            if (ants.firstLineAnts.contains(enemy)) {
                enemyFirstLineCnt++;
            } else {
                enemySecondLineCnt++;
            }
        }

        if (hillDefenders.contains(ant)) {
            mySecondLineCnt++;
        }

        if (angryMode) {
            myFirstLineCnt++;
        }

        if (enemies.size() == 1 && friends.size() == 2 && myAttackForSureCnt < 2 && enemyNoMove(enemies.iterator().next())) {
            return AttackIntension.ESCAPE;
        }

        if (myFirstLineCnt + mySecondLineCnt > enemyFirstLineCnt + enemySecondLineCnt) {
            if (myFirstLineCnt > enemyFirstLineCnt) {
                return AttackIntension.ATTACK;
            }
            return AttackIntension.SUPPORT;
        } else {
            if (myFirstLineCnt > enemyFirstLineCnt && myDist < 2) {
                return AttackIntension.SUPPORT;
            }
            return AttackIntension.ESCAPE;
        }
    }

    private boolean enemyNoMove(Tile ant) {
        for (Direction dir : Direction.evalues) {
            Tile tile = ants.getTile(ant, dir);
            if (!ants.map[tile.row][tile.col].isPassable()) continue;

            if (ants.myAggression[tile.row][tile.col] > 0) continue;

            return false;
        }
        return true;
    }


    private void createAimMaps() {
        foodMap = createAimMap(ants.foodDist, ants.foodDst);
        unseenMap = createAimMap(ants.unseenDist, ants.unseenDst);
        uncontrolledMap = createAimMap(ants.uncontrolledDist, ants.uncontrolledDst);
    }

    private Map<Tile, Tile> createAimMap(int[][] dist, Tile[][] dst) {
        HashMap<Tile, Tile> aimMap = new HashMap<Tile, Tile>();
        for (Tile ant : ants.myAnts) {
            //if (ants.attackers.get(ant) != null) continue;
            Tile food = dst[ant.row][ant.col];
            if (food == null) continue;
            Tile bestAnt = aimMap.get(food);
            if (bestAnt == null || dist[ant.row][ant.col] < dist[bestAnt.row][bestAnt.col]) {
                aimMap.put(food, ant);
            }
        }

        return aimMap;
    }

    private void ensureHills() {
        hillDefenders.clear();
        for (Tile ant : ants.myAnts) {
            Tile hill = ants.myHillDst[ant.row][ant.col];

            if (!ants.myHillsUnderAttack.contains(hill)) continue;

            if (ants.myHillDist[ant.row][ant.col] <= 10) {
                hillDefenders.add(ant);
            }
        }

    }

    List<Direction> moveDirections;

    private MoveIntention makeDecision(Tile ant) {
        moveDirections = Arrays.asList(Direction.evalues);
        Collections.shuffle(moveDirections, rnd);

        MoveIntention moveIntention = null;

        int priority = 1;

        if (moveDirections.size() > 1 && waitForFoodDecision(ant)) {
            debug(ant + ": waitForFoodDecision");
            moveIntention = updateMoveIntention(moveIntention, priority);
        }
        priority++;

        if (moveDirections.size() > 1 && stayOnHillDecision(ant)) {
            debug(ant + ": stayOnHillDecision");
            moveIntention = updateMoveIntention(moveIntention, priority);
        }
        priority++;

        removeUnpassibleDirections(ant);

        if (moveDirections.size() > 1 && attackHillDecision(ant, 3)) {
            debug(ant + ": attack hill 3");
            moveIntention = updateMoveIntention(moveIntention, priority);
        }
        priority++;

        if (moveDirections.size() > 1 && attackAntDecision(ant)) {
            debug(ant + ": attack " + attackIntensions.get(ant) + " " + attackDirections.get(ant) + " " + moveDirections);
            moveIntention = updateMoveIntention(moveIntention, priority);
        }
        priority++;

        if (moveDirections.size() > 1 && supportBlockedDecision(ant)) {
            debug(ant + ": supportBlocked");
            moveIntention = updateMoveIntention(moveIntention, priority);
        }
        priority++;


        if (defendHillDecision(ant)) {
            debug(ant + ": defendHill");
            moveIntention = updateMoveIntention(moveIntention, priority);
        }
        priority++;

        if (moveDirections.size() > 1 && collectFoodDecision(ant, 25)) {
            debug(ant + ": food 25");
            moveIntention = updateMoveIntention(moveIntention, priority);
        }

//        if (moveDirections.size() > 1 && scoutEscapeDecision(ant)) {
//            debug(ant + ": scout escape");
//        }

        if (moveDirections.size() > 1 && attackHillDecision(ant, 25)) {
            debug(ant + ": attack hill 25");
            moveIntention = updateMoveIntention(moveIntention, priority);
        }

        if (moveDirections.size() > 1 && exploreDecision(ant, 25)) {
            debug(ant + ": explore 25");
            moveIntention = updateMoveIntention(moveIntention, priority);
        }
        priority++;

        if (moveDirections.size() > 1 && escapeIgnoredAntDecision(ant)) {
            debug(ant + ": ignore ant");
        }

        if (moveDirections.size() > 1 && controlDecision(ant, 45)) {
            debug(ant + ": control 45");
            moveIntention = updateMoveIntention(moveIntention, priority);
        }
        priority++;

        /*
        if (moveDirections.size() > 1 && exploreDecision(ant, Integer.MAX_VALUE)) {
            debug(ant + ": explore INF");
        }
        */

        if (moveDirections.size() > 1 && attackHillDecision(ant, Integer.MAX_VALUE)) {
            debug(ant + ": hill INF");
            moveIntention = updateMoveIntention(moveIntention, priority);
        }

        if (moveDirections.size() > 1 && exploreNeverSeenDecision(ant, Integer.MAX_VALUE)) {
            debug(ant + ": exploreNeverSeen INF");
            moveIntention = updateMoveIntention(moveIntention, priority);
        }

        if (moveDirections.size() > 1 && controlDecision(ant, Integer.MAX_VALUE)) {
            debug(ant + ": control INF");
            moveIntention = updateMoveIntention(moveIntention, priority);
        }


        if (moveIntention == null) moveIntention = new MoveIntention(priority, moveDirections);

        return moveIntention;
    }

    private boolean supportBlockedDecision(Tile ant) {
        Set<Tile> attackers = ants.attackers.get(ant);
        if (attackers != null && attackers.size() > 0) return false;
        if (!moveDirections.contains(Direction.NONE)) return false;

        boolean needToSupport = false;
        for (Direction dir : Direction.values) {
            Tile tile = ants.getTile(ant, dir);
            if (!ants.myAnts.contains(tile)) continue;
            Set<Tile> friendAttackers = ants.attackers.get(tile);
            if (friendAttackers == null || friendAttackers.size() == 0) continue;
            if (ants.blockCount[tile.row][tile.col] <= 3) continue;
            needToSupport = true;
            break;
        }
        if (!needToSupport) return false;
        moveDirections = Direction.ONLY_NONE;
        return true;
    }

    private MoveIntention updateMoveIntention(MoveIntention moveIntention, int priority) {
        if (moveIntention == null) {
            moveIntention = new MoveIntention(priority, moveDirections);
        } else {
            moveIntention.updateMovesPriority(moveDirections);
        }

        return moveIntention;
    }

    private boolean stayOnHillDecision(Tile ant) {
        if (ants.myHills.contains(ant) && ants.enemyDist[ant.row][ant.col] == 1) {
            moveDirections = Collections.singletonList(Direction.NONE);
            return true;
        }

        return false;
    }

    private boolean scoutEscapeDecision(Tile ant) {
        if (ants.myControlCount[ant.row][ant.col] > 2) return false;
        if (ants.enemyControlCount[ant.row][ant.col] <= 2) return false;

        return moveToRange(ant, ants.enemyAttackedAreaDist, 3, Integer.MAX_VALUE);
    }

    private boolean escapeIgnoredAntDecision(Tile ant) {
        AttackIntension attackIntension = attackIntensions.get(ant);
        if (attackIntension == null || attackIntension != AttackIntension.IGNORE) return false;

        return moveToMin(ant, ants.enemyAggression);
    }

    private boolean removeUnpassibleDirections(Tile ant) {
        List<Direction> result = new ArrayList<Direction>();

        for (Direction dir : moveDirections) {
            Tile newTile = ants.getTile(ant, dir);
            if (!ants.map[newTile.row][newTile.col].isUnoccupied()) continue;
            result.add(dir);
        }

        if (!result.isEmpty()) {
            moveDirections = result;
        } else {
            moveDirections = Collections.singletonList(Direction.NONE);
        }

        return true;
    }

    private boolean waitForFoodDecision(Tile ant) {
        if (ants.foodDist[ant.row][ant.col] != 1) return false;

        moveDirections = Collections.singletonList(Direction.NONE);
        return true;
    }

    private boolean defendHillDecision(Tile ant) {
        if (!hillDefenders.contains(ant)) return false;

        if (moveToMin(ant, ants.hillAttackersDist, ants.hillAttackersDst)) {
            return moveToMin(ant, ants.myHillDist, ants.myHillDst);
        }

        return false;
    }

    private boolean huntDecision(Tile ant, int maxDist) {
        if (ants.enemyDist[ant.row][ant.col] == Ants.UNREACHABLE) return false;

        if (ants.enemyDist[ant.row][ant.col] < maxDist)
            return moveToMin(ant, ants.enemyDist, null);

        return false;
    }

    private boolean exploreDecision(Tile ant, int maxDist) {
        if (ants.unseenDist[ant.row][ant.col] == Ants.UNREACHABLE) return false;

        if (maxDist != Integer.MAX_VALUE && !ant.equals(unseenMap.get(ants.unseenDst[ant.row][ant.col]))) return false;

        if (ants.myHills.isEmpty()) return false;

        if (ants.unseenDist[ant.row][ant.col] < maxDist)
            return moveToMin(ant, ants.unseenDist, ants.unseenDst);

        return false;
    }

    private boolean controlDecision(Tile ant, int maxDist) {
        if (ants.uncontrolledDist[ant.row][ant.col] == Ants.UNREACHABLE) return false;

        if (maxDist != Integer.MAX_VALUE && !ant.equals(uncontrolledMap.get(ants.uncontrolledDst[ant.row][ant.col]))) {
            return false;
        }

        if (ants.myHills.isEmpty()) return false;

        if (ants.uncontrolledDist[ant.row][ant.col] < maxDist)
            return moveToMin(ant, ants.uncontrolledDist, ants.uncontrolledDst);

        return false;
    }

    private boolean exploreNeverSeenDecision(Tile ant, int maxDist) {
        if (ants.neverSeenDist[ant.row][ant.col] == Ants.UNREACHABLE) return false;

        if (ants.neverSeenDist[ant.row][ant.col] < maxDist)
            return moveToMin(ant, ants.neverSeenDist);

        return false;
    }

    private boolean collectFoodDecision(Tile ant, int maxDist) {
        if (ants.foodDist[ant.row][ant.col] == Ants.UNREACHABLE) return false;

        if (!ant.equals(foodMap.get(ants.foodDst[ant.row][ant.col]))) return false;

        if (ants.myHills.isEmpty()) return false;

        if (ants.foodDist[ant.row][ant.col] < maxDist)
            return moveToMin(ant, ants.foodDist, ants.foodDst);

        return false;
    }

    private boolean attackHillDecision(Tile ant, int maxDist) {
        if (ants.enemyHillDist[ant.row][ant.col] == Ants.UNREACHABLE) return false;

        if (ants.enemyHillDist[ant.row][ant.col] < maxDist)
            return moveToMin(ant, ants.enemyHillDist);

        return false;
    }

    private boolean attackAntDecision(Tile ant) {
        AttackIntension attackIntension = attackIntensions.get(ant);
        if (attackIntension == null) return false;

        Direction attackDirection = attackDirections.get(ant);
        if (attackDirection != null && moveToDir(ant, attackDirection)) return true;

        switch (attackIntension) {
            case ATTACK:
                return moveToMin(ant, ants.enemyAttackedAreaDist);
            case SUPPORT:
                return moveToRange(ant, ants.enemyAttackedAreaDist, 1, 1);
            case ESCAPE: {
                List<Direction> backupMoveDirection = new ArrayList<Direction>(moveDirections);
                if (removeDangerDirection(ant)) {
                    if (moveToRange(ant, ants.enemyAggression, 0, 10)) {
                        return moveToMin(ant, ants.enemyAggression);
                    }
                    moveDirections = backupMoveDirection;
                }

                return moveToMin(ant, ants.enemyAggression);
            }
            case IGNORE:
                return false;
            default:
                throw new IllegalStateException();
        }

    }

    private boolean removeDangerDirection(Tile ant) {
        List<Direction> result = new ArrayList<Direction>();

        for (Direction dir : moveDirections) {
            Tile newTile = ants.getTile(ant, dir);
            if (!ants.map[newTile.row][newTile.col].isUnoccupied()) continue;
            if (ants.dangerTiles.contains(newTile)) continue;
            result.add(dir);
        }

        if (result.isEmpty() || result.size() == moveDirections.size()) return false;

        moveDirections = result;
        return true;
    }

    private boolean moveToRange(Tile ant, int[][] dist, int min, int max) {
        return moveByDist(ant, dist, new RangeEvaluator(min, max), null);
    }


    private boolean moveToMin(Tile ant, int[][] dist) {
        return moveToMin(ant, dist, null);
    }

    private boolean moveToMax(Tile ant, int[][] dist) {
        return moveToMax(ant, dist, null);
    }

    private boolean moveToMin(Tile ant, int[][] dist, Tile[][] dst) {
        return moveByDist(ant, dist, MinEvaluator.instance, dst);
    }

    private boolean moveToMax(Tile ant, int[][] dist, Tile[][] dst) {
        return moveByDist(ant, dist, MaxEvaluator.instance, dst);
    }

    private boolean moveToDir(Tile ant, Direction dir) {
        Tile newTile = ants.getTile(ant, dir);
        if (!ants.map[newTile.row][newTile.col].isUnoccupied()) return false;
        moveDirections = Collections.singletonList(dir);
        return true;
    }

    private boolean moveByDist(Tile ant, int[][] dist, DistEvaluator evaluator, Tile[][] dst) {
        if (dist[ant.row][ant.col] == Ants.UNREACHABLE) return false;

        int bestVal = evaluator.worstValue();

        List<Direction> result = new ArrayList<Direction>();

        for (Direction dir : moveDirections) {
            Tile newTile = ants.getTile(ant, dir);
            if (!ants.map[newTile.row][newTile.col].isUnoccupied()) continue;
            if (dst != null && !dst[ant.row][ant.col].equals(dst[newTile.row][newTile.col])) continue;
            if (evaluator.betterOrEqual(dist[newTile.row][newTile.col], bestVal)) {
                if (evaluator.better(dist[newTile.row][newTile.col], bestVal)) result.clear();
                bestVal = dist[newTile.row][newTile.col];
                result.add(dir);
            }
        }

        if (result.isEmpty()) return false;
        moveDirections = result;

        return true;
    }

//    private void submitOrder(Tile ant, Direction dir) {
//        if (dir != null && dir != Direction.NONE) {
//            Tile aimTile = ants.getTile(ant, dir);
//            movements.add(aimTile);
//            queuedOrders.add(new Order(ant, dir));
//            if (!moveUnprocessedAnts.contains(aimTile)) {
//                submitPending();
//            } else {
//                nextTile = aimTile;
//            }
//        } else {
//            if (queuedOrders.isEmpty()) {
//                movements.add(ant);
//            } else {
//                nextTile = queuedOrders.removeLast().tile;
//            }
//        }
//    }

//    private void submitPending() {
//        while (!queuedOrders.isEmpty()) {
//            Order order = queuedOrders.removeLast();
//            antOrders.put(order.tile, order);
//        }
//    }

    private void flushOrders() {
        removeCyclesAndFlushOrders();
//        for (Order order : antOrders.values()) {
//            if (order.dir != Direction.NONE) {
//                ants.issueOrder(order);
//            }
//        }
    }

    private void removeCyclesAndFlushOrders() {
        Set<Order> goodOrders = new HashSet<Order>();
        Set<Order> badOrders = new HashSet<Order>();

        for (Order order : antOrders.values()) {
            if (order.dir == Direction.NONE) continue;
            if (badOrders.contains(order)) continue;
            if (goodOrders.contains(order)) continue;

            Order cur = antOrders.get(order.tile);
            while (cur != null && cur != order && !goodOrders.contains(cur)) {
                cur = antOrders.get(cur.tile);
            }

            if (cur == order) {
                badOrders.add(order);
                cur = antOrders.get(order.tile);
                while (cur != order) {
                    badOrders.add(cur);
                    cur = antOrders.get(cur.tile);
                }
            } else {
                goodOrders.add(order);
                cur = antOrders.get(order.tile);
                while (cur != null && !goodOrders.contains(cur)) {
                    goodOrders.add(cur);
                    cur = antOrders.get(cur.tile);
                }
            }
        }

        for (Order order : goodOrders) {
            ants.issueOrder(order);
            orders.add(order);
        }
    }

    private void debug(String s) {
        System.err.println(s);
    }
}
