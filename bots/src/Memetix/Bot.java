package Memetix;
/**
 * Provides basic game state handling.
 */
public abstract class Bot extends AbstractSystemInputParser {
    private Ants ants;
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void setup(int loadTime, int turnTime, int rows, int cols, int turns, int viewRadius2,
            int attackRadius2, int spawnRadius2, long seed, int players) {
        setAnts(new Ants(loadTime, turnTime, rows, cols, turns, viewRadius2, attackRadius2,
            spawnRadius2, seed, players));
    }
    
    /**
     * Returns game state information.
     * 
     * @return game state information
     */
    public Ants getAnts() {
        return ants;
    }
    
    /**
     * Sets game state information.
     * 
     * @param ants game state information to be set
     */
    protected void setAnts(Ants ants) {
        this.ants = ants;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void beforeUpdate() {
        ants.setTurnStartTime(System.currentTimeMillis());
        ants.clearMap();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void addWater(int row, int col) {
        ants.update(Ilk.WATER, new Tile(row, col));
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void addAnt(int row, int col, int owner) {
        ants.addAnt(owner, new Tile(row, col));
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void addFood(int row, int col) {
        ants.update(Ilk.FOOD, new Tile(row, col));
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void removeAnt(int row, int col, int owner) {
    	ants.removeAnt(owner, new Tile(row, col));
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void addHill(int row, int col, int owner) {
        ants.updateHills(owner, new Tile(row, col));
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void afterUpdate() {
        ants.setVision();
    }
}
