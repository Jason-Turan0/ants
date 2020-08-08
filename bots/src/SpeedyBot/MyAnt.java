package SpeedyBot;
public class MyAnt extends Ant {

	private Field nextField;
	private boolean busy = false;
	private int direction = 4;

	public MyAnt(Field field, Map map) {
		super(field,map);

		this.nextField = field;
		field.setNextAnt(this);

		// TODO performance improvement could be done here by only updating
		// inner view border fields (except for the new ants)
		for (Field viewField : this.getViewField()) {
			viewField.setVisible();
		}
		for (Field visitedField : this.getVisitArea()) {
			visitedField.setVisited();
		}
	}

	public boolean setDirection(int direction, boolean save, boolean priority) {
		if (this.busy) {
			Logger.printLine("Warning: Changing busy ants order!");
		}
		Field next = this.getField().getNeighbor(direction);
		if (next == this.nextField
				|| ((!next.hasNextAnt() || next.getNextAnt().avoid(priority))
						&& (!next.hasHill() || next.getHill().getOwner() != 0)
						&& !next.hasFood() && (next.isSave() || !save) && !next
						.hasWater())) {
			this.direction = direction;
			this.busy = true;
			this.nextField.setNextAnt(null);
			next.setNextAnt(this);
			this.nextField = next;
			Logger.printLine(this + " will move to " + next);
			return true;
		} else if (next.hasFood()) {
			Logger.printLine("Warning: Tried to send ant on food!");
			return false;
		} else if (next.hasWater()) {
			Logger.printLine("ERROR: Tried to send ant on water!");
			return false;
		} else if (next.hasHill() && next.getHill().getOwner() == 0) {
			Logger.printLine("Warning: Tried to send ant on own hill!");
			return false;
		} else {
			Logger.printLine("Blocked...");
			return false;
		}
	}

	public boolean setNextField(Field next, boolean save, boolean priority) {
		Offset offset = this.getField().getSmallestOffset(next);
		if (offset.getOneLength() > 1) {
			Logger
					.printLine("ERROR: Tried to send ant over more than one field!");
			return false;
		} else {
			return this.setDirection(offset.getDirection(), save, priority);
		}
	}

	public boolean avoid(boolean priority) {
		if (!this.busy) {
			for (int i = 0; i < 4; i++) {
				Field next = this.getField().getNeighbor(i);
				if (next != this.nextField && !next.hasNextAnt()
						&& (!next.hasHill() || next.getHill().getOwner() != 0)
						&& !next.hasFood() && (next.isSave() || priority)
						&& !next.hasWater()) {
					this.direction = i;
					this.nextField.setNextAnt(null);
					next.setNextAnt(this);
					this.nextField = next;
					Logger.printLine(this + " avoids to " + next);
					return true;
				}
			}
		}
		Logger.printLine(this + " can't avoid");
		return false;
	}

	public boolean canAvoid() {
		if (!this.busy) {
			for (Field next : this.getField().getTurnNeighbors()) {
				if (next != this.nextField && !next.hasNextAnt()
						&& (!next.hasHill() || next.getHill().getOwner() != 0)
						&& !next.hasFood() && !next.isSave()
						&& !next.hasWater()) {
					return true;
				}
			}
		}
		return false;
	}

	public int getDirection() {
		return this.direction;
	}

	public boolean isBusy() {
		return this.busy;
	}

	public Field getNextField(int turn) {
		return this.nextField;
	}

	@Override
	public int getOwner() {
		return 0;
	}

}
