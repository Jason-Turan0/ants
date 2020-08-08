package Memetix;
/*
 * Stores information discovered while rippling out from a given point in the maze
 */
public class Ripple {
	public int	distance;	//The distance the ripple moved out (defines the area covered)
	public int	enemies;	//The number of enemy ants encountered in this area
	public int	ants;		//The number of my ants encountered in this area
	public int	nearestEnemy; //The distance to the nearest enemy
	public int	nearestAnt;	//The distance to our nearest ant
	public int	danger;		//The distance at which the number of enemy ants is greater than our ants
	public int	safe;		//The distance at which the number of our ants is greater than the enemy ants
	
	public Ripple() {
		this.distance = 0;
		this.enemies = 0;
		this.ants = 0;
		this.nearestEnemy = -1;
		this.nearestAnt = -1;
		this.danger = -1;
		this.safe = -1;
	}	
}
