package SpeedyBot;
import java.util.Collections;
import java.util.LinkedList;

public class SurviveStratetgy extends Strategy {

	public SurviveStratetgy(double ratio, MySpeedyBot bot) {
		super(ratio,bot);
	}

	@Override
	public void controllAnts(LinkedList<MyAnt> freeAnts, long time) {
		for (MyAnt ant : freeAnts) {
			Logger.printLine(ant + " tries to survive");
			LinkedList<Field> neighbors = new LinkedList<Field>();
			for (Field neighbor : ant.getField().getTurnNeighbors()) {
				if (!neighbor.hasWater())
					neighbors.add(neighbor);
			}
			Collections.sort(neighbors, Field.getDangerComparator());
			while (!neighbors.isEmpty()
					&& !ant.setNextField(neighbors.pop(), false, false))
				;
		}
	}

	@Override
	public String toString() {
		return "SURVIVING";
	}

}
