package SpeedyBot;
public class Offset {

	private int xOffset;
	private int yOffset;
	private Map map;

	public Offset(int x, int y, Map map) {
		this.xOffset = x;
		this.yOffset = y;
		this.map = map;
	}

	public int getX() {
		return this.xOffset;
	}

	public int getY() {
		return this.yOffset;
	}

	public Field getField(Field field) {
		return map.getField((field.getX() + this.xOffset + map.getCols())
				% map.getCols(), (field.getY() + this.yOffset + map.getRows())
				% map.getRows());
	}

	public static Offset getDirectionOffset(int direction, Map map) {
		if (direction <= 3 && direction >= 0) {
			int x = ((direction + 1) % 2) * (1 - direction);
			int y = (direction % 2) * (2 - direction);
			return new Offset(x, y, map);
		} else
			return new Offset(0, 0, map);
	}

	public int getDirection() {
		if (this.getOneLength() > 1) {
			Logger
					.printLine("ERROR: Requested direction of Offset longer than 1");
		}
		if (this.xOffset == 1)
			return 0;
		else if (this.yOffset == 1)
			return 1;
		else if (this.xOffset == -1)
			return 2;
		else if (this.yOffset == -1)
			return 3;
		else
			return 4;
	}

	public int getOneLength() {
		return Math.abs(this.xOffset) + Math.abs(this.yOffset);
	}

	public int getTwoLengthSquared() {
		return (int) (Math.pow(this.xOffset, 2) + Math.pow(this.yOffset, 2));
	}

	public Offset getInverse() {
		return new Offset(-this.xOffset, -this.yOffset,this.map);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + xOffset;
		result = prime * result + yOffset;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Offset other = (Offset) obj;
		if (xOffset != other.xOffset)
			return false;
		if (yOffset != other.yOffset)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return this.xOffset + "|" + this.yOffset;
	}
}
