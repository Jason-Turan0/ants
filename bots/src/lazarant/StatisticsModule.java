package lazarant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class StatisticsModule {
    Ants ants;
    MyBot myBot;

    private Map<Tile, AttackIntension> enemy1on1PossibleMoves = new HashMap<Tile, AttackIntension>();
    private Map<Integer, OneOnOneStatistics> enemy1on1Statistics = new HashMap<Integer, OneOnOneStatistics>();

    public StatisticsModule(MyBot myBot) {
        this.myBot = myBot;
        ants = myBot.ants;
    }

    public void updateEnemyStatistics() {
        for (Tile tile : ants.enemyColors.keySet()) {
            if (enemy1on1PossibleMoves.containsKey(tile)) {
                update1on1Statistics(ants.enemyColors.get(tile), enemy1on1PossibleMoves.get(tile));
            }

        }

        enemy1on1PossibleMoves.clear();
    }

    private void update1on1Statistics(Integer enemyColor, AttackIntension attackIntension) {
        OneOnOneStatistics oneOnOneStat = enemy1on1Statistics.get(enemyColor);
        if (oneOnOneStat == null) {
            oneOnOneStat = new OneOnOneStatistics();
            enemy1on1Statistics.put(enemyColor, oneOnOneStat);
        }

        if (attackIntension == AttackIntension.ATTACK) {
            oneOnOneStat.attackCount++;
        } else {
            oneOnOneStat.escapeCount++;
        }
    }

    public AttackIntension oneOnOneIntension(Tile ant) {
        Tile enemy = ants.attackers.get(ant).iterator().next();

        Set<Tile> generalFriends = ants.generalAttackers.get(enemy);
        if (generalFriends != null && generalFriends.size() > 0) {
//            System.err.println(ant + ": Big fight. Escape.");
            return AttackIntension.ESCAPE;
        }

        if (!update1on1PossibleMoves(enemy)) {
            return AttackIntension.ESCAPE;
        }

        Integer enemyColor = ants.enemyColors.get(enemy);
        OneOnOneStatistics oneOnOneStat = enemy1on1Statistics.get(enemyColor);

        if (oneOnOneStat != null && oneOnOneStat.isEscaper()) {
            return AttackIntension.IGNORE;
        }

        if (ants.myControlCount[ant.row][ant.col] > 2 && ants.enemyControlCount[ant.row][ant.col] < 2) {
            return AttackIntension.IGNORE;
        }

        return AttackIntension.ESCAPE;
    }


    private boolean update1on1PossibleMoves(Tile enemy) {
        Map<Tile, AttackIntension> possibleMoves = new HashMap<Tile, AttackIntension>();
        int attackCells = 0;
        int escapeCells = 0;
        for (Direction dir : Direction.evalues) {
            Tile movedEnemy = ants.getTile(enemy, dir);
            if (!ants.map[movedEnemy.row][movedEnemy.col].isPassable()) continue;
            if (ants.myAttackedAreaDist[movedEnemy.row][movedEnemy.col] <= 1) {
                possibleMoves.put(movedEnemy, AttackIntension.ATTACK);
                attackCells++;
            } else {
                possibleMoves.put(movedEnemy, AttackIntension.ESCAPE);
                escapeCells++;
            }
        }

        if (attackCells > 0 && escapeCells > 0) {
            enemy1on1PossibleMoves.putAll(possibleMoves);
            return true;
        } else {
            return false;
        }
    }

    private Set<Tile> neighbourWater(Tile friend) {
        Set<Tile> neighbourWater = new HashSet<Tile>();
        for (Direction dir : Direction.values) {
            Tile tile = ants.getTile(friend, dir);
            if (!ants.map[tile.row][tile.col].isPassable()) {
                neighbourWater.add(tile);
            }
        }
        return neighbourWater;
    }


}
