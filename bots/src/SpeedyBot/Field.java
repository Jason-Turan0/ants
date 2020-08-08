package SpeedyBot;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;

public class Field {
	private final Map map;
	private boolean discovered;
	private int lastSeen;
	private int lastVisited;
	private boolean water;
	private int danger;
	private Ant ant;
	private MyAnt nextAnt;
	private Hill hill;
	private Food food;
	private int x;
	private int y;
	private LinkedList<Field> attackBorder2;
	private LinkedList<Field> attackBorder;


	public Field(int x, int y, Map map) {
		this.lastSeen = 2000;
		this.lastVisited = 2000;
		this.ant = null;
		this.nextAnt = null;
		this.danger = 0;
		this.discovered = false;
		this.water = false;
		this.x = x;
		this.y = y;
		this.map = map;
	}

	public void setAnt(Ant ant) {
		this.ant = ant;
	}

	public boolean hasAnt() {
		return this.ant != null;
	}

	public Ant getAnt() {
		return this.ant;
	}

	public void setNextAnt(MyAnt ant) {
		this.nextAnt = ant;
	}

	public boolean hasNextAnt() {
		return this.nextAnt != null;
	}

	public MyAnt getNextAnt() {
		return this.nextAnt;
	}

	public void setHill(Hill hill) {
		this.hill = hill;
	}

	public boolean hasHill() {
		return this.hill != null;
	}

	public Hill getHill() {
		return this.hill;
	}

	public void setFood(Food food) {
		this.food = food;
	}

	public boolean hasFood() {
		return this.food != null;
	}

	public Food getFood() {
		return this.food;
	}

	public int getDanger() {
		return this.danger;
	}

	public void setDanger(int danger) {
		if (danger > this.danger)
			this.danger = danger;
	}

	public boolean isSave() {
		return this.danger <= 1;
	}

	public void setWater() {
		this.water = true;
	}

	public boolean hasWater() {
		return this.water;
	}

	public  void setRows(int rows) {
		rows = rows;
	}

	public  void setCols(int cols) {
		cols = cols;
	}

	public void setVisible() {
		if (this.lastSeen > 0) {
			this.lastSeen = 0;
			this.discovered = true;
		}
	}

	public void setVisited() {
		this.lastVisited = 0;
	}

	public boolean isDiscovered() {
		return this.discovered;
	}

	public boolean isVisible() {
		return this.lastSeen == 0;
	}

	public int lastSeen() {
		return this.lastSeen;
	}

	public int lastVisited() {
		return this.lastVisited;
	}

	public void setAttackBorder2(LinkedList<Field> attackBorder2) {
		this.attackBorder2 = attackBorder2;
	}

	public LinkedList<Field> getAttackBorder2() {
		return this.attackBorder2;
	}

	public void setAttackBorder(LinkedList<Field> attackBorder) {
		this.attackBorder = attackBorder;
	}

	public LinkedList<Field> getAttackBorder() {
		return this.attackBorder;
	}

	public  double getVisibleRatio() {
		return (double) (map.getVisibleFields())
				/ (double) (map.getRows() * map.getCols());
	}

	public ArrayList<Field> getNeighbors() {
		ArrayList<Field> list = new ArrayList<Field>();
		for (int i = 0; i < 4; i++) {
			list.add(this.getNeighbor(i));
		}
		return list;
	}

	public Field getNeighbor(int direction) {
		Offset offset = Offset.getDirectionOffset(direction, this.map);
		int a = (this.x + map.getCols() + offset.getX()) % map.getCols();
		int b = (this.y + map.getRows() + offset.getY()) % map.getRows();
		return map.getField(a,b);
	}

	public ArrayList<Field> getTurnNeighbors() {
		ArrayList<Field> list = this.getNeighbors();
		list.add(this);
		return list;
	}

	public int getX() {
		return this.x;
	}

	public int getY() {
		return this.y;
	}

	public int oneDistance(Field to) {
		Offset offset = getSmallestOffset(to);
		return offset.getOneLength();
	}

	public int twoDistanceSquared(Field to) {
		Offset offset = getSmallestOffset(to);
		return offset.getTwoLengthSquared();
	}

	public double twoDistance(Field to) {
		return Math.sqrt(twoDistanceSquared(to));
	}

	public  Offset getSmallestOffset(int x1, int y1, int x2, int y2) {
		int dx;
		int dx1 = Math.abs(x1 - x2);
		if (dx1 == 0) {
			dx = 0;
		} else {
			int dx2 = map.getCols() - dx1;
			int fx = (x2 - x1) / dx1;
			if (dx1 < dx2) {
				dx = dx1 * fx;
			} else
				dx = dx2 * -fx;
		}

		int dy;
		int dy1 = Math.abs(y1 - y2);
		if (dy1 == 0) {
			dy = 0;
		} else {
			int dy2 = map.getRows() - dy1;
			int fy = (y2 - y1) / dy1;
			if (dy1 < dy2) {
				dy = dy1 * fy;
			} else
				dy = dy2 * -fy;
		}

		return new Offset(dx, dy,this.map);
	}

	public  Offset getSmallestOffset(Field field1, Field field2) {
		return getSmallestOffset(field1.x, field1.y, field2.x, field2.y);
	}

	public Offset getSmallestOffset(Field to) {
		return getSmallestOffset(this, to);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + x;
		result = prime * result + y;
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
		Field other = (Field) obj;
		if (x != other.x)
			return false;
		if (y != other.y)
			return false;
		return true;
	}

	public  Comparator<Field> getVisibleComparator() {
		class VisibleComperator implements Comparator<Field> {

			@Override
			public int compare(Field o1, Field o2) {
				return (o2.lastSeen() - o1.lastSeen());
			}

		}
		return new VisibleComperator();
	}

	public  Comparator<Field> getVisitedComparator() {
		class VisitedComperator implements Comparator<Field> {

			@Override
			public int compare(Field o1, Field o2) {
				return (o2.lastVisited() - o1.lastVisited());
			}

		}
		return new VisitedComperator();
	}

	public static Comparator<Field> getDangerComparator() {
		class VisitedComperator implements Comparator<Field> {

			@Override
			public int compare(Field o1, Field o2) {
				return (o1.danger - o2.danger);
			}

		}
		return new VisitedComperator();
	}

	@Override
	public String toString() {
		return this.x + "," + this.y;
	}

	public void reset() {
		lastSeen++;
		lastVisited++;
		ant = null;
		hill = null;
		food = null;
		nextAnt = null;
		danger = 0;
	}
}
