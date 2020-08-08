package SpeedyBot;
import java.util.LinkedList;

public abstract class Strategy {

	private double timeRatio;
	protected MySpeedyBot bot;

	public Strategy(double ratio, MySpeedyBot bot) {
		this.timeRatio = ratio;
		this.bot=bot;
	}

	public abstract void controllAnts(LinkedList<MyAnt> freeAnts, long time);

	public double getTimeRatio() {
		return this.timeRatio;
	}

	@Override
	public abstract String toString();
}
