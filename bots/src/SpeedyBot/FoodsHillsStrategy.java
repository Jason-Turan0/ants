package SpeedyBot;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

public class FoodsHillsStrategy extends Strategy {

	public FoodsHillsStrategy(double ratio, MySpeedyBot bot) {
		super(ratio,bot);
	}

	private double antHillRatio = 1 / 8d;

	@Override
	public void controllAnts(LinkedList<MyAnt> freeAnts, long time) {
		class TargetData {
			Target target;
			HashMap<Field, Boolean> checkedFields = new HashMap<Field, Boolean>();
			LinkedList<PathField> outerPathFields = new LinkedList<PathField>();
			int foundAnts = 0;

			TargetData(Target t) {
				this.target = t;
				this.checkedFields.put(t.getField(), true);
				this.outerPathFields.add(new PathField(t.getField(), null));
			}
		}

		int numFreeAnts = freeAnts.size();
		int antsPerHill = (int) (bot.getMyAnts().size()
				* bot.getEnemyHillCount() * antHillRatio);
		long startTime = System.currentTimeMillis();
		LinkedList<TargetData> targets = new LinkedList<TargetData>();
		for (Target target : bot.getTargets()) {
			TargetData td = new TargetData(target);
			targets.add(td);
		}

		long remainingTime = time - System.currentTimeMillis() + startTime;
		long lastIterationTime = 0;
		while (numFreeAnts > 0 && !targets.isEmpty()
				&& remainingTime > lastIterationTime) {
			long iterationStartTime = System.currentTimeMillis();
			for (Iterator<TargetData> iterator = targets.iterator(); iterator
					.hasNext();) {
				TargetData targetData = iterator.next();
				LinkedList<PathField> outerPathFields = targetData.outerPathFields;
				targetData.outerPathFields = new LinkedList<PathField>();
				boolean foundAll = false;

				for (PathField pf : outerPathFields) {
					Field checkField = pf.getField();
					for (Field neighbor : checkField.getNeighbors()) {
						if (!neighbor.hasWater()
								&& !targetData.checkedFields
										.containsKey(neighbor)) {
							if (neighbor.hasAnt()
									&& neighbor.getAnt().getOwner() == 0) {
								MyAnt ant = (MyAnt) neighbor.getAnt();
								if (!ant.isBusy()
										|| targetData.target.getClass().equals(
												EnemyHill.class)) {
									Logger.printLine(ant + " heads for "
											+ targetData.target.getField());
									if (!ant.isBusy()
											&& targetData.target.getField() == checkField
											&& targetData.target.getClass()
													.equals(Food.class)) {
										ant.setNextField(neighbor, true, false);
									} else if (!ant.isBusy()) {
										ant.setNextField(checkField, true,
												false);
									}
									numFreeAnts--;
									targetData.foundAnts++;
									if ((targetData.target.getClass().equals(
											EnemyHill.class) && targetData.foundAnts == antsPerHill)
											|| targetData.target.getClass()
													.equals(Food.class)) {
										iterator.remove();
										foundAll = true;
										break;
									}
								}
							}
							// TODO zickzack
							targetData.checkedFields.put(neighbor, true);
							boolean myHill = neighbor.hasHill()
									&& neighbor.getHill().getOwner() == 0;
							if (!myHill
									&& pf.getNext() == null
									|| neighbor.getSmallestOffset(checkField)
											.getTwoLengthSquared() == 4) {
								targetData.outerPathFields.add(new PathField(
										neighbor, pf));
							} else if (!myHill) {
								targetData.outerPathFields
										.addFirst(new PathField(neighbor, pf));
							}
						}
					}
					if (foundAll)
						break;
				}
			}
			lastIterationTime = System.currentTimeMillis() - iterationStartTime;
			remainingTime = time - System.currentTimeMillis() + startTime;
		}
	}

	@Override
	public String toString() {
		return "GATHERING FOOD AND RAZING HILLS";
	}

}
