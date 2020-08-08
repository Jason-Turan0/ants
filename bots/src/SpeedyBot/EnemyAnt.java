package SpeedyBot;
public class EnemyAnt extends Ant {

	private int owner;
	private int dangerous;

	public EnemyAnt(Field field, int owner,Map map) {
		super(field,map);
		this.owner = owner;
		for (Field attackField : this.getAttackArea()) {
			attackField.setDanger(3);
		}
		for (Field attackField : this.getAttackBorder()) {
			attackField.setDanger(2);
		}
		for (Field attackField : this.getAttackBorder2()) {
			attackField.setDanger(1);
		}
	}

	public void setDangerous(int d) {
		if (d > this.dangerous) {
			this.dangerous = d;
		}
	}

	public int getDangerous() {
		return this.dangerous;
	}

	@Override
	public int getOwner() {
		return this.owner;
	}
}
