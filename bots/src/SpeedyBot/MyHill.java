package SpeedyBot;
public class MyHill extends Hill {

	public MyHill(Field field) {
		super(field);
	}

	@Override
	public int getOwner() {
		return 0;
	}

	@Override
	public String toString() {
		return "MyHill at " + this.getField();
	}
}
