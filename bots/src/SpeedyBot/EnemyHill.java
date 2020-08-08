package SpeedyBot;
public class EnemyHill extends Hill implements Target {

	private int owner;

	public EnemyHill(Field field, int owner) {
		super(field);
		this.owner = owner;
	}

	@Override
	public int getOwner() {
		return this.owner;
	}
}
