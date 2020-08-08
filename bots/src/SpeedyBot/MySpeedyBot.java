package SpeedyBot;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class MySpeedyBot {
    private  int loadtime;
	private  int turnTime;
	private  int turns;
	private  int viewRadius2;
	private  int attackRadius2;
	private  int spawnRadius2;
	private  int visitRadius2 = 5;

	private  int turn = 0;
	private  long longestThinkTime = 0;
	private  long totalThinkTime = 0;

	private  LinkedList<Strategy> strategies;

	private  LinkedList<MyAnt> myAnts;
	private  LinkedList<EnemyAnt> enemyAnts;

	private  LinkedList<MyHill> myHills;
	private  LinkedList<MyHill> myOldHills;

	private  int enemyHillCount;
	private  LinkedList<Target> targets;
	private  LinkedList<Target> oldTargets;
    private Map map;

    public MySpeedyBot(Map map) {
        this.map = map;
        targets = new LinkedList<Target>();
        myHills = new LinkedList<MyHill>();
        myOldHills = new LinkedList<MyHill>();
        strategies = new LinkedList<Strategy>();
        strategies.add(new FightStrategy(7 / 10d, this,map));
        strategies.add(new DefendStrategy(1 / 20d,this));
        strategies.add(new FoodsHillsStrategy(1 / 10d,this));
        strategies.add(new ExploreStrategy(3 / 40d,this, map));
        strategies.add(new MoveToFrontStrategy(1 / 20d,this));
        strategies.add(new SurviveStratetgy(1 / 40d,this));
        prepareTurn();
    }

	private  void prepareTurn() {
		myAnts = new LinkedList<MyAnt>();
		enemyAnts = new LinkedList<EnemyAnt>();
		myOldHills = myHills;
		myHills = new LinkedList<MyHill>();
		oldTargets = targets;
		targets = new LinkedList<Target>();
		enemyHillCount = 0;
		turn++;
	}

	public  List<String> doTurn() {
		checkTargetsAndMyHills();

		long turnStartTime = System.currentTimeMillis();
		double ratioSum = 0;
		LinkedList<MyAnt> freeAnts = new LinkedList<MyAnt>(myAnts);
		for (Strategy strategy : strategies) {
			double ratio = strategy.getTimeRatio();
			long remainingTime = turnTime - System.currentTimeMillis()
					+ turnStartTime;
			long taskTime = (long) (ratio / (1 - ratioSum) * remainingTime);
			long taskStartTime = System.currentTimeMillis();
			Logger.printLine("Starting with " + strategy + " with " + taskTime + "ms");
			strategy.controllAnts(freeAnts, taskTime);
			long neededTime = System.currentTimeMillis() - taskStartTime;
			Logger.printLine(strategy + " took " + neededTime + "ms.");
			for (Iterator<MyAnt> iterator = freeAnts.iterator(); iterator.hasNext();) {
				MyAnt myAnt = iterator.next();
				if (myAnt.isBusy()) {
					iterator.remove();
				}
			}
			ratioSum += ratio;
		}
		List<String> commands = createCommands();
		prepareTurn();
		long turnTime = System.currentTimeMillis() - turnStartTime;
		totalThinkTime += turnTime;
		if (longestThinkTime < turnTime) {
			longestThinkTime = turnTime;
		}
		Logger.printLine("Whole turn took " + turnTime + "ms");
		return commands;
	}

	private  void checkTargetsAndMyHills() {
		for (Target target : oldTargets) {
			if (!target.getField().isVisible()) {
				if (target.getClass().equals(Food.class)) {
					targets.add(new Food(target.getField()));
				} else {
					targets.add(new EnemyHill(target.getField(),
							((EnemyHill) target).getOwner()));
					enemyHillCount++;
				}
			}
		}
		for (MyHill hill : myOldHills) {
			if (!hill.getField().isVisible()) {
				myHills.add(new MyHill(hill.getField()));
			}
		}

	}

	public List<String> createCommands() {
		ArrayList<String> commands = new ArrayList<String>();
		for (MyAnt ant : myAnts) {
			int dir = ant.getDirection();
			String direction = "";
			if (dir < 4) {
				switch (dir) {
				case 0:
					direction = "E";
					break;
				case 1:
					direction = "S";
					break;
				case 2:
					direction = "W";
					break;
				case 3:
					direction = "N";
					break;
				default:
					direction = "E";
					break;
				}
				String command = "o " + ant.getY() + " " + ant.getX() + " "						+ direction;
				commands.add(command);
			}
		}
		return commands;
	}

	public  void setLoadTime(int time) {
		loadtime = time;
	}

	public  void setTurnTime(int time) {
		// TODO dummy time for tcp
		time = 500;
		turnTime = time;
	}

	public  void setTurns(int turns) {
		turns = turns;
	}

	public  void setViewRadius2(int radius2) {
		viewRadius2 = radius2;
	}

	public  void setAttackRadius2(int radius) {
		attackRadius2 = radius;
	}

	public  void setSpawnRadius2(int radius) {
		spawnRadius2 = radius;
	}

	public  void setHill(int x, int y, int owner) {
		Field field = map.getField(x, y);
		if (owner == 0 && !field.hasHill()) {
			MyHill hill = new MyHill(field);
			myHills.add(hill);
		} else if (owner != 0 && !field.hasHill()) {
			EnemyHill hill = new EnemyHill(field, owner);
			targets.add(hill);
			enemyHillCount++;
		}
	}

	public  void setFood(int x, int y) {
		Field field = map.getField(x, y);
		if (!field.hasFood()) {
			Food food = new Food(field);
			targets.add(food);
		}
	}

	public  void setAnt(int x, int y, int owner) {
		Field field = map.getField(x, y);
		if (owner == 0) {
			myAnts.add(new MyAnt(field,map));
		} else {
			enemyAnts.add(new EnemyAnt(field, owner,map));
		}
	}

	public  void antDead(int x, int y, int owner) {
	}

	public  int getTurns() {
		return turns;
	}

	public  int getTurn() {
		return turn;
	}


	public  LinkedList<MyAnt> getMyAnts() {
		return myAnts;
	}

	public  LinkedList<Target> getTargets() {
		return targets;
	}

	public  LinkedList<EnemyAnt> getEnemyAnts() {
		return enemyAnts;
	}

	public  int getEnemyHillCount() {
		return enemyHillCount;
	}

	public  int getAttackRadius2() {
		return attackRadius2;
	}

	public  int getViewRadius2() {
		return viewRadius2;
	}

	public  LinkedList<MyHill> getMyHills() {
		return myHills;
	}
}
