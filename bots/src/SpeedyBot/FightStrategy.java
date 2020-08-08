package SpeedyBot;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;

public class FightStrategy extends Strategy {

	private int maxAnts = 7;
	private double majorityFactor = 1.5;
	private Map map;

	public FightStrategy(double ratio, MySpeedyBot bot, Map map) {
		super(ratio,   bot);
		this.map = map;
	}

	@Override
	public void controllAnts(LinkedList<MyAnt> freeAnts, long time) {
		class listSizeComparator implements Comparator<LinkedList<Integer>> {
			@Override
			public int compare(LinkedList<Integer> arg0,
					LinkedList<Integer> arg1) {
				return arg0.size() - arg1.size();
			}
		}
		long startTime = System.currentTimeMillis();
		LinkedList<LinkedList<Integer>> sortList = new LinkedList<LinkedList<Integer>>();
		LinkedList<EnemyAnt> tmpEnemies = new LinkedList<EnemyAnt>(bot
				.getEnemyAnts());
		ArrayList<LinkedList<MyAnt>> myConflictList = new ArrayList<LinkedList<MyAnt>>();
		ArrayList<LinkedList<EnemyAnt>> enemyConflictList = new ArrayList<LinkedList<EnemyAnt>>();
		int counter = 0;
		while (!tmpEnemies.isEmpty()) {
			EnemyAnt enemy = tmpEnemies.pop();
			LinkedList<MyAnt> tmpMyAnts = new LinkedList<MyAnt>();
			LinkedList<EnemyAnt> tmpEnemyAnts = new LinkedList<EnemyAnt>();
			LinkedList<MyAnt> myConflictAnts = new LinkedList<MyAnt>();
			LinkedList<EnemyAnt> enemyConflictAnts = new LinkedList<EnemyAnt>();
			enemyConflictAnts.add(enemy);
			for (Ant myAnt : enemy.getConflictAnts()) {
				tmpMyAnts.add((MyAnt) myAnt);
			}
			while (!tmpMyAnts.isEmpty()) {
				for (MyAnt ant : tmpMyAnts) {
					myConflictAnts.add(ant);
					for (Ant ant2 : ant.getConflictAnts()) {
						if (!enemyConflictAnts.contains(ant2)
								&& !tmpEnemyAnts.contains(ant2)) {
							tmpEnemyAnts.add((EnemyAnt) ant2);
						}
					}
				}
				tmpMyAnts.clear();
				for (EnemyAnt ant : tmpEnemyAnts) {
					tmpEnemies.remove(ant);
					enemyConflictAnts.add(ant);
					for (Ant ant2 : ant.getConflictAnts()) {
						if (!myConflictAnts.contains(ant2)
								&& !tmpMyAnts.contains(ant2)) {
							tmpMyAnts.add((MyAnt) ant2);
						}
					}
				}
				tmpEnemyAnts.clear();
			}
			myConflictList.add(myConflictAnts);
			enemyConflictList.add(enemyConflictAnts);
			LinkedList<Integer> intList = new LinkedList<Integer>();
			int n = myConflictAnts.size() + enemyConflictAnts.size();
			for (int i = 0; i < n; i++) {
				intList.add(counter);
			}
			sortList.add(intList);
			counter++;
		}
		Collections.sort(sortList, new listSizeComparator());
		Logger.printLine("" + myConflictList.size());
		for (int i = 0; i < sortList.size()
				&& time > System.currentTimeMillis() - startTime; i++) {
			long remainingTime = time - System.currentTimeMillis() + startTime;
			int combatResult = this.calculateCombat(myConflictList.get(sortList
					.get(i).getFirst()), enemyConflictList.get(sortList.get(i)
					.getFirst()), bot.getAttackRadius2(), remainingTime);
			if (combatResult == 2) {
				for (MyAnt ant : myConflictList.get(sortList.get(i).getFirst())) {
					LinkedList<Field> neighbors = new LinkedList<Field>();
					for (Field neighbor : ant.getField().getTurnNeighbors()) {
						if (!neighbor.hasWater())
							neighbors.add(neighbor);
					}
					Collections.sort(neighbors, Field.getDangerComparator());
					ant.setNextField(neighbors.getLast(), false, true);
				}
			}
		}
		if (time <= System.currentTimeMillis() - startTime) {
			Logger.printLine("Fight strategy was cut!");
		}
	}

	public int calculateCombat(LinkedList<MyAnt> myAnts,
			LinkedList<EnemyAnt> enemyAnts, int attackRadius2, long time) {
		if (myAnts.size() <= 1)
			return 0;
		else if (myAnts.size() + enemyAnts.size() <= this.maxAnts) {
			Logger.printLine("Fighting ants "
					+ (myAnts.size() + enemyAnts.size()));
			long startTime = System.currentTimeMillis();
			Field[] myAntFields = new Field[myAnts.size()];
			String debugText = "Calculating combat between ";
			int j = 0;
			for (Ant ant : myAnts) {
				myAntFields[j] = ant.getField();
				debugText += myAntFields[j].getX() + ","
						+ myAntFields[j].getY() + " ";
				j++;
			}

			debugText += "and";
			Field[] enemyAntFields = new Field[enemyAnts.size()];
			j = 0;
			for (Ant ant : enemyAnts) {
				enemyAntFields[j] = ant.getField();
				debugText += " " + enemyAntFields[j].getX() + ","
						+ enemyAntFields[j].getY();
				j++;
			}

			Logger.printLine(debugText + ".");

			Field[] myAntFieldsLater = new Field[myAnts.size()];

			int[] directionRotation = new int[myAnts.size()];
			int[] myDirections = new int[myAnts.size()];
			for (int i = 0; i < myAnts.size(); i++) {
				myDirections[i] = 0;
				directionRotation[i] = (int) (Math.random() * 5);
			}

			boolean overFlow1 = false;
			int bestWorstCaseDifference = -myAnts.size() - 1;
			int bestBestCaseDifference = -myAnts.size() - 1;
			int numBestBestCases = 0;
			int bestWorstCaseLoss = -1;
			int[] bestMyDirections = new int[myDirections.length];
			boolean[][] occupied = new boolean[map.getCols()][map.getRows()];
			while (!overFlow1) {
				int[] enemyDirections = new int[enemyAnts.size()];
				Field[] enemyAntFieldsLater = new Field[enemyAnts.size()];
				boolean overFlow2 = false;
				boolean permutationImpossible1 = false;
				int bestCaseDifference = -myAnts.size() - 1;
				int numBestCases = 0;
				int worstCaseDifference = enemyAnts.size() + 1;
				int worstCaseLoss = -1;
				for (int i = 0; i < myAnts.size(); i++) {
					myAntFieldsLater[i] = Offset.getDirectionOffset(
							(myDirections[i] + directionRotation[i]) % 5,map)
							.getField(myAntFields[i]);
					if (myAntFieldsLater[i].hasWater()
							|| occupied[myAntFieldsLater[i].getX()][myAntFieldsLater[i]
									.getY()]) {
						permutationImpossible1 = true;
					}
					occupied[myAntFieldsLater[i].getX()][myAntFieldsLater[i]
							.getY()] = true;
				}
				if (!permutationImpossible1) {
					do {
						if (time < System.currentTimeMillis() - startTime) {
							Logger.printLine("Calculating combat was cut!");
							if (myAnts.size() > enemyAnts.size()) {
								return 2;
							} else {
								return 1;
							}
						}
						boolean permutationImpossible2 = false;
						for (int i = 0; i < enemyAnts.size(); i++) {
							enemyAntFieldsLater[i] = Offset.getDirectionOffset(
									enemyDirections[i],map).getField(
									enemyAntFields[i]);
							if (enemyAntFieldsLater[i].hasWater()
									|| occupied[enemyAntFieldsLater[i].getX()][enemyAntFieldsLater[i]
											.getY()]) {
								permutationImpossible2 = true;
							}
							occupied[enemyAntFieldsLater[i].getX()][enemyAntFieldsLater[i]
									.getY()] = true;
						}

						if (!permutationImpossible2) {
							int[] deadAnts = this.getDeadAnts(myAntFieldsLater,
									enemyAntFieldsLater, attackRadius2);
							if (deadAnts[1] - deadAnts[0] < worstCaseDifference
									|| (deadAnts[1] - deadAnts[0] == worstCaseDifference && deadAnts[0] > worstCaseLoss)) {
								worstCaseDifference = deadAnts[1] - deadAnts[0];
								worstCaseLoss = deadAnts[0];
							}
							if (deadAnts[1] - deadAnts[0] > bestCaseDifference) {
								numBestCases = 0;
								bestCaseDifference = deadAnts[1] - deadAnts[0];
							} else if (deadAnts[1] - deadAnts[0] == bestCaseDifference) {
								numBestCases++;
							}
						}
						boolean carry = true;
						for (int i = 0; i < enemyAnts.size(); i++) {
							occupied[enemyAntFieldsLater[i].getX()][enemyAntFieldsLater[i]
									.getY()] = false;
							if (carry) {
								enemyDirections[i]++;
								if (enemyDirections[i] > 4
										&& i == enemyAnts.size() - 1) {
									overFlow2 = true;
								} else if (enemyDirections[i] > 4) {
									enemyDirections[i] = 0;
								} else {
									carry = false;
								}
							}
						}
					} while (!overFlow2);

					if (worstCaseDifference > bestWorstCaseDifference
							|| (worstCaseDifference == bestWorstCaseDifference && worstCaseLoss < bestWorstCaseLoss)
							|| (worstCaseDifference == bestWorstCaseDifference
									&& worstCaseLoss == bestWorstCaseLoss && bestCaseDifference > bestBestCaseDifference)
							|| (worstCaseDifference == bestWorstCaseDifference
									&& worstCaseLoss == bestWorstCaseLoss
									&& bestCaseDifference == bestBestCaseDifference && numBestCases > numBestBestCases)) {
						bestWorstCaseDifference = worstCaseDifference;
						bestWorstCaseLoss = worstCaseLoss;
						bestBestCaseDifference = bestCaseDifference;
						numBestBestCases = numBestCases;
						bestMyDirections = myDirections.clone();
					}

					if (bestWorstCaseDifference == enemyAnts.size()) {
						break;
					}
				}

				boolean carry = true;
				for (int i = 0; i < myAnts.size(); i++) {
					occupied[myAntFieldsLater[i].getX()][myAntFieldsLater[i]
							.getY()] = false;
					if (carry) {
						myDirections[i]++;
						if (myDirections[i] > 4 && i == myAnts.size() - 1) {
							overFlow1 = true;
						} else if (myDirections[i] > 4) {
							myDirections[i] = 0;
						} else {
							carry = false;
						}
					}
				}
			}

			j = 0;
			for (MyAnt ant : myAnts) {
				int direction = (bestMyDirections[j] + directionRotation[j]) % 5;
				ant.setDirection(direction, false, true);
				j++;
			}
			return 0;
		} else if (myAnts.size() > enemyAnts.size()) {
			return 2;
		} else {
			return 1;
		}
	}

	private int[] getDeadAnts(Field[] myAntFields, Field[] enemyAntFields,
			int attackRadius2) {

		int result[] = new int[2];

		ArrayList<LinkedList<Integer>> myRangeAnts = new ArrayList<LinkedList<Integer>>(
				enemyAntFields.length);
		ArrayList<LinkedList<Integer>> enemyRangeAnts = new ArrayList<LinkedList<Integer>>(
				myAntFields.length);
		for (int i = 0; i < enemyAntFields.length; i++) {
			enemyRangeAnts.add(i, new LinkedList<Integer>());
		}

		boolean[] myAntDead = new boolean[myAntFields.length];
		boolean[] enemyAntDead = new boolean[enemyAntFields.length];

		for (int i = 0; i < myAntFields.length; i++) {
			LinkedList<Integer> myList = new LinkedList<Integer>();
			for (int j = 0; j < enemyAntFields.length; j++) {
				LinkedList<Integer> enemyList = enemyRangeAnts.get(j);
				if (myAntFields[i].getSmallestOffset(enemyAntFields[j])
						.getTwoLengthSquared() <= attackRadius2) {
					myList.add(Integer.valueOf(j));
					enemyList.add(Integer.valueOf(i));
				}
			}
			myRangeAnts.add(i, myList);
		}

		for (int i = 0; i < myAntFields.length; i++) {
			for (Integer enemyAnt : myRangeAnts.get(i)) {
				if (!myAntDead[i]
						&& myRangeAnts.get(i).size() >= enemyRangeAnts.get(
								enemyAnt).size()) {
					myAntDead[i] = true;
					result[0]++;
				}
				if (!enemyAntDead[enemyAnt]
						&& myRangeAnts.get(i).size() <= enemyRangeAnts.get(
								enemyAnt).size()) {
					enemyAntDead[enemyAnt] = true;
					result[1]++;
				}
			}
		}

		return result;
	}

	@Override
	public String toString() {
		return "FIGHTING";
	}
}
