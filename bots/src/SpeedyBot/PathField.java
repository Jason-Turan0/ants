package SpeedyBot;
public class PathField implements Comparable<PathField> {

	private PathField next;
	private Field field;

	public PathField(Field field, PathField next) {
		this.field = field;
		this.next = next;
	}

	public Field getField() {
		return this.field;
	}

	public Field getNext() {
		if (this.next == null) {
			return null;
		} else {
			return this.next.getField();
		}
	}

	@Override
	public int compareTo(PathField other) {
		return other.field.lastSeen() - this.field.lastSeen();
	}
}
