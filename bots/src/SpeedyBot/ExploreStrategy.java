package SpeedyBot;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;

public class ExploreStrategy extends Strategy {

	private Map map;

	public ExploreStrategy(double ratio, MySpeedyBot bot, Map map) {
		super(ratio,bot);
		this.map = map;
	}

	private class FogRegion implements Comparable<FogRegion> {
		private LinkedList<Field> borderFields = new LinkedList<Field>();
		private HashMap<Field, Boolean> borderFieldsCheck = new HashMap<Field, Boolean>();
		private int value = 0;
		private int numFields = 0;

		public void addField(Field field) {
			this.value += field.lastSeen();
			this.numFields++;
		}

		public void setBorderField(Field borderField) {
			if (!borderFieldsCheck.containsKey(borderField)) {
				this.borderFields.add(borderField);
				borderFieldsCheck.put(borderField, true);
			}
		}

		public int getAntsNeeded() {
			return (int) Math.max(1, this.numFields
					/ (double) bot.getViewRadius2());
		}

		public LinkedList<Field> getBorderFields() {
			return this.borderFields;
		}

		public int getSize() {
			return this.numFields;
		}

		public int getValue() {
			return this.value;
		}

		@Override
		public int compareTo(FogRegion o) {
			return o.value - this.value;
		}
	}

	@Override
	public void controllAnts(LinkedList<MyAnt> freeAnts, long time) {
		long startTime = System.currentTimeMillis();
		ArrayList<FogRegion> fogRegions = new ArrayList<FogRegion>(this
				.getFogRegions());
		Logger.printLine("fog regions: " + fogRegions.size());
		Collections.sort(fogRegions);
		Integer[] antsForRegion = new Integer[fogRegions.size()];
		int neededAnts = 0;
		int k = 0;
		for (FogRegion region : fogRegions) {
			neededAnts += region.getAntsNeeded();
			antsForRegion[k] = 0;
			k++;
		}
		Logger.printLine("needed ants: " + neededAnts);
		// int availableAnts = Math.min(neededAnts, freeAnts.size());
		int moreAnts = Math.max(0, freeAnts.size() - neededAnts);
		moreAnts = Math.max(0, moreAnts - 2 * bot.getEnemyAnts().size());
		int availableAnts = neededAnts + moreAnts;
		int remainingAnts = availableAnts;
		Logger.printLine("available ants: " + availableAnts);
		double ratioSum = 0;
		int[] regionIterations = new int[fogRegions.size()];

		for (int i = fogRegions.size() - 1; i >= 0 && remainingAnts > 0; i--) {
			int need = fogRegions.get(i).getAntsNeeded();
			double ratio = need / (double) neededAnts;
			if (i == 0) {
				antsForRegion[i] = remainingAnts;
			} else {
				// antsForRegion[i] = (int) Math.max(1, remainingAnts
				// * (ratio / (1 - ratioSum)));
				antsForRegion[i] = (int) (remainingAnts * (ratio / (1 - ratioSum)));
			}
			remainingAnts -= antsForRegion[i];
			regionIterations[i] = (int) Math.ceil(fogRegions.get(i).getValue());
			Logger.printLine("Region " + i + ": size "
					+ fogRegions.get(i).getSize() + " ratio sum "
					+ Math.round(ratioSum * 100) / 100d + " ratio "
					+ Math.round(ratio * 100) / 100d + " needs " + need
					+ " gets " + antsForRegion[i] + " remaining: "
					+ remainingAnts);
			ratioSum += ratio;
		}

		ArrayList<HashMap<Field, Integer>> checkedFields = new ArrayList<HashMap<Field, Integer>>();
		ArrayList<LinkedList<PathField>> outerPathFields = new ArrayList<LinkedList<PathField>>(
				fogRegions.size());
		for (int i = 0; i < fogRegions.size(); i++) {
			LinkedList<PathField> startPathFields = new LinkedList<PathField>();
			HashMap<Field, Integer> cf = new HashMap<Field, Integer>();
			for (Field f : fogRegions.get(i).getBorderFields()) {
				cf.put(f, antsForRegion[i]);
				startPathFields.add(new PathField(f, null));
			}
			checkedFields.add(cf);
			Collections.sort(startPathFields);
			outerPathFields.add(startPathFields);
		}
		// int firstDepth = (int) Math.sqrt(MySpeedyBot.getViewRadius2());
		// for (int depth = 0; depth < firstDepth; depth++) {
		// for (int i = 0; i < outerPathFields.size(); i++) {
		// LinkedList<PathField> newPathFields = new LinkedList<PathField>();
		// for (PathField pf : outerPathFields.get(i)) {
		// for (Field f : pf.getField().getNeighbors()) {
		// if (!f.hasWater() && !checkedFields.containsKey(f)
		// && !f.isVisible()) {
		// newPathFields.add(new PathField(f, pf, 0));
		// checkedFields.put(f, antsForRegion[i]);
		// }
		// if (f.hasAnt()) {
		// Logger
		// .printLine("FIXME: Ant found in first depth!");
		// }
		// }
		// }
		// outerPathFields.get(i).clear();
		// outerPathFields.get(i).addAll(newPathFields);
		// }
		// }
		// // TODO BS proportional to value
		int openFields = outerPathFields.size();
		boolean first = true;
		while (openFields > 0 && System.currentTimeMillis() - startTime < time) {
			openFields = outerPathFields.size();
			for (int i = 0; i < outerPathFields.size(); i++) {
				if (!outerPathFields.get(i).isEmpty() && antsForRegion[i] > 0) {
					LinkedList<PathField> newPathFields = new LinkedList<PathField>();
					for (PathField pf : outerPathFields.get(i)) {
						boolean allFound = false;
						for (Field f : pf.getField().getNeighbors()) {
							if (!f.hasWater()
									&& !checkedFields.get(i).containsKey(f)
									&& (f.isVisible() || !first)) {
								newPathFields.add(new PathField(f, pf));
								checkedFields.get(i).put(f, antsForRegion[i]);
							}
							if (f.hasAnt() && f.getAnt().getOwner() == 0) {
								MyAnt ant = (MyAnt) f.getAnt();
								if (!ant.isBusy()) {
									Logger.printLine(ant + " explores region "
											+ i);
									if (ant.setNextField(pf.getField(), true,
											false)) {
										antsForRegion[i]--;
										if (antsForRegion[i] == 0) {
											allFound = true;
											break;
										}
									}
								}
							}
						}
						if (allFound) {
							break;
						}
					}
					outerPathFields.get(i).clear();
					Collections.sort(newPathFields);
					outerPathFields.get(i).addAll(newPathFields);
				} else {
					openFields--;
				}
			}
			first = false;
		}
	}

	private LinkedList<FogRegion> getFogRegions() {
		boolean[][] checked = new boolean[map.getCols()][map.getRows()];
		LinkedList<FogRegion> fogRegions = new LinkedList<FogRegion>();

		for (int i = 0; i < map.getCols(); i++) {
			for (int j = 0; j < map.getRows(); j++) {
				Field startField = map.getField(i, j);
				if (!startField.isVisible() && !checked[i][j]
						&& !startField.hasWater()) {
					FogRegion newFogRegion = new FogRegion();
					LinkedList<Field> newFields = new LinkedList<Field>();
					newFogRegion.addField(startField);
					newFields.add(startField);
					checked[i][j] = true;
					while (!newFields.isEmpty()) {
						Field nextField = newFields.pop();
						for (Field checkField : nextField.getNeighbors()) {
							if (!checkField.isVisible()
									&& !checked[checkField.getX()][checkField
											.getY()] && !checkField.hasWater()) {
								newFields.add(checkField);
								checked[checkField.getX()][checkField.getY()] = true;
								newFogRegion.addField(checkField);
							} else if (checkField.isVisible()
									&& !checkField.hasWater()) {
								newFogRegion.setBorderField(nextField);
							}
						}
					}
					fogRegions.add(newFogRegion);
				}
			}
		}

		return fogRegions;
	}

	@Override
	public String toString() {
		return "EXPLORING";
	}

}
