package SpeedyBot;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

public class DefendStrategy extends Strategy {

	private int defendRange = 16;
	private int numAntsPerAnt = 999;
	private int defenderDistance = 12;

	public DefendStrategy(double ratio, MySpeedyBot bot) {
		super(ratio,bot);
	}

	@Override
	public void controllAnts(LinkedList<MyAnt> freeAnts, long time) {
		ArrayList<LinkedList<PathField>> outerPathFields = new ArrayList<LinkedList<PathField>>();
		ArrayList<HashMap<Field, Boolean>> checkedFields = new ArrayList<HashMap<Field, Boolean>>();
		HashMap<Field, Boolean> fighters = new HashMap<Field, Boolean>();
		LinkedList<EnemyAnt> evilAnts = this.getEvilAnts();
		Logger.printLine("Evil ants: " + evilAnts.size());
		for (EnemyAnt enemy : evilAnts) {
			LinkedList<PathField> list = new LinkedList<PathField>();
			HashMap<Field, Boolean> cf = new HashMap<Field, Boolean>();
			list.add(new PathField(enemy.getField(), null));
			cf.put(enemy.getField(), true);
			outerPathFields.add(list);
			checkedFields.add(cf);
		}

		for (int i = 0; i < checkedFields.size(); i++) {
			int foundAnts = 0;
			int range = 0;
			while (range < this.defenderDistance
					&& foundAnts < this.numAntsPerAnt) {
				LinkedList<PathField> nextPathFields = new LinkedList<PathField>(
						outerPathFields.get(i));
				outerPathFields.get(i).clear();
				boolean found = false;
				for (PathField pf : nextPathFields) {
					Field checkField = pf.getField();
					for (Field neighbor : checkField.getNeighbors()) {
						if (!neighbor.hasWater()
								&& !checkedFields.get(i).containsKey(neighbor)) {
							if (neighbor.hasAnt()
									&& neighbor.getAnt().getOwner() == 0
									&& !fighters.containsKey(neighbor)) {
								MyAnt ant = (MyAnt) neighbor.getAnt();
								Logger.printLine(ant + " defends");
								if (!ant.isBusy()) {
									ant.setNextField(checkField, true, false);
								}
								fighters.put(neighbor, true);
								foundAnts++;
								if (foundAnts == this.numAntsPerAnt) {
									found = true;
									break;
								}
							}
							// TODO zickzack pruefen
							checkedFields.get(i).put(neighbor, true);
							boolean myHill = neighbor.hasHill()
									&& neighbor.getHill().getOwner() == 0;
							if (!myHill
									&& pf.getNext() == null
									|| neighbor.getSmallestOffset(checkField)
											.getTwoLengthSquared() == 4) {
								outerPathFields.get(i).add(
										new PathField(neighbor, pf));
							} else if (!myHill) {
								outerPathFields.get(i).addFirst(
										new PathField(neighbor, pf));
							}
						}
					}
					if (found)
						break;
				}
				range++;
			}
		}
	}

	private LinkedList<EnemyAnt> getEvilAnts() {
		LinkedList<EnemyAnt> evilAnts = new LinkedList<EnemyAnt>();
		boolean found = false;
		HashMap<Field, Boolean> checkedFields = new HashMap<Field, Boolean>();
		// TODO
		for (MyHill myHill : bot.getMyHills()) {
			LinkedList<Field> outerFields = new LinkedList<Field>();
			outerFields.add(myHill.getField());
			checkedFields.put(myHill.getField(), true);
			for (int range = 0; range < this.defendRange
					&& !outerFields.isEmpty() && !found; range++) {
				LinkedList<Field> newFields = outerFields;
				outerFields = new LinkedList<Field>();
				for (Field checkField : newFields) {
					for (Field neighbor : checkField.getNeighbors()) {
						if (!neighbor.hasWater()
								&& !checkedFields.containsKey(neighbor)) {
							checkedFields.put(neighbor, true);
							outerFields.add(neighbor);
							if (neighbor.hasAnt()
									&& neighbor.getAnt().getOwner() != 0) {
								evilAnts.add((EnemyAnt) neighbor.getAnt());
								found = true;
							}
						}
					}
				}
			}
		}
		return evilAnts;
	}

	@Override
	public String toString() {
		return "DEFENDING";
	}

}
