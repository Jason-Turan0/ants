package SpeedyBot;
import java.util.HashMap;
import java.util.LinkedList;

public class MoveToFrontStrategy extends Strategy {

	public MoveToFrontStrategy(double ratio, MySpeedyBot bot) {
		super(ratio,bot);
	}

	@Override
	public void controllAnts(LinkedList<MyAnt> freeAnts, long time) {
		LinkedList<EnemyAnt> enemyAnts = bot.getEnemyAnts();
		int numFreeAnts = freeAnts.size();
		LinkedList<PathField> outerPathFields = new LinkedList<PathField>();
		HashMap<Field, Boolean> checkedFields = new HashMap<Field, Boolean>();
		for (EnemyAnt ant : enemyAnts) {
			outerPathFields.add(new PathField(ant.getField(), null));
			checkedFields.put(ant.getField(), true);
		}

		while (numFreeAnts > 0 && !outerPathFields.isEmpty()) {
			LinkedList<PathField> newPathFields = outerPathFields;
			outerPathFields = new LinkedList<PathField>();
			for (PathField pf : newPathFields) {
				for (Field neighbor : pf.getField().getNeighbors()) {
					if (!neighbor.hasWater()
							&& !checkedFields.containsKey(neighbor)) {
						checkedFields.put(neighbor, true);
						outerPathFields.add(new PathField(neighbor, pf));
						if (neighbor.hasAnt()
								&& neighbor.getAnt().getOwner() == 0
								&& !((MyAnt) neighbor.getAnt()).isBusy()) {
							((MyAnt) neighbor.getAnt()).setNextField(pf
									.getField(), true, false);
							Logger.printLine(neighbor.getAnt()
									+ " moves to front");
							numFreeAnts--;
						}
					}
				}
			}
		}
	}

	@Override
	public String toString() {
		return "MOVING ANTS TO FRONT";
	}

}
