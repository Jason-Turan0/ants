package SpeedyBot;
import java.util.HashMap;
import java.util.LinkedList;

public abstract class Ant {
	private Field field;
	private Map map;

	public Ant(Field field, Map map) {
		this.field = field;
		this.map = map;
		field.setAnt(this);
	}

	public int getX() {
		return this.field.getX();
	}

	public int getY() {
		return this.field.getY();
	}

	public Field getField() {
		return this.field;
	}

	public abstract int getOwner();

	public LinkedList<Field> getViewBorder() {
		LinkedList<Field> fields = new LinkedList<Field>();
		for (Offset offset : map.getViewBorder()) {
			fields.add(offset.getField(this.field));
		}

		return fields;
	}

	public LinkedList<Field> getViewField() {
		LinkedList<Field> fields = new LinkedList<Field>();
		for (Offset offset : map.getView()) {
			fields.add(offset.getField(this.field));
		}
		return fields;
	}

	public LinkedList<Field> getAttackArea() {
		LinkedList<Field> fields = new LinkedList<Field>();
		for (Offset offset : map.getAttackArea()) {
			fields.add(offset.getField(this.field));
		}

		return fields;
	}

	public LinkedList<Field> getAttackBorder() {
		LinkedList<Field> fields = this.field.getAttackBorder();
		if (fields == null) {
			fields = new LinkedList<Field>();
			for (Offset offset : map.getAttackBorder()) {
				boolean found = false;
				if (!offset.getField(this.field).hasWater()) {
					for (Offset o : map.getAttackOffsets().get(offset)) {
						if (!o.getField(this.field).hasWater()) {
							found = true;
							break;
						} else if (!o.getInverse().getField(
								offset.getField(this.field)).hasWater()) {
							found = true;
							break;
						}
					}
				}
				if (found) {
					fields.add(offset.getField(this.field));
				}
			}
			this.field.setAttackBorder(fields);
		}

		return fields;
	}

	public LinkedList<Field> getAttackBorder2() {
		LinkedList<Field> fields = this.field.getAttackBorder2();
		if (fields == null) {
			fields = new LinkedList<Field>();
			for (Offset offset : map.getAttackBorder2()) {
				boolean found = false;
				if (!offset.getField(this.field).hasWater()) {
					for (Offset o1 : map.getAttackOffsets().get(offset)) {
						if (!o1.getField(this.field).hasWater()) {
							for (Offset o2 : map.getAttackOffsets().get(new Offset(
									offset.getX() - o1.getX(), offset.getY()
											- o1.getY(), map))) {
								if (!o2.getInverse().getField(
										offset.getField(this.field)).hasWater()) {
									found = true;
									break;
								}
							}
						}
						if (!found
								&& !o1.getInverse().getField(
										offset.getField(this.field)).hasWater()) {
							for (Offset o2 : map.getAttackOffsets().get(new Offset(
									offset.getX() - o1.getX(), offset.getY()
											- o1.getY(), map))) {
								if (!o2.getField(this.field).hasWater()) {
									found = true;
									break;
								}
							}
						}
					}
				}
				if (found) {
					fields.add(offset.getField(this.field));
				}
			}
			this.field.setAttackBorder2(fields);
		}

		return fields;
	}

	public LinkedList<Ant> getConflictAnts() {
		LinkedList<Ant> result = new LinkedList<Ant>();
		for (Field field : this.getAttackBorder()) {
			Ant ant = field.getAnt();
			if (ant != null && (getOwner() == 0 ^ this.getOwner() == 0)) {
				result.add(ant);
			}
		}
		for (Field field : this.getAttackBorder2()) {
			Ant ant = field.getAnt();
			if (ant != null && (getOwner() == 0 ^ this.getOwner() == 0)) {
				result.add(ant);
			}
		}
		return result;
	}

	public LinkedList<Field> getVisitArea() {
		LinkedList<Field> fields = new LinkedList<Field>();
		for (Offset offset : map.getVisitArea()) {
			fields.add(offset.getField(this.field));
		}

		return fields;
	}

	public LinkedList<Field> getVisitBorder() {
		LinkedList<Field> fields = new LinkedList<Field>();
		for (Offset offset : map.getVisitBorder()) {
			fields.add(offset.getField(this.field));
		}

		return fields;
	}

	@Override
	public String toString() {
		return "ant at " + this.field.getX() + "," + this.field.getY();
	}
}
