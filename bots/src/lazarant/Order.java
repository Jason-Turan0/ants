package lazarant;
/**
 * Represents an order to be issued.
 */
public class Order {

    final Tile tile;

    final Direction dir;

    final int priority;


    /**
     * Creates new {@link Order} object.
     * 
     * @param tile map tile with my ant
     * @param direction direction in which to move my ant
     */
    public Order(Tile tile, Direction direction, int priority) {
        this.tile = tile;
        this.dir = direction;
        this.priority = priority;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return tile.row + " " + tile.col + " " + dir.symbol;
    }

    public String toCommand() {
        return "o " + tile.row + " " + tile.col + " " + dir.symbol;
    }

}
