package lazarant;
/**
 * Represents type of tile on the game map.
 */
public enum Ilk {
    /** Water tile. */
    WATER,

    MY_HILL,

    /** My ant tile. */
    MY_ANT,

    /** Food tile. */
    FOOD,
    
    /** Land tile. */
    LAND,
    
    /** Dead ant tile. */
    DEAD,

    /** Enemy ant tile. */
    ENEMY_ANT;

    /**
     * Checks if this type of tile is passable, which means it is not a water tile.
     * 
     * @return <code>true</code> if this is not a water tile, <code>false</code> otherwise
     */
    public boolean isPassable() {
        return ordinal() > WATER.ordinal();
    }
    
    /**
     * Checks if this type of tile is unoccupied, which means it is a land tile or a dead ant tile.
     * 
     * @return <code>true</code> if this is a land tile or a dead ant tile, <code>false</code>
     *         otherwise
     */
    public boolean isUnoccupied() {
        return this == LAND || this == DEAD || this == MY_ANT || this == ENEMY_ANT;
    }
}
